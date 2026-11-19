package io.student.rangiffler.junit.extension;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Отключает мишени, пока параметр конфигурации явно не разрешит их выполнение.
 */
public final class ProbeTargetCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (context.getConfigurationParameter("rfr.probe.targets.enabled").map(Boolean::parseBoolean).orElse(false)) {
            return ConditionEvaluationResult.enabled("Выполнение мишеней разрешено параметром конфигурации");
        }
        return ConditionEvaluationResult.disabled("Мишень отключена без разрешающего параметра конфигурации");
    }
}
