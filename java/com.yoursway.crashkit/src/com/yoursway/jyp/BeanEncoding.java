package com.yoursway.jyp;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

import java.beans.IntrospectionException;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the given bean (which may also be a collection) using a restricted
 * set of types: <code>null</code>, <code>String</code>, <code>Number</code>s,
 * <code>Map</code>, <code>List</code>. The simplified representation is meant
 * to be further encoded into JSON, YAML, OS X Property List or whatever.
 * 
 * @author Andrey Tarantsov <andreyvit@gmail.com>
 */
public class BeanEncoding {
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target( { METHOD })
    public @interface Transient {
        
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target( { PARAMETER, METHOD })
    public @interface Property {
        
        String value();
        
    }
    
    public static class BeanificationException extends Exception {
        private static final long serialVersionUID = 1L;
        
        public BeanificationException(String s, Class<?> klass, Throwable cause) {
            super(s + " (bean " + klass.getName() + ")", cause);
        }
        
        public BeanificationException(String s, Class<?> klass) {
            super(s + " (bean " + klass.getName() + ")");
        }
        
    }
    
    public static Object simplify(Object bean) {
        if (bean == null || bean instanceof CharSequence || bean instanceof Number || bean instanceof Boolean)
            return bean;
        if (bean instanceof Character)
            return bean.toString();
        if (bean instanceof Date)
            return ((Date) bean).getTime();
        if (bean instanceof Map<?, ?>)
            return simplifyMap((Map<?, ?>) bean);
        if (bean instanceof Iterable<?>)
            return simplifyIterable((Iterable<?>) bean);
        if (bean.getClass().isArray())
            return simplifyIterable(Arrays.asList((Object[]) bean));
        try {
            return simplifyBean(bean);
        } catch (IntrospectionException e) {
            throw new IllegalArgumentException("Cannot introspect bean " + bean.getClass().getName(), e);
        }
    }
    
    private static Object beanify(Object value, Type type) throws BeanificationException {
        if (type instanceof Class<?>)
            return beanify(value, (Class<?>) type);
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            if (rawType instanceof Class<?>) {
                Class<?> klass = (Class<?>) rawType;
                if (klass == Collection.class || klass == List.class)
                    return beanifyList(value, parameterizedType.getActualTypeArguments()[0]);
                if (klass == Map.class)
                    return beanifyMap(value, parameterizedType.getActualTypeArguments()[0], parameterizedType
                            .getActualTypeArguments()[1]);
                else
                    return beanify(value, klass);
            }
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }
    
    private static List<Object> beanifyList(Object value, Type elementType) throws BeanificationException {
        if (value == null)
            return null;
        
        if (!(value instanceof Collection<?>))
            throw new BeanificationException("Expected a collection", List.class);
        Collection<?> source = (Collection<?>) value;
        List<Object> result = new ArrayList<Object>(source.size());
        for (Object item : source)
            result.add(beanify(item, elementType));
        return result;
    }
    
