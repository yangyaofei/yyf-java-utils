package io.github.yangyaofei.spring;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 用于 JPA Entity 中 Field 中的一些类的JSON序列化
 *
 * @param <T> the type parameter
 */
@Converter
@Component
public class JsonConverter<T> implements AttributeConverter<T, String> {
    @SuppressWarnings("MissingJavadoc")
    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        var config = this.getConfig();
        this.objectMapper = objectMapper.copy();
        if (config != null && config.excludeNull()) {
            this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
        if (config != null) {
            var namingStrategy = switch (config.namingStrategy()) {
                case DEFAULT -> null;
                case LOWER_CAMEL_CASE -> PropertyNamingStrategies.LOWER_CAMEL_CASE;
                case UPPER_CAMEL_CASE -> PropertyNamingStrategies.UPPER_CAMEL_CASE;
                case SNAKE_CASE -> PropertyNamingStrategies.SNAKE_CASE;
                case UPPER_SNAKE_CASE -> PropertyNamingStrategies.UPPER_SNAKE_CASE;
                case LOWER_CASE -> PropertyNamingStrategies.LOWER_CASE;
                case KEBAB_CASE -> PropertyNamingStrategies.KEBAB_CASE;
                case LOWER_DOT_CASE -> PropertyNamingStrategies.LOWER_DOT_CASE;
            };
            if (namingStrategy != null) {
                this.objectMapper.setPropertyNamingStrategy(namingStrategy);
            }
        }
    }

    private ObjectMapper objectMapper;
    private ObjectWriter objectWriter;

    @SneakyThrows
    @Override
    public String convertToDatabaseColumn(T attribute) {
        if (attribute == null) {
            return null;
        }
        // 使用 T 获取的类型 并使用 objectWriter 进行转换, 可以避免 T 为 Collection 等类型时候
        // 由于 type erasure 导致的序列化的时候一个设定消失
        // 具体例子可以见 chatbit-backend 中的 ChatMessage 中的 Message, 使用 Custom JsonTypeIdResolver 的情况
        if (objectWriter == null) {
            JavaType javaType = objectMapper.constructType(this.getGenericsType());
            objectWriter = objectMapper.writerFor(javaType);
        }
        return objectWriter.writeValueAsString(attribute);
    }

    @SneakyThrows
    @Override
    @SuppressWarnings("unchecked")
    public T convertToEntityAttribute(String data) {
        if (data == null) {
            return null;
        }
        Type type = this.getGenericsType();
        JavaType javaType = objectMapper.constructType(type);
        Object value = objectMapper.readValue(data, javaType);
        return (T) value;
    }

    /**
     * 动态获取运行时子类的泛型类型, 用于序列化工具进行序列化
     *
     * @return the generics type
     */
    @SuppressWarnings("rawtypes")
    protected Type getGenericsType() {
        Type type = this.getClass().getGenericSuperclass();
        // 递归获取带有泛型参数的超类, 若超类为 JsonConverter 则获取对应泛型参数
        while (type != Object.class) {
            if (type instanceof ParameterizedType parameterizedType) {
                if (parameterizedType.getRawType() == JsonConverter.class) {
                    Type[] typeArguments = parameterizedType.getActualTypeArguments();
                    return typeArguments[0];
                }
            }
            assert type instanceof Class;
            type = ((Class) type).getGenericSuperclass();
        }
        return null;
    }

    /**
     * Gets config from annotation
     *
     * @return the config
     */
    protected ConverterConfig getConfig() {
        return this.getClass().getDeclaredAnnotation(ConverterConfig.class);
    }
}
