package no.toll.jsondoc;

import no.toll.jsondoc.tools.JsonBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlTests {

    private Context ctx(final String mode) {
        return new Context(mode)
                .add("test", "true")
//                .add("sampleColumns", "eksempel")
//                .add(Context.SKIP_TABLES,"metadata,data")
//                .add("variant", "tags")
//                .add(Context.EXCLUDED_COLUMNS, "x-ICS2_XML")
                ;
    }

    private String runHtml(final String data) {
        final var context = ctx("HTML");
        final var root = new JsonDocParser(context).parseString(data);
        return new HtmlPrinter(root, context).create();
    }

    private String runXhtml(final String data) {
        final var context = ctx("XHTML");
        final var root = new JsonDocParser(context).parseString(data);
        return new WikiPrinter(root, context).create();
    }

    @Test
    void html_lineBreaks() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v("x-bar", "ab\\nc")
                    .endObject()
                .endProperties()
                .toString();
        final var res = runHtml(data);
        assertTrue(res.matches("(?s).*ab<br/>c.*"));
    }

    @Test
    void html_simpleLink() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v("x-bar", "linkTo(http://github.com)")
                    .endObject()
                .endProperties()
                .toString();
        final var res = runHtml(data);
        assertTrue(res.matches("(?s).*<a href=.http://github.com.>http://github.com</a>.*"), res);
    }

    @Test
    void html_linkWithText() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                       .v("x-bar", "linkTo(http://github.com,  target)")
                    .endObject()
                .endProperties()
                .toString();
        final var res = runHtml(data);
        assertTrue(res.matches("(?s).*<a href=.http://github.com.>target</a>.*"), res);
    }

    @Test
    void html_langAttr() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v("x-bar", "linkTo(http://github.com,  target)")
                    .endObject()
                .endProperties()
                .toString();
        final var res = runHtml(data);
        assertTrue(res.matches("(?s).*lang=." + Context.LANG_EN + ".*"), res);
        final var context2 = ctx("HTML").add(Context.LANG, "no");
        final var res2 =  new HtmlPrinter(new JsonDocParser(context2).parseString(data), context2).create();
        assertTrue(res2.matches("(?s).*lang=.no.*"), res);
    }

    @Test
    void html_linkToTable() {
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
        final var res = runHtml(data);
        assertTrue(res.matches("(?s).*<a href=.#foo__bar.>bar.*<h3 id=.foo__bar.*"), res);
    }

    @Test
    void xhtml_linkToTable() {
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
        final var res = runXhtml(data);
        assertTrue(res.matches("(?s).*ac:link ac:anchor=.foo__bar.*CDATA.bar.*ac:name=.anchor.*foo__bar.*bar.*"),
                res);
    }
}