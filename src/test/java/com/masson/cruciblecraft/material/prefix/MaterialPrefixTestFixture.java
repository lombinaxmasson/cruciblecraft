package com.masson.cruciblecraft.material.prefix;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Test-only lifecycle fixture. Production access never bootstraps implicitly;
 * the test engine explicitly installs built-ins before each test class.
 */
public final class MaterialPrefixTestFixture implements BeforeAllCallback {
    @Override
    public void beforeAll(ExtensionContext context) {
        bootstrapBuiltins();
    }

    public static synchronized void bootstrapBuiltins() {
        if (!MaterialPrefixCatalog.isBootstrapped()) {
            MaterialPrefixCatalog.bootstrap(null);
        }
    }

    static synchronized void reset() {
        MaterialPrefixCatalog.resetForTests();
    }
}
