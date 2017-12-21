package com.mesosphere.sdk.scheduler;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;

import com.mesosphere.sdk.api.SchedulerModule;

class ModuleLoader {

    private static final Class<SchedulerModule> MODULE_CLASS = SchedulerModule.class;

    private ModuleLoader() {
        // do not instantiate.
    }

    static SchedulerModule loadModule(String className, Optional<File> jarFile) {
        ClassLoader classLoader;
        if (jarFile.isPresent()) {
            // Use provided JAR URL
            try {
                classLoader = new URLClassLoader(new URL[] {jarFile.get().toURI().toURL()}, ModuleLoader.class.getClassLoader());
            } catch (MalformedURLException e) {
                throw new IllegalStateException("Unable to construct URL for file: " + jarFile.get().getAbsolutePath(), e);
            }
        } else {
            // Search default classpath
            classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = ModuleLoader.class.getClassLoader();
            }
        }

        final Object instance;
        try {
            instance = Class.forName(className, true, classLoader)
                    .asSubclass(MODULE_CLASS)
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Missing empty constructor to instantiate instance of " + className, e);
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new IllegalStateException("Could not instantiate instance of " + className, e);
        }
        if (!MODULE_CLASS.isInstance(instance)) {
            throw new IllegalStateException(className + " is not an instance of type " + MODULE_CLASS.getName());
        }
        return MODULE_CLASS.cast(instance);
    }
}
