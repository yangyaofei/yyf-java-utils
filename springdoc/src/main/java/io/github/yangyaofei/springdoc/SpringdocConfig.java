package io.github.yangyaofei.springdoc;

import org.springdoc.core.providers.SpringDocJavadocProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * springdoc 使用 markdown 的配置项, 在需要使用的项目中使用 {@link ComponentScan} 方式引用即可
 */
@AutoConfiguration
public class SpringdocConfig {
    /**
     * Spring doc javadoc provider spring doc javadoc provider.
     *
     * @return the spring doc javadoc provider
     */
    @Bean
    SpringDocJavadocProvider springDocJavadocProvider() {
        return new MarkdownJavadocProvider();
    }


}
