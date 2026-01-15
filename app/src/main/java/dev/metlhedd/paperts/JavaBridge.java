package dev.metlhedd.paperts;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

public class JavaBridge {

    /**
     * Get enum value by class name and value name.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object enumValue(String className, String valueName) {
        try {
            Class<? extends Enum> enumClass = (Class<? extends Enum>) Class.forName(className);
            return Enum.valueOf(enumClass, valueName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found: " + className, e);
        }
    }

    /**
     * Get all enum values.
     */
    public Object[] enumValues(String className) {
        try {
            Class<?> enumClass = Class.forName(className);
            return enumClass.getEnumConstants();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found: " + className, e);
        }
    }

    /**
     * Create new instance with constructor arguments.
     */
    public Object newInstance(String className, Object... args) {
        try {
            Class<?> clazz = Class.forName(className);

            if (args == null || args.length == 0) {
                return clazz.getDeclaredConstructor().newInstance();
            }

            for (Constructor<?> constructor : clazz.getConstructors()) {
                if (constructor.getParameterCount() == args.length) {
                    if (canApply(constructor.getParameterTypes(), args)) {
                        return constructor.newInstance(args);
                    }
                }
            }

            throw new NoSuchMethodException(
                    "No matching constructor for " + className +
                            " with args: " + Arrays.toString(args)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance: " + className, e);
        }
    }

    /**
     * Call static method.
     */
    public Object callStatic(String className, String methodName, Object... args) {
        try {
            Class<?> clazz = Class.forName(className);

            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(methodName) &&
                        method.getParameterCount() == (args == null ? 0 : args.length)) {
                    if (args == null || canApply(method.getParameterTypes(), args)) {
                        return method.invoke(null, args);
                    }
                }
            }

            throw new NoSuchMethodException(
                    "No matching method: " + className + "." + methodName
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to call static method", e);
        }
    }

    /**
     * Get static field value.
     */
    public Object getStatic(String className, String fieldName) {
        try {
            Class<?> clazz = Class.forName(className);
            return clazz.getField(fieldName).get(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get static field", e);
        }
    }

    /**
     * Check if a class exists.
     */
    public boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Create a singleton list (immutable, single element).
     */
    public <T> java.util.List<T> singletonList(T element) {
        return java.util.Collections.singletonList(element);
    }

    /**
     * Create a Java List from varargs.
     */
    @SafeVarargs
    public final <T> java.util.List<T> asList(T... elements) {
        return new java.util.ArrayList<>(Arrays.asList(elements));
    }

    /**
     * Create a Java List from an array.
     */
    public <T> java.util.List<T> toList(Object[] elements) {
        java.util.List<T> list = new java.util.ArrayList<>();
        for (Object item : elements) {
            @SuppressWarnings("unchecked")
            T typed = (T) item;
            list.add(typed);
        }
        return list;
    }

    /**
     * Create an empty ArrayList.
     */
    public <T> java.util.List<T> emptyList() {
        return new java.util.ArrayList<>();
    }

    /**
     * Create an immutable empty list.
     */
    public <T> java.util.List<T> emptyImmutableList() {
        return java.util.Collections.emptyList();
    }

    private boolean canApply(Class<?>[] paramTypes, Object[] args) {
        for (int i = 0; i < paramTypes.length; i++) {
            if (args[i] == null) continue;

            Class<?> paramType = paramTypes[i];
            Class<?> argType = args[i].getClass();

            // Handle primitives
            if (paramType.isPrimitive()) {
                paramType = boxPrimitive(paramType);
            }

            if (!paramType.isAssignableFrom(argType)) {
                // Special handling for numbers
                if (Number.class.isAssignableFrom(paramType) &&
                        Number.class.isAssignableFrom(argType)) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }

    private Class<?> boxPrimitive(Class<?> primitive) {
        if (primitive == int.class) return Integer.class;
        if (primitive == long.class) return Long.class;
        if (primitive == double.class) return Double.class;
        if (primitive == float.class) return Float.class;
        if (primitive == boolean.class) return Boolean.class;
        if (primitive == byte.class) return Byte.class;
        if (primitive == short.class) return Short.class;
        if (primitive == char.class) return Character.class;
        return primitive;
    }
}