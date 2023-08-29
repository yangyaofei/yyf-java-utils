package com.lingjoin.jackson;


import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;

import java.util.Objects;

/**
 * 用于 实体类的序列化和反序列化的 TypeResolver, 后期可能考虑将其工具化.
 */
@SuppressWarnings("unused")
public class RelativeTypeResolver extends TypeIdResolverBase {
    private JavaType javaType;
    private String basePackagePrefix;

    @Override
    public void init(JavaType baseType) {
        Class<?> clazz = baseType.getRawClass();
        while (!clazz.isAnnotationPresent(JsonTypeInfo.class)) {
            clazz = clazz.getSuperclass();
            Objects.requireNonNull(clazz, "序列化失败,找不到@JsonTypeInfo信息");
        }
        this.basePackagePrefix = clazz.getPackageName();
        javaType = baseType;
    }

    @Override
    public String idFromValue(Object value) {
        return idFromValueAndType(value, value.getClass());
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        String clazzName = suggestedType.getName();
        if (clazzName.startsWith(basePackagePrefix)) {
            return clazzName.substring(basePackagePrefix.length());
        } else {
            // TODO generalize this exception
            throw new RuntimeException("Object not in basePackage");
        }
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CUSTOM;
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String type) {
        try {
            return context.constructSpecializedType(javaType, Class.forName(basePackagePrefix + type));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}

