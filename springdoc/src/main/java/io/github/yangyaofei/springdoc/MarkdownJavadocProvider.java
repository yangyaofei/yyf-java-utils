package io.github.yangyaofei.springdoc;

import com.github.therapi.runtimejavadoc.*;
import org.springdoc.core.providers.SpringDocJavadocProvider;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

/**
 * 将 Javadoc 中的 markdown 部分转换成 html, 以支持在 springdoc 中使用 markdown
 */
public class MarkdownJavadocProvider extends SpringDocJavadocProvider {
    /**
     * The comment formatter.
     */
    private final CommentFormatter formatter = new CommentFormatter();


    @Override
    public String getClassJavadoc(Class<?> cl) {
        ClassJavadoc classJavadoc = RuntimeJavadoc.getJavadoc(cl);
        return MarkdownUtils.getHtml(formatter.format(classJavadoc.getComment()));
    }


    @Override
    public Map<String, String> getRecordClassParamJavadoc(Class<?> cl) {
        ClassJavadoc classJavadoc = RuntimeJavadoc.getJavadoc(cl);
        return classJavadoc.getRecordComponents().stream()
                .collect(Collectors.toMap(ParamJavadoc::getName, recordClass -> MarkdownUtils.getHtml(formatter.format(recordClass.getComment()))));
    }


    @Override
    public String getMethodJavadocDescription(Method method) {
        MethodJavadoc methodJavadoc = RuntimeJavadoc.getJavadoc(method);
        return MarkdownUtils.getHtml(formatter.format(methodJavadoc.getComment()));
    }


    @Override
    public String getMethodJavadocReturn(Method method) {
        MethodJavadoc methodJavadoc = RuntimeJavadoc.getJavadoc(method);
        return MarkdownUtils.getHtml(formatter.format(methodJavadoc.getReturns()));
    }


    public Map<String, String> getMethodJavadocThrows(Method method) {
        return RuntimeJavadoc.getJavadoc(method)
                .getThrows()
                .stream()
                .collect(toMap(ThrowsJavadoc::getName, javadoc -> formatter.format(javadoc.getComment())));
    }


    @Override
    public String getParamJavadoc(Method method, String name) {
        MethodJavadoc methodJavadoc = RuntimeJavadoc.getJavadoc(method);
        List<ParamJavadoc> paramsDoc = methodJavadoc.getParams();
        return paramsDoc.stream().filter(paramJavadoc1 -> name.equals(paramJavadoc1.getName())).findAny()
                .map(paramJavadoc1 -> MarkdownUtils.getHtml(formatter.format(paramJavadoc1.getComment()))).orElse(null);
    }


    @Override
    public String getFieldJavadoc(Field field) {
        FieldJavadoc fieldJavadoc = RuntimeJavadoc.getJavadoc(field);
        return MarkdownUtils.getHtml(formatter.format(fieldJavadoc.getComment()));
    }

}
