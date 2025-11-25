package com.yourcompany.uac.testing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal reflective test runner to avoid pulling external JUnit artifacts in
 * offline CI. It discovers lifecycle annotations defined in the local
 * org.junit.jupiter.api stubs and executes known test classes.
 */
public final class SimpleTestRunner {

    private SimpleTestRunner() {}

    public static void main(String[] args) throws Exception {
        Class<?>[] tests = new Class<?>[]{
                com.yourcompany.uac.checks.CheckManagerTest.class,
                com.yourcompany.uac.mitigation.MitigationManagerTest.class,
                com.yourcompany.uac.config.ConfigManagerTest.class,
                com.yourcompany.uac.storage.DatabaseManagerTest.class,
                com.yourcompany.uac.PluginSmokeTest.class
        };

        int failures = 0;
        for (Class<?> testClass : tests) {
            Object instance = testClass.getDeclaredConstructor().newInstance();
            List<Method> befores = new ArrayList<>();
            List<Method> afters = new ArrayList<>();
            List<Method> testMethods = new ArrayList<>();
            for (Method method : testClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(BeforeEach.class)) {
                    method.setAccessible(true);
                    befores.add(method);
                }
                if (method.isAnnotationPresent(AfterEach.class)) {
                    method.setAccessible(true);
                    afters.add(method);
                }
                if (method.isAnnotationPresent(Test.class)) {
                    method.setAccessible(true);
                    testMethods.add(method);
                }
            }

            for (Method test : testMethods) {
                try {
                    for (Method before : befores) {
                        before.invoke(instance);
                    }
                    test.invoke(instance);
                } catch (InvocationTargetException ex) {
                    Throwable cause = ex.getTargetException();
                    failures++;
                    System.err.println("[FAIL] " + testClass.getSimpleName() + "." + test.getName() + ": " + cause);
                } catch (Exception ex) {
                    failures++;
                    System.err.println("[FAIL] " + testClass.getSimpleName() + "." + test.getName() + ": " + ex);
                } finally {
                    for (Method after : afters) {
                        after.invoke(instance);
                    }
                }
            }
        }

        if (failures > 0) {
            throw new AssertionError("SimpleTestRunner detected " + failures + " failure(s)");
        }
        System.out.println("SimpleTestRunner completed successfully.");
    }
}

