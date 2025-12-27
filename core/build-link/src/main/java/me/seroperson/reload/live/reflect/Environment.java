package me.seroperson.reload.live.reflect;

import me.seroperson.reload.live.UnrecoverableException;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Environment {

  public static void putEnv(Map<String, String> newEnv) {
    updateEnv(false, newEnv);
  }

  public static void setEnv(Map<String, String> newEnv) {
    updateEnv(true, newEnv);
  }

  // Copied and edited from:
  // - https://github.com/Philippus/sbt-dotenv/blob/main/src/main/scala/au/com/onegeek/sbtdotenv/DirtyEnvironmentHack.scala
  // - https://stackoverflow.com/questions/318239/how-do-i-set-environment-variables-from-java/7201825#7201825
  @SuppressWarnings("unchecked")
  private static void updateEnv(boolean clear, Map<String, String> newEnv) {
    try {
      if (hasProcessVariableClass()) {
        var processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
        var theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
        theEnvironmentField.setAccessible(true);

        var variableClass = Class.forName("java.lang.ProcessEnvironment$Variable");
        var convertToVariable = variableClass.getMethod("valueOf", String.class);
        convertToVariable.setAccessible(true);

        var valueClass = Class.forName("java.lang.ProcessEnvironment$Value");
        var convertToValue = valueClass.getMethod("valueOf", String.class);
        convertToValue.setAccessible(true);

        var env = (Map<Object, Object>) theEnvironmentField.get(null);

        if (clear) {
          env.clear();
        }
        for (var entry : newEnv.entrySet()) {
          var variable = convertToVariable.invoke(null, entry.getKey());
          var value = convertToValue.invoke(null, entry.getValue());
          env.put(variable, value);
        }
      } else {
        // First attempt: access ProcessEnvironment fields
        var processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");

        var theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
        theEnvironmentField.setAccessible(true);
        var env = (Map<String, String>) theEnvironmentField.get(null);
        if (clear) {
          env.clear();
        }
        env.putAll(newEnv);

        var theCaseInsensitiveEnvironmentField =
                processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
        theCaseInsensitiveEnvironmentField.setAccessible(true);
        var cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
        if (clear) {
          cienv.clear();
        }
        cienv.putAll(newEnv);
      }
    } catch (NoSuchFieldException e) {
      // Fallback: modify the UnmodifiableMap used by System.getenv()
      try {
        Map<String, String> env = System.getenv();
        Class<?>[] classes = Collections.class.getDeclaredClasses();
        for (Class<?> cl : classes) {
          if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
            Field field = cl.getDeclaredField("m");
            field.setAccessible(true);
            Map<String, String> map = (Map<String, String>) field.get(env);
            if (clear) {
              map.clear();
            }
            map.putAll(newEnv);
          }
        }
      } catch (Throwable t) {
        throw new UnrecoverableException("Unable to update environment", t);
      }
    } catch (Throwable t) {
      throw new UnrecoverableException("Unable to update environment", t);
    }
  }

  private static boolean hasProcessVariableClass() {
    return MiscUtils.hasClass("java.lang.ProcessEnvironment$Variable");
  }

}
