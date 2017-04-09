package org.jdbi.v3.sqlobject;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.sun.management.HotSpotDiagnosticMXBean;

import org.bitstrings.test.junit.runner.ClassLoaderPerTestRunner;
import org.bitstrings.test.junit.runner.TestClassLoader;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

public class LeakDetectorClassLoaderPerTestRunner extends Runner
{
    private ClassLoaderPerTestRunner delegate;
    private WeakReference<TestClassLoader> weakClassLoader;
    private String name;

    public LeakDetectorClassLoaderPerTestRunner(Class<?> klass) throws InitializationError
    {
        delegate = new ClassLoaderPerTestRunner(klass) {
            @Override
            protected TestClassLoader createClassLoader(String testPath) {
                TestClassLoader result = super.createClassLoader(testPath);
                weakClassLoader = new WeakReference<>(result);
                name = getName() + "." + getTestMethodName();
                return result;
            }
        };
    }

    static String dumpHeap(String name) {
        String where = "/tmp/leaked-classloader.hprof";
        try {
            Files.deleteIfExists(Paths.get(where));
            ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class).dumpHeap(where, true);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        throw new AssertionError("Test case " + name + " has leaks!  Heap dump at " + where);
    }

    @Override
    public Description getDescription() {
        return delegate.getDescription();
    }

    @Override
    public void run(RunNotifier notifier) {
        delegate.run(notifier);
        delegate = null;

        long since = System.currentTimeMillis();
        while (weakClassLoader.get() != null && System.currentTimeMillis() - since < 2000) {
            System.gc();
        }

        TestClassLoader stillLoaded = weakClassLoader.get();
        if (stillLoaded != null) {
            System.err.println("Class loader " + stillLoaded + " didn't unload!");
            System.gc();
            System.err.println("-> " + dumpHeap(name));
        }
    }
}
