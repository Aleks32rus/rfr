package io.student.rangiffler.junit.extension;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Отключает мишени вне запуска движка с локальным разрешением на их выполнение.
 */
public final class ProbeTargetCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (context.getConfigurationParameter("rfr.probe.targets.enabled").map(Boolean::parseBoolean).orElse(false)) {
            return ConditionEvaluationResult.enabled("Выполнение мишеней разрешено текущим запуском движка");
        }
        return ConditionEvaluationResult.disabled("Мишень доступна только внутри разрешённого запуска EngineTestKit");
    }
}
