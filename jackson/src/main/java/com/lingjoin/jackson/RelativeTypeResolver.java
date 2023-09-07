package com.lingjoin.jackson;


import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

    @Override
    public void init(JavaType baseType) {
        Class<?> clazz = baseType.getRawClass();
        JavaType superClass = baseType.getSuperClass();
        while (!baseType.getRawClass().isAnnotationPresent(JsonTypeInfo.class)) {
            baseType = baseType.getSuperClass();
            Objects.requireNonNull(baseType, "序列化失败,找不到@JsonTypeInfo信息");
        }

        basePackageMap.put(baseType.getRawClass().getPackageName(), baseType);
    }

    @Override
    public String idFromValue(Object value) {
        return idFromValueAndType(value, value.getClass());
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        String clazzName = suggestedType.getName();
        for (var base : basePackageMap.entrySet()) {
            if (clazzName.startsWith(base.getKey())) {
                return clazzName.substring(base.getKey().length());
            }
        }
        // TODO generalize this exception
        throw new RuntimeException("Object not in basePackage");
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
        // TODO more info in this exception
        throw new RuntimeException("Object not in basePackage");
    }
}

