package io.github.yangyaofei.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * 用于反序列化 TaskParameterDto 时使用的 TypeResolver
 * <p>
 * 使用{@link #typeResolveMap} 存储 {@code @type} 字段存储的名称和类的对应 map, 并在本工程和下游工程中使用 {@code @PostConstruct} 注解
 * 使其在工程启动时添加到上述 map 中
 */
public class MapTypeResolver extends TypeIdResolverBase {
    private static final Map<String, Class<?>> typeResolveMap = new LinkedHashMap<>();
    private JavaType javaType;

    /**
     * 添加 type 和 类 的对应关系的方法, 若已经存在对应的 type 字符串则会覆盖
     *
     * @param map type, class 对应关系
     */
    public static void addClass(Map<String, Class<?>> map) {
        typeResolveMap.putAll(map);
    }

    /**
     * 获取当前的解析对应关系
     *
     * @return map
     */
    public static Map<String, Class<?>> getTypeResolveMap() {
        return Map.copyOf(typeResolveMap);
    }

    @Override
    public String idFromValue(Object value) {
        return idFromValueAndType(value, value.getClass());
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        var invertMap = typeResolveMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        if (!invertMap.containsKey(suggestedType))
            throw new RuntimeException(suggestedType + " not in resolveMap: \n" + typeResolveMap);
        return invertMap.get(suggestedType);
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CUSTOM;
    }


    @Override
    public void init(JavaType baseType) {
        Class<?> clazz = baseType.getRawClass();
        while (!clazz.isAnnotationPresent(JsonTypeInfo.class)) {
            clazz = clazz.getSuperclass();
            Objects.requireNonNull(clazz, "序列化失败,找不到@JsonTypeInfo信息");
        }
        javaType = baseType;
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) {
        if (!typeResolveMap.containsKey(id))
            throw new RuntimeException(id + " not in resolveMap: \n" + typeResolveMap);
        return context.constructSpecializedType(javaType, typeResolveMap.get(id));
    }
}
