package com.lingjoin.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.ToString;

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
            return new ValidationResult(true);
        } catch (JsonParseException | JsonMappingException e) {
            int lineNumber = e.getLocation().getLineNr();
            int columnNumber = e.getLocation().getColumnNr();
            String message = e.getOriginalMessage();
            return new ValidationResult(false, lineNumber, columnNumber, message);
        } catch (Exception e) {
            return new ValidationResult(false, e.getMessage());
        }
    }

    /**
     * 验证 JSON 合法性结果
     */
    @Getter
    @ToString
    public static class ValidationResult {
        final private boolean isValid;
        final private int lineNumber;
        final private int columnNumber;
        final private String message;

        protected ValidationResult(boolean isValid) {
            this(isValid, 0, 0, null);
        }

        protected ValidationResult(boolean isValid, String message) {
            this(isValid, 0, 0, message);
        }

        protected ValidationResult(boolean isValid, int lineNumber, int columnNumber, String message) {
            this.isValid = isValid;
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
            this.message = message;
        }
    }
}