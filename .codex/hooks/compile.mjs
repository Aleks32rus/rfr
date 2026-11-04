import { createHash } from 'node:crypto';
import { mkdirSync, readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, relative, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const modules = ['rfr-api', 'rfr-auth', 'rfr-e2e'];

function digest(value) {
  return createHash('sha256').update(value).digest('hex');
}

function files(directory, accept, snapshot) {
  for (const entry of readdirSync(directory, { withFileTypes: true })) {
    const path = join(directory, entry.name);
    if (entry.isDirectory() && !['node_modules', 'dist', 'build', '.git', '.idea'].includes(entry.name)) {
      files(path, accept, snapshot);
    } else if (entry.isFile() && accept(relative(root, path).replaceAll('\\', '/'))) {
      snapshot[relative(root, path).replaceAll('\\', '/')] = digest(readFileSync(path));
    }
  }
}

function sources() {
  const snapshot = {};
  for (const module of modules) {
    files(join(root, module, 'src'), path => path.endsWith('.java'), snapshot);
  }
  files(join(root, 'rfr-gql-client'), path => /\.(?:[cm]?[jt]sx?|css|scss|sass|less|html|json|svg|png|jpe?g|webp|gif|ico|woff2?)$/i.test(path), snapshot);
  return snapshot;
}

function compilation(command, args, cwd) {
  const windows = process.platform === 'win32';
  const result = spawnSync(windows ? 'cmd.exe' : command, windows ? ['/d', '/s', '/c', [command, ...args].join(' ')] : args, {
    cwd,
    encoding: 'utf8',
    timeout: 240_000,
    maxBuffer: 16 * 1024 * 1024,
    windowsHide: true,
    env: { ...process.env, NO_COLOR: '1', FORCE_COLOR: '0' },
  });
  return [
    `Команда: ${command} ${args.join(' ')}`,
    `Каталог: ${cwd}`,
    `Код завершения: ${result.status ?? 'отсутствует'}`,
    result.error ? `Ошибка запуска: ${result.error.message}` : '',
    result.signal ? `Сигнал: ${result.signal}` : '',
    result.stdout,
    result.stderr,
  ].filter(Boolean).join('\n');
}

function feedback(event, context) {
  process.stdout.write(JSON.stringify({
    hookSpecificOutput: {
      hookEventName: event,
      additionalContext: context,
    },
  }));
}

function main() {
  const event = JSON.parse(readFileSync(0, 'utf8').replace(/^\uFEFF/, ''));
  try {
    if (!event.session_id || !event.tool_use_id) {
      throw new Error('В событии отсутствует session_id или tool_use_id');
    }
    const directory = join(tmpdir(), 'rfr-codex-compile', digest(root));
    const path = join(directory, `${digest(`${event.session_id}:${event.tool_use_id}`)}.json`);
    const snapshot = sources();
    if (event.hook_event_name === 'PreToolUse') {
      mkdirSync(directory, { recursive: true });
      writeFileSync(path, JSON.stringify(snapshot), 'utf8');
      return;
    }
    const previous = JSON.parse(readFileSync(path, 'utf8'));
    const changes = [...new Set([...Object.keys(previous), ...Object.keys(snapshot)])]
      .filter(file => previous[file] !== snapshot[file]);
    const tasks = modules.filter(module => changes.some(file => file.startsWith(`${module}/`)))
      .flatMap(module => [`:${module}:compileJava`, `:${module}:compileTestJava`]);
    const output = [];
    if (tasks.length) {
      output.push(compilation(process.platform === 'win32' ? 'gradlew.bat' : './gradlew', [...tasks, '--console=plain', '--no-parallel'], root));
    }
    if (changes.some(file => file.startsWith('rfr-gql-client/'))) {
      output.push(compilation(process.platform === 'win32' ? 'npm.cmd' : 'npm', ['run', 'build'], join(root, 'rfr-gql-client')));
    }
    if (output.length) {
      feedback('PostToolUse', `Результат автоматической компиляции после изменения исходников:\n${output.join('\n\n')}\nПри ошибке исправь причину перед завершением задачи`);
    }
  } catch (error) {
    feedback(event.hook_event_name, `Автоматическая компиляция не выполнена: ${error.message}`);
  }
}

main();
