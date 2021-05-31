package net.kalars.jsondoc;

import net.kalars.jsondoc.tools.JsonBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownTests {

    private Context ctx() {
        return new Context("MARKDOWN")
                .add("test", "true")
//                .add("sampleColumns", "eksempel")
//                .add(Context.SKIP_TABLES,"metadata,data")
//                .add("variant", "tags")
//                .add(Context.EXCLUDED_COLUMNS, "x-ICS2_XML")
                ;
    }

    private String runMarkdown(final String data) {
        final var context = ctx();
        final var root = new JsonDocParser(context).parseString(data);
        return new MarkdownPrinter(root).toString();
    }


    @Test
    void markdown_lineBreaks() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v("x-bar", "ab\\nc")
                    .endObject()
                .endProperties()
                .toString();
        final var res = runMarkdown(data);
        assertTrue(res.matches("(?s).*ab<br />c.*"));
    }

    @Test
    void markdown_simpleLink() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v("x-bar", "linkTo(http://github.com)")
                    .endObject()
                .endProperties()
                .toString();
        final var res = runMarkdown(data);
        assertTrue(res.matches("(?s).*\\[http://github.com].*"), res);
    }

    @Test
    void markdown_linkWithText() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v("x-bar", "linkTo(http://github.com,  target)")
                    .endObject()
                .endProperties()
                .toString();
        final var res = runMarkdown(data);
        assertTrue(res.matches("(?s).*\\[target]\\(http://github.com\\).*"), res);
    }

    @Test
    void markdown_linkToTable() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v("description", "1")
                        .object("bar")
                            .v("description", "2")
                            .v("ting", "tang")
                        .endObject()
                        .object("baz")
                            .v("description", "3")
                            .v("ting", "tang")
                        .endObject()
                    .endObject()
                .endProperties()
                .toString();
        final var res = runMarkdown(data);
        assertTrue(res.matches("(?s).*\\[bar>]\\(#foo__bar\\).*"), res);
    }
}