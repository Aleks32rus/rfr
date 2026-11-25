package io.student.rangiffler.junit.extension;

import org.junit.jupiter.api.extension.ExtensionContext;

public class DemoSuiteExtension implements SuiteExtension {

    @Override
    public void beforeSuite(ExtensionContext context) {
        SuiteExtension.super.beforeSuite(context);
    }

    @Override
    public void afterSuite(ExtensionContext context) {
        SuiteExtension.super.afterSuite(context);
    }
}
