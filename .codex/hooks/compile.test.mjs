import assert from 'node:assert/strict';
import { copyFileSync, mkdirSync, mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

function fixture() {
  const root = mkdtempSync(join(tmpdir(), 'rfr-compile-check-'));
  mkdirSync(join(root, '.codex/hooks'), { recursive: true });
  copyFileSync(join(dirname(fileURLToPath(import.meta.url)), 'compile.mjs'), join(root, '.codex/hooks/compile.mjs'));
  for (const module of ['rfr-api', 'rfr-auth', 'rfr-e2e']) {
    mkdirSync(join(root, module, 'src'), { recursive: true });
    writeFileSync(join(root, module, 'src/Sample.java'), 'class Sample {}');
  }
  mkdirSync(join(root, 'rfr-gql-client/src'), { recursive: true });
  writeFileSync(join(root, 'rfr-gql-client/src/main.ts'), 'const value = 37;');
  writeFileSync(join(root, 'gradlew.bat'), '@echo off\r\necho JAVA %*\r\nexit /b 0\r\n');
  writeFileSync(join(root, 'gradlew'), '#!/bin/sh\necho JAVA "$@"\n', { mode: 0o755 });
  writeFileSync(join(root, 'npm.cmd'), '@echo off\r\necho FRONT %*\r\necho diagnostic 1>&2\r\nexit /b 7\r\n');
  writeFileSync(join(root, 'npm'), '#!/bin/sh\necho FRONT "$@"\necho diagnostic >&2\nexit 7\n', { mode: 0o755 });
  return root;
}

function invoke(root, event, id) {
  const result = spawnSync(process.execPath, [join(root, '.codex/hooks/compile.mjs')], {
    input: JSON.stringify({ hook_event_name: event, session_id: root, tool_use_id: id }),
    encoding: 'utf8',
    timeout: 15_000,
    windowsHide: true,
    env: { ...process.env, PATH: `${root}${process.platform === 'win32' ? ';' : ':'}${process.env.PATH}` },
  });
  if (result.error || result.status !== 0) {
    throw result.error ?? new Error(result.stderr);
  }
  return result.stdout ? JSON.parse(result.stdout).hookSpecificOutput.additionalContext : '';
}

test('Unchanged sources dont trigger compilation', () => {
  const root = fixture();
  invoke(root, 'PreToolUse', 'unchanged');
  assert.equal(invoke(root, 'PostToolUse', 'unchanged'), '', 'Unchanged sources must not trigger compilation');
});

test('Java changes cannot compile unrelated modules', () => {
  const root = fixture();
  invoke(root, 'PreToolUse', 'java');
  writeFileSync(join(root, 'rfr-auth/src/Sample.java'), 'class Changed {}');
  const output = invoke(root, 'PostToolUse', 'java');
  assert.deepEqual([output.includes(':rfr-auth:compileJava :rfr-auth:compileTestJava'), output.includes(':rfr-api:'), output.includes('Код завершения: 0')], [true, false, true], 'Java changes must not compile unrelated modules or lose the exit code');
});

test('Frontend failures cannot lose stdout stderr or exit status', () => {
  const root = fixture();
  invoke(root, 'PreToolUse', 'frontend');
  writeFileSync(join(root, 'rfr-gql-client/src/main.ts'), 'const value = 59;');
  const output = invoke(root, 'PostToolUse', 'frontend');
  assert.deepEqual([output.includes('FRONT run build'), output.includes('diagnostic'), output.includes('Код завершения: 7')], [true, true, true], 'Frontend failures must not lose stdout stderr or exit status');
});

test('Repeated changes to an uncommitted file cannot be missed', () => {
  const root = fixture();
  invoke(root, 'PreToolUse', 'first');
  writeFileSync(join(root, 'rfr-e2e/src/Sample.java'), 'class First {}');
  invoke(root, 'PostToolUse', 'first');
  invoke(root, 'PreToolUse', 'second');
  writeFileSync(join(root, 'rfr-e2e/src/Sample.java'), 'class Second {}');
  assert.match(invoke(root, 'PostToolUse', 'second'), /:rfr-e2e:compileJava :rfr-e2e:compileTestJava/, 'Repeated changes must not be missed');
});

test('Build artifacts dont trigger another compilation', () => {
  const root = fixture();
  invoke(root, 'PreToolUse', 'artifacts');
  mkdirSync(join(root, 'rfr-gql-client/dist'), { recursive: true });
  writeFileSync(join(root, 'rfr-gql-client/dist/main.js'), 'generated');
  assert.equal(invoke(root, 'PostToolUse', 'artifacts'), '', 'Build artifacts must not trigger another compilation');
});
