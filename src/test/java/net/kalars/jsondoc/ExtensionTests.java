package net.kalars.jsondoc;

import net.kalars.jsondoc.tools.JsonBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtensionTests {

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
        return new HtmlPrinter(root, context).toString();
    }

    private String runSchema(final String data) {
        final var context = ctx("SCHEMA");
        final var root = new JsonDocParser(context).parseString(data);
        return new HtmlPrinter(root, context).toString(); // TODO Change printer
    }

    @Test
    void xif_isIgnoredIfNoVariants() {
        final var data = new JsonBuilder()
                .properties()
                    .v("title", "X")
                    .object("foo")
                        .v("baz", "Z")
                        .v("description", "Y")
                        .v("xif-variant", "var1")
                    .endObject()
                    .object("bar")
                        .v("key", "Q")
                    .endObject()
                .endProperties()
                .toString();
        final var context = ctx("HTML");
        final var res = new HtmlPrinter(new JsonDocParser(context).parseString(data), context).toString();
        assertTrue(res.contains("<td>X</td>"), "title");
        assertTrue(res.contains("<td>Y<"), "description");
        assertTrue(res.contains("<td>bar</td>"), "bar");
        assertTrue(res.contains("<td>key</td>"), "key");
        assertTrue(res.contains("<td>baz</td>"), "baz");
        assertFalse(res.contains("xif"), "xif");
    }

    @Test
    void xif_includesOnCorrectVariable() {
        final var data = new JsonBuilder()
                .properties()
                    .v("title", "X")
                    .object("foo")
                        .v("baz", "Z")
                        .v("description", "Y")
                        .v("xif-variant", "var1")
                    .endObject()
                    .object("bar")
                        .v("key", "Q")
                    .endObject()
                .endProperties()
                .toString();
        final var context = ctx("HTML")
                .add("variant", "var1"); // This is different from previous test
        final var res = new HtmlPrinter(new JsonDocParser(context).parseString(data), context).toString();
        assertTrue(res.contains("<td>X</td>"), "title");
        assertTrue(res.contains("<td>Y"), "description");
        assertTrue(res.contains("<td>bar</td>"), "bar");
        assertTrue(res.contains("<td>key</td>"), "key");
        assertTrue(res.contains("<td>baz</td>"), "baz");
        assertFalse(res.contains("xif"), "xif");
    }

    @Test
    void xif_excludesOnOtherVariable() {
        final var data = new JsonBuilder()
                .properties()
                    .v("title", "X")
                    .object("foo")
                        .v("baz", "Z")
                        .v("description", "Y")
                        .v("xif-variant", "var1")
                    .endObject()
                    .object("bar")
                        .v("key", "Q")
                    .endObject()
                .endProperties()
                .toString();
        final var context = ctx("HTML")
                .add("variant", "var2"); // This is different from previous test
        final var res = new HtmlPrinter(new JsonDocParser(context).parseString(data), context).toString();
        assertTrue(res.contains("<td>X</td>"), "title");
        assertFalse(res.contains("<td>Y"), "description");
        assertTrue(res.contains("<td>bar</td>"), "bar");
        assertTrue(res.contains("<td>key</td>"), "key");
        assertFalse(res.contains("<td>baz</td>"), "baz");
        assertFalse(res.contains("xif"), "xif");
    }

    @Test
    void xifnot_isIgnoredIfNoVariants() {
        final var data = new JsonBuilder()
                .properties()
                    .v("title", "X")
                    .object("foo")
                        .v("baz", "Z")
                        .v("description", "Y")
                        .v("xifnot-variant", "var1")
                    .endObject()
                        .object("bar")
                    .v("key", "Q")
                    .endObject()
                .endProperties()
                .toString();
        final var context = ctx("HTML");
        final var res = new HtmlPrinter(new JsonDocParser(context).parseString(data), context).toString();
        assertTrue(res.contains("<td>X</td>"), "title");
        assertTrue(res.contains("<td>Y<"), "description");
        assertTrue(res.contains("<td>bar</td>"), "bar");
        assertTrue(res.contains("<td>key</td>"), "key");
        assertTrue(res.contains("<td>baz</td>"), "baz");
        assertFalse(res.contains("xif"), "xif");
    }

    @Test
    void xifnot_excludesOnCorrectVariable() {
        final var data = new JsonBuilder()
                .properties()
                    .v("title", "X")
                    .object("foo")
                        .v("baz", "Z")
                        .v("description", "Y")
                        .v("xifnot-variant", "var1")
                    .endObject()
                    .object("bar")
                        .v("key", "Q")
                    .endObject()
                .endProperties()
                .toString();
        final var context = ctx("HTML")
                .add("variant", "var1"); // This is different from previous test
        final var res = new HtmlPrinter(new JsonDocParser(context).parseString(data), context).toString();
        assertTrue(res.contains("<td>X</td>"), "title");
        assertFalse(res.contains("<td>Y"), "description");
        assertTrue(res.contains("<td>bar</td>"), "bar");
        assertTrue(res.contains("<td>key</td>"), "key");
        assertFalse(res.contains("<td>baz</td>"), "baz");
        assertFalse(res.contains("xif"), "xif");
    }

    @Test
    void xifnot_includesOnOtherVariable() {
        final var data = new JsonBuilder()
                .properties()
                    .v("title", "X")
                    .object("foo")
                        .v("baz", "Z")
                        .v("description", "Y")
                        .v("xifnot-variant", "var1")
                    .endObject()
                    .object("bar")
                        .v("key", "Q")
                    .endObject()
                .endProperties()
                .toString();
        final var context = ctx("HTML")
                .add("variant", "var2"); // This is different from previous test
        final var res = new HtmlPrinter(new JsonDocParser(context).parseString(data), context).toString();
        assertTrue(res.contains("<td>X</td>"), "title");
        assertTrue(res.contains("<td>Y"), "description");
        assertTrue(res.contains("<td>bar</td>"), "bar");
        assertTrue(res.contains("<td>key</td>"), "key");
        assertTrue(res.contains("<td>baz</td>"), "baz");
        assertFalse(res.contains("xif"), "xif");
    }

    @Test
    void xdoc_convertsToColumn() {
        final var data = new JsonBuilder()
                .v("title", "X")
                .properties()
                    .object("foo")
                        .v("baz", "Z")
                        .v("x-bar", "Y")
                    .endObject()
                .endProperties()
                .toString();
        final var res = runHtml(data);
        assertTrue(res.contains("<td>baz</td>"), "row");
        assertTrue(res.contains("<th>Bar</th>"), "row");
        assertFalse(res.contains("x-bar"), "xif");
    }
}