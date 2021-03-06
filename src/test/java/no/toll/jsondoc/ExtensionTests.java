package no.toll.jsondoc;

import no.toll.jsondoc.tools.JsonBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtensionTests {

    // TODO test skipTables

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
                        .v("x-bar_ba_DOS", "Y")
                    .endObject()
                .endProperties()
                .toString();
        final var res = runHtml(data);
        assertTrue(res.contains("<td>baz</td>"), "row");
        assertTrue(res.contains("<th>Bar ba DOS</th>"), "row");
        assertFalse(res.contains("x-bar"), "xif");
    }

    @Test
    void excludedColumns_excludes() {
        final var data = new JsonBuilder()
                .properties()
                .v("title", "X")
                .v("A", 1.0)
                .v("B", true)
                .endProperties()
                .toString();
        final var context = ctx("HTML").add(Context.EXCLUDE_COLUMNS, "Q,B,W");
        final var res = new HtmlPrinter(new JsonDocParser(context).parseString(data), context).toString();
        assertTrue(res.contains("<td>A</td>"), "A");
        assertFalse(res.contains("<td>B</td>"), "B");
    }

    @Test
    void strict_introducesExtraProperty() {
        final var data = new JsonBuilder()
                .properties()
                    .v("title", "X")
                    .object("foo")
                        .v("type", "number")
                    .endObject()
                .endProperties()
                .toString();
        final var context = ctx("SCHEMA").add(Context.STRICT, "true");
        final var res = new SchemaPrinter(new JsonDocParser(context).parseString(data)).toString();
        assertTrue(res.matches("(?s).*" + JsonDocNames.ADDITIONAL_PROPERTIES + ".: false.*"), res);
    }

    @Test
    void strict_introducesExtraItems() {
        final var data = new JsonBuilder()
                .properties()
                    .v("title", "X")
                    .object("foo")
                        .v("type", "array")
                        .object("items")
                            .v("type", "string")
                        .endObject()
                    .endObject()
                .endProperties()
                .toString();
        final var context = ctx("SCHEMA").add(Context.STRICT, "true");
        final var res = new SchemaPrinter(new JsonDocParser(context).parseString(data)).toString();
        System.err.println(data);
        System.err.println(res);
        assertTrue(res.matches("(?s).*" + JsonDocNames.ADDITIONAL_ITEMS + ".: false.*"), res);
    }
}