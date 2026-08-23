package common.extensions;

import org.junit.jupiter.api.extension.*;

public class TestTimingExtension implements
        BeforeEachCallback, AfterEachCallback,
        BeforeTestExecutionCallback, AfterTestExecutionCallback {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(TestTimingExtension.class);

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        String threadName = Thread.currentThread().getName();
        String testName = context.getRequiredTestClass().getPackageName() + "." + context.getDisplayName();

        ExtensionContext.Store store = context.getStore(NAMESPACE);
        store.put("beforeEachStart", System.currentTimeMillis());
        store.put("threadName", threadName);
        store.put("testName", testName);

        System.out.println(threadName + " [setup] starting for '" + testName + "'");
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        ExtensionContext.Store store = context.getStore(NAMESPACE);

        long beforeEachStart = store.get("beforeEachStart", Long.class);
        String threadName = store.get("threadName", String.class);
        String testName = store.get("testName", String.class);

        long setupEnd = System.currentTimeMillis();
        store.put("testExecutionStart", setupEnd);

        System.out.println(threadName + " [setup] finished for '" + testName + "' after " + (setupEnd - beforeEachStart) + " ms");
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        ExtensionContext.Store store = context.getStore(NAMESPACE);

        long testExecutionStart = store.get("testExecutionStart", Long.class);
        String threadName = store.get("threadName", String.class);
        String testName = store.get("testName", String.class);

        long testExecutionEnd = System.currentTimeMillis();
        store.put("testExecutionEnd", testExecutionEnd);

        System.out.println(threadName + " [test] '" + testName + "' finished after " + (testExecutionEnd - testExecutionStart) + " ms");
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        ExtensionContext.Store store = context.getStore(NAMESPACE);

        long testExecutionEnd = store.get("testExecutionEnd", Long.class);
        String threadName = store.get("threadName", String.class);
        String testName = store.get("testName", String.class);

        long afterEachEnd = System.currentTimeMillis();

        System.out.println(threadName + " [teardown] '" + testName + "' took " + (afterEachEnd - testExecutionEnd) + " ms");
    }
}