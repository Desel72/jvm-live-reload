package me.seroperson.reload.live.reflect;

import me.seroperson.reload.live.build.BuildLogger;

import java.util.Arrays;
import java.util.function.Predicate;

public final class MiscUtils {

    /**
     * Checks if a class with the given name is available on the classpath.
     *
     * @param className the fully qualified class name to check
     * @return true if the class is available, false otherwise
     */
    public static boolean hasClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static void dumpThreads(BuildLogger logger, ThreadGroup threadGroup) {
        var threads = new Thread[threadGroup.activeCount()];
        threadGroup.enumerate(threads);
        logger.debug("Dumping " + threads.length + " threads:");
        Arrays.stream(threads).forEach((t) -> logger.debug("- " + t));
    }

    public static void updateContextClassLoader(
            ThreadGroup threadGroup, Predicate<Thread> predicate, ClassLoader cl) {
        var threads = new Thread[threadGroup.activeCount()];
        threadGroup.enumerate(threads);
        Arrays.stream(threads).filter(predicate).forEach((t) -> t.setContextClassLoader(cl));
    }

}
