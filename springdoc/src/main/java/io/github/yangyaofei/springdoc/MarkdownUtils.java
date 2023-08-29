package io.github.yangyaofei.springdoc;

import lombok.experimental.UtilityClass;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.ins.InsExtension;
import org.commonmark.ext.task.list.items.TaskListItemsExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.List;

/**
 * Markdown utils
 */
@UtilityClass
class MarkdownUtils {

    List<Extension> extensions = List.of(
            StrikethroughExtension.create(),
            InsExtension.create(),
            TaskListItemsExtension.create()
    );

    Parser parser = Parser.builder()
            .extensions(extensions)
            .build();

    /**
     * Markdown to HTML
     *
     * @param markdown the markdown
     * @return the html
     */
    String getHtml(String markdown) {
        Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder()
                .extensions(extensions)
                .build();
        return renderer.render(document);
    }
}