    private static Map<Object, Object> beanifyMap(Object value, Type keyType, Type valueType)
            throws BeanificationException {
        if (value == null)
            return null;
        
        if (!(value instanceof Map<?, ?>))
            throw new BeanificationException("Expected a map", List.class);
        Map<?, ?> source = (Map<?, ?>) value;
        Map<Object, Object> result = new HashMap<Object, Object>(source.size());
        for (Map.Entry<?, ?> entry : source.entrySet())
            result.put(beanify(entry.getKey(), keyType), beanify(entry.getValue(), valueType));
        return result;
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T beanify(Object value, Class<T> klass) throws BeanificationException {
        if (value == null)
            return null;
        
        if (klass.isAssignableFrom(value.getClass()))
            return klass.cast(value);
        
        // handle primitive types
        if (klass == int.class)
            return (T) beanify(value, Integer.class);
        if (klass == long.class)
            return (T) beanify(value, Long.class);
        if (klass == float.class)
            return (T) beanify(value, Float.class);
        if (klass == double.class)
            return (T) beanify(value, Double.class);
        if (klass == byte.class)
            return (T) beanify(value, Byte.class);
        if (klass == short.class)
            return (T) beanify(value, Short.class);
        if (klass == char.class)
            return (T) beanify(value, Character.class);
        
        // handle arrays
        if (klass.isArray()) {
            List<Object> list = beanifyList(value, klass.getComponentType());
            Object[] result = (Object[]) Array.newInstance(klass.getComponentType(), list.size());
            return (T) list.toArray(result);
        }
        
        // special handling for some types
        if (Number.class.isAssignableFrom(klass) && value instanceof Number) {
            Number number = (Number) value;
            if (klass == Integer.class)
                return (T) (Integer) number.intValue();
            else if (klass == Long.class)
                return (T) (Long) number.longValue();
            else if (klass == Float.class)
                return (T) (Float) number.floatValue();
            else if (klass == Double.class)
                return (T) (Double) number.doubleValue();
            else if (klass == Byte.class)
                return (T) (Byte) number.byteValue();
            else if (klass == Short.class)
                return (T) (Short) number.shortValue();
            else
                throw new BeanificationException(
                        "Got incompatible number type " + value.getClass().getName(), klass);
        }
        if (klass == Date.class && value instanceof Number)
            return (T) new Date(((Number) value).longValue());
        if (klass == String.class)
            return (T) value.toString();
        if (klass == Character.class) {
            String s = value.toString();
            if (s.length() != 1)
                throw new BeanificationException("A single-character string expected, got " + s.length()
                        + " chars", klass);
            return (T) (Character) s.charAt(0);
        }
        
        if (value instanceof Map<?, ?>)
            return beanifyBean(klass, new HashMap<Object, Object>((Map<?, ?>) value));
        
        throw new BeanificationException("Incompatible value " + value.getClass().getName(), klass);
    }
    
    private static <T> T beanifyBean(Class<T> klass, Map<Object, Object> map) throws BeanificationException {
        Constructor<T> constructor = chooseConstructor(klass);
        String[] constructorPropertyNames = parameterPropertyNames(constructor);
        Object[] arguments = new Object[constructorPropertyNames.length];
        Type[] parameterTypes = constructor.getGenericParameterTypes();
        for (int i = 0; i < arguments.length; i++) {
            String property = constructorPropertyNames[i];
            if (!map.containsKey(property))
                throw new BeanificationException("Missing value for constructor property " + property, klass);
            arguments[i] = beanify(map.remove(property), parameterTypes[i]);
        }
        T bean;
        
        try {
            bean = constructor.newInstance(arguments);
        } catch (Throwable e) {
            throw new BeanificationException("Error creating bean instance", klass, e);
        }
        
        Map<String, Member> setterByProperty = findPropertySetters(klass);
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = beanify(entry.getKey(), String.class);
            Member member = setterByProperty.get(key);
            if (member == null)
                throw new BeanificationException("No setter found for property " + key, klass);
            if (member instanceof Method)
                setUsingMethod(klass, bean, key, entry.getValue(), (Method) member);
        }
        return bean;
    }
    
    private static <T> Map<String, Member> findPropertySetters(Class<T> klass) {
        Map<String, Member> setterByProperty = new HashMap<String, Member>();
        for (Method method : klass.getMethods()) {
            String name = method.getName();
            if (isSetter(name)) {
                String beanPropertyName = propertyNameOf(name);
                Property annotation = method.getAnnotation(Property.class);
                if (annotation == null) {
                    Method getter = findGetter(klass, beanPropertyName);
                    if (getter != null)
                        annotation = getter.getAnnotation(Property.class);
                }
                String propertyName = (annotation == null ? beanPropertyName : annotation.value());
                setterByProperty.put(propertyName, method);
            }
        }
        return setterByProperty;
    }
    
    private static <T> Method findGetter(Class<T> klass, String beanPropertyName) throws AssertionError {
        String prefix = Character.toUpperCase(beanPropertyName.charAt(0)) + beanPropertyName.substring(1);
        Method getter = null;
        try {
            getter = klass.getMethod("get" + prefix);
        } catch (SecurityException e) {
            throw new AssertionError(e);
        } catch (NoSuchMethodException e) {
            try {
                getter = klass.getMethod("is" + prefix);
            } catch (SecurityException e1) {
                throw new AssertionError(e);
            } catch (NoSuchMethodException e1) {
            }
        }
        return getter;
    }
    
    private static <T> Map<String, Member> findPropertyGetters(Class<T> klass) {
        Map<String, Member> getterByProperty = new HashMap<String, Member>();
        for (Method method : klass.getMethods()) {
            String name = method.getName();
            if ("getClass".equals(name))
                continue;
            if (isGetter(name)) {
                Property annotation = method.getAnnotation(Property.class);
                String propertyName = (annotation == null ? propertyNameOf(name) : annotation.value());
                getterByProperty.put(propertyName, method);
            }
        }
        return getterByProperty;
    }
    
    private static <T> void setUsingMethod(Class<T> klass, T bean, String key, Object rawValue, Method method)
            throws BeanificationException {
        Type[] setterParameterTypes = method.getGenericParameterTypes();
        if (setterParameterTypes.length != 1)
            throw new BeanificationException("Unsuitable setter signature for property " + key, klass);
        Object value = beanify(rawValue, setterParameterTypes[0]);
        try {
            method.invoke(bean, value);
        } catch (Throwable e) {
            throw new BeanificationException("Error invoking setter for property " + key, klass, e);
        }
    }
    
