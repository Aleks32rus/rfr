package io.student.rangiffler.test.demo;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Собирает фактические события одного запуска демо и печатает дерево после всех тестов.
 */
public final class DemoLifecycle implements BeforeAllCallback, AfterAllCallback, ParameterResolver, DemoTrace {
    private final List<Event> events = new ArrayList<>();
    private final Map<DemoExtension, String> identities = new LinkedHashMap<>();

    @Override
    public void beforeAll(ExtensionContext context) {
        if (context.getParent().orElseThrow().getTestClass().isPresent()) {
            return;
        }
        context.getStore(ExtensionContext.Namespace.create(DemoTrace.class)).put(DemoTrace.class, this);
    }

    @Override
    public synchronized void record(DemoExtension extension, long creation, String event, ExtensionContext context) {
        if (!identities.containsKey(extension)) {
            identities.put(extension, "E" + (identities.size() + 1));
            events.add(new Event(creation, context, identities.get(extension), "constructor"));
        }
        events.add(new Event(System.nanoTime(), context, identities.get(extension), event));
    }

    @Override
    public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
        return parameter.getParameter().getType().equals(DemoBody.class);
    }

    @Override
    public DemoBody resolveParameter(ParameterContext parameter, ExtensionContext context) {
        return () -> test(context);
    }

    private synchronized void test(ExtensionContext context) {
        events.add(new Event(System.nanoTime(), context, "Тест", "Выполнение тела теста"));
    }

    @Override
    public synchronized void afterAll(ExtensionContext context) {
        if (context.getParent().orElseThrow().getTestClass().isPresent()) {
            return;
        }
        Branch root = new Branch(context.getDisplayName());
        Map<String, Branch> branches = new HashMap<>();
        branches.put(context.getUniqueId(), root);
        for (Event event : events.stream().sorted(Comparator.comparingLong(Event::time)).toList()) {
            branch(event.context(), branches, root).append(event.identity(), event.action());
        }
        StringBuilder output = new StringBuilder("\n" + root.label + "\n");
        root.render(output, "");
        System.out.print(output);
        context.publishReportEntry("Жизненный цикл расширений", output.toString());
    }

    private Branch branch(ExtensionContext context, Map<String, Branch> branches, Branch root) {
        if (!branches.containsKey(context.getUniqueId())) {
            Branch parent = context.getParent().map(origin -> branch(origin, branches, root)).orElse(root);
            Branch child = new Branch(context.getDisplayName());
            parent.children.add(child);
            branches.put(context.getUniqueId(), child);
        }
        return branches.get(context.getUniqueId());
    }

    /**
     * Сохраняет время события независимо от момента его добавления в общий журнал.
     */
    private record Event(long time, ExtensionContext context, String identity, String action) {
    }

    /**
     * Объединяет события контекста и дочерние тесты в порядке первого обращения.
     */
    private static final class Branch {
        private final String label;
        private final List<Branch> children = new ArrayList<>();
        private final Set<String> creations = new HashSet<>();
        private final Set<String> usages = new HashSet<>();

        private Branch(String label) {
            this.label = label;
        }

        private void append(String identity, String action) {
            if (identity.equals("Тест")) {
                children.add(new Branch("Тест → " + action));
            } else if (action.equals("constructor")) {
                creations.add(identity);
                children.add(new Branch("Создан DemoExtension " + identity + " → constructor"));
            } else {
                if (usages.add(identity)) {
                    children.add(new Branch("DemoExtension " + identity + (creations.contains(identity)
                            ? " — новый экземпляр в этой ветке" : " — тот же экземпляр, созданный выше")));
                }
                children.add(new Branch(identity + " → " + action));
            }
        }

        private void render(StringBuilder output, String prefix) {
            for (int index = 0; index < children.size(); index++) {
                Branch child = children.get(index);
                boolean last = index == children.size() - 1;
                output.append(prefix).append(last ? "└── " : "├── ").append(child.label).append('\n');
                child.render(output, prefix + (last ? "    " : "│   "));
            }
        }
    }
}
