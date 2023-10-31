package com.lingjoin.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 验证 JSON 是否合法, 并返回 JSON 错误的位置
 */
@SuppressWarnings("unused")
public class JsonValidation {
    /**
     * @param jsonStr 需要验证的 JSON
     * @param clazz   需要反序列化后的对应类型
     * @param <T>     同上
     * @return 验证结果
     */
    public static <T> ValidationResult validate(String jsonStr, Class<T> clazz) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            objectMapper.readValue(jsonStr, clazz);
            return new ValidationResult(true, 0, 0, null);
        } catch (JsonParseException | JsonMappingException e) {
            int lineNumber = e.getLocation().getLineNr();
            int columnNumber = e.getLocation().getColumnNr();
            String message = e.getOriginalMessage();
            return new ValidationResult(false, lineNumber, columnNumber, message);
        } catch (Exception e) {
            return new ValidationResult(false, 0, 0, e.getMessage());
        }
    }

    /**
     * 验证 JSON 合法性结果
     */
    public record ValidationResult(boolean isValid, int lineNumber, int columnNumber, String message) {
    }
}