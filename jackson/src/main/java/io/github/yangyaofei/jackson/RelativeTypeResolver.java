package io.github.yangyaofei.jackson;


import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.util.*;

/**
 * 用于实体类的序列化和反序列化的 TypeResolver , id被存储成相对于 Base Class 的包名的相对路径
 * <ol>
 *     <li>1. 支持多个 Base Class</li>
 *     <li>2. 多个 Base Class 的包路径必须不同, 也不可以一个是另一个的子串</li>
 * </ol>
 */
@SuppressWarnings("unused")
public class RelativeTypeResolver extends TypeIdResolverBase {
    private final static Map<String, JavaType> basePackageMap = new HashMap<>();
    // 用于存储已经进行 init 的 type 的 packageName, 缓存
    private final static Set<String> typeSet = new HashSet<>();

    @Override
    public void init(JavaType baseType) {
        updatePackageMap(baseType);
    }

    /**
     * 更新 baseType 到 baseClass 路径上
     *
     * @param baseType type
     */
    public static void updatePackageMap(JavaType baseType) {
        String packageName = baseType.getRawClass().getPackageName();
        if (typeSet.contains(packageName)) {
            return;
        }
        Class<?> clazz = baseType.getRawClass();
        JavaType superClass = baseType.getSuperClass();
        while (!baseType.getRawClass().isAnnotationPresent(JsonTypeInfo.class)) {
            baseType = baseType.getSuperClass();
            Objects.requireNonNull(baseType, "序列化失败,找不到@JsonTypeInfo信息");
        }
        typeSet.add(packageName);
        basePackageMap.put(baseType.getRawClass().getPackageName(), baseType);
    }

    @Override
    public String idFromValue(Object value) {
        return idFromValueAndType(value, value.getClass());
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        try {
            return idFromType(suggestedType);
        } catch (ClassNotFoundException e) {
            throw new ClassCannotResolveException("Cannot resolve " + suggestedType.getCanonicalName(), e);
        }
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CUSTOM;
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String type) {
        for (var base : basePackageMap.entrySet()) {
            try {
                return context.constructSpecializedType(base.getValue(), Class.forName(base.getKey() + type));
            } catch (ClassNotFoundException ignored) {
            }
        }
        throw new ClassCannotResolveException("typeId: " + type + " can not resolve");
    }

    /**
     * Get a copy of basePackageMap
     *
     * @return basePackageMap base package map
     */
    public static Map<String, JavaType> getBasePackageMap() {
        return Map.copyOf(basePackageMap);
    }

    /**
     * Gets class from typeId
     *
     * @param typeId the typeId
     * @return the class
     * @throws ClassNotFoundException class not found exception
     */
    public static Class<?> getClass(String typeId) throws ClassNotFoundException {
        Class<?> clazz = null;
        for (var base : getBasePackageMap().entrySet()) {
            try {
                return Class.forName(base.getKey() + typeId);
            } catch (ClassNotFoundException ignored) {
            }
        }
        throw new ClassNotFoundException("typeId: " + typeId + " can not resolve");
    }

    /**
     * 用于获取对应的类的Type值, 可用于构建对应的类
     *
     * @param type Type class
     * @return TypeId string
     * @throws ClassNotFoundException class not found exception
     */
    public static String idFromType(Class<?> type) throws ClassNotFoundException {
        if (!typeSet.contains(type.getPackageName())) {
            JavaType javaType = TypeFactory.defaultInstance().constructType(type);
            updatePackageMap(javaType);
        }
        return idFromTypeInner(type);
    }

    /**
     * 用于获取对应的类的Type值, 可用于构建对应的类
     *
     * @param type Type class
     * @return TypeId string
     * @throws ClassNotFoundException class not found exception
     */
    public static String idFromType(JavaType type) throws ClassNotFoundException {
        if (!typeSet.contains(type.getRawClass().getPackageName())) {
            updatePackageMap(type);
        }
        return idFromTypeInner(type.getRawClass());
    }

    private static String idFromTypeInner(Class<?> type) throws ClassNotFoundException {
        String clazzName = type.getName();
        for (var base : basePackageMap.entrySet()) {
            if (clazzName.startsWith(base.getKey())) {
                return clazzName.substring(base.getKey().length());
            }
        }
        throw new ClassNotFoundException("Class: " + type.getCanonicalName() + " not in base packages", null);

    }

    /**
     * exception raised when {@link RelativeTypeResolver} cannot resolve the type to class vers visa
     */
    static public class ClassCannotResolveException extends RuntimeException {
        /**
         * Instantiates a new Class cannot resolve exception.
         *
         * @param message the message
         */
        protected ClassCannotResolveException(String message) {
            super(message);
        }

        /**
         * Instantiates a new Class cannot resolve exception.
         *
         * @param message the message
         * @param cause   the cause
         */
        protected ClassCannotResolveException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}