    private static String propertyNameOf(String name) {
        if (isSetter(name))
            return Character.toLowerCase(name.charAt(3)) + name.substring(4);
        else if (isRegularGetter(name))
            return Character.toLowerCase(name.charAt(3)) + name.substring(4);
        else if (isBooleanGetter(name))
            return Character.toLowerCase(name.charAt(2)) + name.substring(3);
        else
            throw new IllegalArgumentException("Method name is not a getter/setter: " + name);
    }
    
    private static boolean isSetter(String methodName) {
        return (methodName.length() >= 4 && methodName.startsWith("set") && methodName.charAt(3) == Character
                .toUpperCase(methodName.charAt(3)));
    }
    
    private static boolean isGetter(String methodName) {
        return isRegularGetter(methodName) || isBooleanGetter(methodName);
    }
    
    private static boolean isRegularGetter(String methodName) {
        return (methodName.length() >= 4 && methodName.startsWith("get") && methodName.charAt(3) == Character
                .toUpperCase(methodName.charAt(3)));
    }
    
    private static boolean isBooleanGetter(String methodName) {
        return (methodName.length() >= 3 && methodName.startsWith("is") && methodName.charAt(2) == Character
                .toUpperCase(methodName.charAt(2)));
    }
    
    private static <T> Constructor<T> chooseConstructor(Class<T> klass) {
        Constructor<T> propertyConstructor = null, defaultConstructor = null;
        for (Constructor<T> constructor : klass.getConstructors())
            if (isPropertyConstructor(constructor))
                propertyConstructor = constructor;
            else if (constructor.getParameterTypes().length == 0)
                defaultConstructor = constructor;
        if (propertyConstructor == null && defaultConstructor == null)
            throw new IllegalArgumentException("No suitable deserialization constructor found for "
                    + klass.getName());
        if (propertyConstructor == null)
            propertyConstructor = defaultConstructor;
        return propertyConstructor;
    }
    
    private static <T> boolean isPropertyConstructor(Constructor<T> constructor) {
        boolean anyAnnotated = false, allAnnotated = true;
        for (Annotation[] annotations : constructor.getParameterAnnotations()) {
            boolean annotated = false;
            for (Annotation annotation : annotations)
                if (annotation instanceof Property) {
                    annotated = true;
                    break;
                }
            anyAnnotated |= annotated;
            allAnnotated &= annotated;
        }
        if (anyAnnotated && !allAnnotated)
            throw new IllegalArgumentException("If any constructor parameters is annotated with "
                    + Property.class.getName() + ", then all of them must be annotated.");
        return anyAnnotated;
    }
    
    private static <T> String[] parameterPropertyNames(Constructor<T> constructor) {
        Annotation[][] parameterAnnotations = constructor.getParameterAnnotations();
        String[] names = new String[parameterAnnotations.length];
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation annotation : parameterAnnotations[i])
                if (annotation instanceof Property) {
                    names[i] = ((Property) annotation).value();
                    break;
                }
        }
        return names;
    }
    
    private static List<Object> simplifyIterable(Iterable<?> bean) {
        List<Object> result = new ArrayList<Object>();
        for (Object value : bean)
            result.add(simplify(value));
        return result;
    }
    
    private static Map<Object, Object> simplifyMap(Map<?, ?> bean) {
        Map<Object, Object> result = new HashMap<Object, Object>();
        for (Map.Entry<?, ?> entry : bean.entrySet())
            result.put(simplify(entry.getKey()), entry.getValue());
        return result;
    }
    
    private static Map<String, Object> simplifyBean(Object bean) throws IntrospectionException {
        Map<String, Member> getters = findPropertyGetters(bean.getClass());
        Map<String, Object> map = new HashMap<String, Object>();
        for (Map.Entry<String, Member> entry : getters.entrySet()) {
            String key = entry.getKey();
            Member member = entry.getValue();
            Object value;
            if (member instanceof Method) {
                if (((Method) member).isAnnotationPresent(Transient.class))
                    continue;
                value = getUsingMethod(bean, key, member);
            } else
                throw new AssertionError("Unreachable");
            map.put(key, simplify(value));
            
        }
        return map;
    }
    
    private static Object getUsingMethod(Object bean, String key, Member member) {
        Method method = (Method) member;
        try {
            return method.invoke(bean);
        } catch (Throwable e) {
            throw new IllegalArgumentException("Error reading property " + key + " from bean "
                    + bean.getClass().getName(), e);
        }
    }
    
}
