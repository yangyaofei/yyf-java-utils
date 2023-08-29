package io.github.yangyaofei.spring;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 控制 JsonConverter 的配置, 不进行继承
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConverterConfig {
    /**
     * 是否忽略 null
     * @return true 表示忽略不转换, false 表示使用objectMapper默认配置, 默认为 false
     */
    boolean excludeNull() default false;

    /**
     * namingStrategy
     *
     * @return NamingStrategy, default 为 {@link NamingStrategy#DEFAULT}
     */
    NamingStrategy namingStrategy() default NamingStrategy.DEFAULT;

    /**
     * ObjectMapper namingStrategy
     */
    enum NamingStrategy {

        /**
         * 默认策略，使用系统 objectMapper 设置
         */
        DEFAULT,
        /**
         * use {@link PropertyNamingStrategies#LOWER_CAMEL_CASE}
         */
       LOWER_CAMEL_CASE,
        /**
         * use {@link PropertyNamingStrategies#UPPER_CAMEL_CASE}
         */
       UPPER_CAMEL_CASE,
        /**
         * use {@link PropertyNamingStrategies#SNAKE_CASE}
         */
       SNAKE_CASE,
        /**
         * use {@link PropertyNamingStrategies#UPPER_SNAKE_CASE}
         */
       UPPER_SNAKE_CASE,
        /**
         * use {@link PropertyNamingStrategies#LOWER_CASE}
         */
        LOWER_CASE,
        /**
         * use {@link PropertyNamingStrategies#KEBAB_CASE}
         */
        KEBAB_CASE,
        /**
         * use {@link PropertyNamingStrategies#LOWER_DOT_CASE}
         */
        LOWER_DOT_CASE



    }
}
