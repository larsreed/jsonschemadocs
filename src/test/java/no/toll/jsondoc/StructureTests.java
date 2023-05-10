package no.toll.jsondoc;

import no.toll.jsondoc.tools.JsonBuilder;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("OptionalGetWithoutIsPresent")
class StructureTests {

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
        return new HtmlPrinter(new JsonDocParser(context).parseString(data), context).create();
    }

    private String runSchema(final String data) {
        final var context = ctx("SCHEMA");
        final var root = new JsonDocParser(context).parseString(data);
        return new SchemaPrinter(root).create();
    }

    @Test
    void attributesOnTopNode_areShown() {
        final var data = new JsonBuilder().v("description", "Top!").v("title", "X").toString();
        assertTrue(runHtml(data).contains("Top!"));
    }

    @Test
    void required_areHiddenForHtmlAndShownForSchema() {
        final var data = new JsonBuilder().v("title", "X").required("A").toString();
        assertFalse(runHtml(data).contains(no.toll.jsondoc.JsonDocNames.REQUIRED));
        assertTrue(runSchema(data).contains(no.toll.jsondoc.JsonDocNames.REQUIRED));
    }

    @Test
    void required_marksAttribute() {
        final var data = new JsonBuilder()
                .properties()
                  .v("title", "X")
                  .v("B", true)
                .endProperties()
                .required("A", "title")
                .toString();
        final var root = new JsonDocParser(ctx("HTML")).parseString(data);
        assertFalse(root.isRequired(), "root");
        for (final var c : root.children) {
            if (c.name.equals("title")) assertTrue(c.isRequired(), c.name);
            else assertFalse(c.isRequired(), c.name);
        }
    }

    @Test
    void properties_areRemovedForHtmlAndShownForSchema() {
        final var data = new JsonBuilder().v("title", "X")
                .properties()
                .v("A", "b")
                .endProperties()
                .toString();
        assertFalse(runHtml(data).contains(no.toll.jsondoc.JsonDocNames.PROPERTIES));
        assertTrue(runSchema(data).contains(no.toll.jsondoc.JsonDocNames.PROPERTIES));
    }

    @Test
    void properties_areMovedToParent() {
        final var data = new JsonBuilder().v("top", "")
                .properties()
                  .v("prop", "")
                .endProperties()
                .toString();
        assertTrue(runHtml(data).matches("(?s).*<td>top(<[/]?td>)+</tr>(\\s*\\R*\\s*)*<tr><td>prop</td>.*"));
    }

    @Test
    void type_canContainArray() {
        final var data = new JsonBuilder()
                .properties()
                    .v("A", "b")
                    .object("foo")
                        .array("type", "string", "null")
                        .endArray()
                    .endObject()
                .endProperties()
                .toString();
        assertTrue(runHtml(data).contains("string, null"));
    }

    @Test
    void alwaysColumns_areThere() {
        final var data = new JsonBuilder().v("A", "b").toString();
        final var res = runHtml(data).toUpperCase();
        for (final var c: no.toll.jsondoc.JsonDocNames.ALWAYS_COLUMNS) assertTrue(res.contains("<TH>" + c.toUpperCase() + "</TH>"), c);
    }

    @Test
    void cardinality_none() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v("title", "x")
                    .endObject()
                .endProperties()
                .toString();
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("Type"))
                .map(n -> n.values)
                .map(NodeValues::toString);
        assertFalse(vals.isPresent());
    }

    @Test
    void cardinality_required() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v("title", "x")
                    .endObject()
                .endProperties()
                .required("foo")
                .toString();
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertTrue(vals.contains(no.toll.jsondoc.JsonDocNames.REQUIRED));
    }

    @Test
    void cardinality_minOnly() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v("title", "x")
                        .v(no.toll.jsondoc.JsonDocNames.MIN_ITEMS, 11)
                    .endObject()
                .endProperties()
                .toString();
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("[11, ...]", vals);
    }

    @Test
    void cardinality_maxOnly() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v("title", "x")
                        .v(no.toll.jsondoc.JsonDocNames.MAX_ITEMS, 12)
                    .endObject()
                .endProperties()
                .toString();
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("[0, 12]", vals);
    }

    @Test
    void cardinality_arrayMinOnly() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v("type", "array")
                        .v(no.toll.jsondoc.JsonDocNames.MIN_LENGTH, 0)
                        .object("items")
                            .v("type", "object")
                            .properties()
                                .object("id")
                                    .v("type", "string")
                                .endObject()
                            .endProperties()
                        .endObject()
                    .endObject()
                .endProperties()
                .toString();
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertTrue(vals.contains("[0...]"));
    }

    @Test
    void cardinality_minMax() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v("title", "x")
                        .v(no.toll.jsondoc.JsonDocNames.MIN_ITEMS, 30)
                        .v(no.toll.jsondoc.JsonDocNames.MAX_ITEMS, 20) // illogical ...
                    .endObject()
                .endProperties()
                .toString();
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("[30, 20]", vals);
    }

    @Test
    void cardinality_minMaxEqual() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v("title", "x")
                        .v(no.toll.jsondoc.JsonDocNames.MIN_ITEMS, 3)
                        .v(no.toll.jsondoc.JsonDocNames.MAX_ITEMS, 3)
                    .endObject()
                .endProperties()
                .toString();
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("[3]", vals);
    }

    @Test
    void length_minOnly() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v("title", "x")
                        .v(no.toll.jsondoc.JsonDocNames.MIN_LENGTH, 11)
                    .endObject()
                .endProperties()
                .toString();
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("[11...]", vals);
    }

    @Test
    void length_maxOnly() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v("title", "x")
                        .v(no.toll.jsondoc.JsonDocNames.MAX_LENGTH, 12)
                    .endObject()
                .endProperties()
                .toString();
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("[0..12]", vals);
    }

    @Test
    void length_minMax() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v("title", "x")
                        .v(no.toll.jsondoc.JsonDocNames.MIN_LENGTH, 30)
                        .v(no.toll.jsondoc.JsonDocNames.MAX_LENGTH, 20) // illogical ...
                    .endObject()
                .endProperties()
                .toString();
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("[30..20]", vals);
    }

    @Test
    void length_minMaxEqual() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v("title", "x")
                        .v(no.toll.jsondoc.JsonDocNames.MIN_LENGTH, 3)
                        .v(no.toll.jsondoc.JsonDocNames.MAX_LENGTH, 3)
                    .endObject()
                .endProperties()
                .toString();
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("[3]", vals);
    }

    @Test
    void minmax_minOnly() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v("title", "x")
                        .v(no.toll.jsondoc.JsonDocNames.MINIMUM, 11)
                    .endObject()
                .endProperties()
                .toString();
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("[11, ...]", vals);
    }

    @Test
    void minmax_maxOnly() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v("title", "x")
                        .v(no.toll.jsondoc.JsonDocNames.MAXIMUM, 12)
                    .endObject()
                .endProperties()
                .toString();
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("[0, 12]", vals);
    }

    @Test
    void minmax_minMax() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v("title", "x")
                        .v(no.toll.jsondoc.JsonDocNames.MINIMUM, 20)
                        .v(no.toll.jsondoc.JsonDocNames.MAXIMUM, 30)
                    .endObject()
                .endProperties()
                .toString();
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("[20, 30]", vals);
    }

    @Test
    void minmax_minMaxEqual() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v("title", "x")
                        .v(no.toll.jsondoc.JsonDocNames.MINIMUM, 3)
                        .v(no.toll.jsondoc.JsonDocNames.MAXIMUM, 3)
                    .endObject()
                .endProperties()
                .toString();
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("[3]", vals);
    }


    @Test
    void minmaxExcl_minOnly() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v("title", "x")
                        .v(no.toll.jsondoc.JsonDocNames.EXCLUSIVE_MINIMUM, 11)
                    .endObject()
                .endProperties()
                .toString();
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("<11, ...]", vals);
    }

    @Test
    void minmaxExcl_maxOnly() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v("title", "x")
                        .v(no.toll.jsondoc.JsonDocNames.EXCLUSIVE_MAXIMUM, 12)
                    .endObject()
                .endProperties()
                .toString();
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("[0, 12>", vals);
    }

    @Test
    void minmaxExcl_minMax() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v("title", "x")
                        .v(no.toll.jsondoc.JsonDocNames.EXCLUSIVE_MINIMUM, 20)
                        .v(no.toll.jsondoc.JsonDocNames.EXCLUSIVE_MAXIMUM, 30)
                    .endObject()
                .endProperties()
                .toString();
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("<20, 30>", vals);
    }

    @Test
    void type_miscConversions() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v(no.toll.jsondoc.JsonDocNames.FORMAT, "date-time")
                        .v(no.toll.jsondoc.JsonDocNames.TYPE, "string")
                        .v(no.toll.jsondoc.JsonDocNames.UNIQUE_ITEMS, true)
                        .v(no.toll.jsondoc.JsonDocNames.READ_ONLY, true)
                        .v(no.toll.jsondoc.JsonDocNames.WRITE_ONLY, true)
                        .v(no.toll.jsondoc.JsonDocNames.DEPRECATED, true)
                        .v(no.toll.jsondoc.JsonDocNames.PATTERN, "^parrot$")
                        .v(no.toll.jsondoc.JsonDocNames.PROPERTY_NAMES, "^bird$")
                        .v(no.toll.jsondoc.JsonDocNames.CONST, 42)
                        .v(no.toll.jsondoc.JsonDocNames.MULTIPLE_OF, 13)
                        .v(no.toll.jsondoc.JsonDocNames.DEFAULT, "q1")
                        .v(no.toll.jsondoc.JsonDocNames.ADDITIONAL_PROPERTIES, Boolean.FALSE)
                        .array(no.toll.jsondoc.JsonDocNames.ENUM, "1", "2", "3").endArray()
                .endObject()
                .endProperties()
                .toString();
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertTrue(vals.contains("date-time"), "format");
        assertTrue(vals.contains("string"), "type");
        assertTrue(vals.contains("1, 2, 3"), "enum");
        assertTrue(vals.contains(no.toll.jsondoc.JsonDocNames.UNIQUE_ITEMS), "unique");
        assertTrue(vals.contains(no.toll.jsondoc.JsonDocNames.READ_ONLY), "RO");
        assertTrue(vals.contains(no.toll.jsondoc.JsonDocNames.WRITE_ONLY), "WO");
        assertTrue(vals.contains(no.toll.jsondoc.JsonDocNames.DEPRECATED.toUpperCase()), "deprecated");
        assertTrue(vals.matches("(?s).*" + no.toll.jsondoc.JsonDocNames.PATTERN + ".*=.*parrot.*"), "pattern");
        assertTrue(vals.contains("==42"), "const");
        assertTrue(vals.contains(no.toll.jsondoc.JsonDocNames.MULTIPLE_OF + " 13"), "multiple");
        assertTrue(vals.contains(no.toll.jsondoc.JsonDocNames.DEFAULT + "=q1"), "default");
        assertTrue(vals.contains(no.toll.jsondoc.JsonDocNames.ADDITIONAL_DISP), vals);
        assertTrue(vals.contains(no.toll.jsondoc.JsonDocNames.PROPERTY_NAMES_DISP), vals);
    }

    @Test
    void type_ignoreBooleanSwitches() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v(no.toll.jsondoc.JsonDocNames.FORMAT, "x")
                        .v(no.toll.jsondoc.JsonDocNames.UNIQUE_ITEMS, false)
                        .v(no.toll.jsondoc.JsonDocNames.READ_ONLY, false)
                        .v(no.toll.jsondoc.JsonDocNames.WRITE_ONLY, false)
                        .v(no.toll.jsondoc.JsonDocNames.DEPRECATED, false)
                    .endObject()
                .endProperties()
                .toString();
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertFalse(vals.contains(no.toll.jsondoc.JsonDocNames.UNIQUE_ITEMS), "unique");
        assertFalse(vals.contains(no.toll.jsondoc.JsonDocNames.READ_ONLY), "RO");
        assertFalse(vals.contains(no.toll.jsondoc.JsonDocNames.WRITE_ONLY), "WO");
        assertFalse(vals.contains(no.toll.jsondoc.JsonDocNames.DEPRECATED.toUpperCase()), "deprecated");
    }

    @Test
    void extid_handlesSpecialChars() {
        final var node = new Node("(rød)", NodeType.Object, null, ctx("HTML"));
        assertEquals("r_d_", node.extId());
    }

    @Test
    void description_convertsId() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v(no.toll.jsondoc.JsonDocNames.ID, "bar")
                    .endObject()
                .endProperties()
                .toString();
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild(no.toll.jsondoc.JsonDocNames.DESCRIPTION))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertTrue(vals.contains("id="), "format");
        assertTrue(vals.contains("bar"), vals);
        assertFalse(vals.contains(no.toll.jsondoc.JsonDocNames.ID), vals);
    }

    @Test
    void specialNames_notConfused() {
        final var data = new JsonBuilder()
                .object("foo")
                    .v("type", "object")
                    .properties()
                        .object("type")
                            .v("type", "string")
                            .v("minLength", 1)
                        .endObject()
                        .object("required")
                            .v("type", "string")
                            .v("minLength", 1)
                        .endObject()
                    .endProperties()
                    .object("description")
                        .v("type", "string")
                        .v("minLength", 1)
                    .endObject()
                    .object("field")
                        .v("type", "string")
                        .v("minLength", 1)
                    .endObject()
                    .required("type", "field")
                .endObject()
                .toString();
        final var data2 = new JsonBuilder()
                .object("foo")
                    .v("type", "object")
                    .properties()
                        .object("type2")
                            .v("type", "string")
                            .v("minLength", 1)
                        .endObject()
                        .object("required2")
                            .v("type", "string")
                            .v("minLength", 1)
                        .endObject()
                        .object("description2")
                            .v("type", "string")
                            .v("minLength", 1)
                        .endObject()
                        .object("field2")
                            .v("type", "string")
                            .v("minLength", 1)
                        .endObject()
                    .endProperties()
                    .required("type2", "field2")
                .endObject()
                .toString();
        final var res = new DebugPrinter(new JsonDocParser(ctx("HTML")).parseString(data)).create()
                .replaceAll("2", "");
        final var res2 = new DebugPrinter(new JsonDocParser(ctx("HTML")).parseString(data2)).create()
                .replaceAll("2", "");
        assertEquals(res2, res);
    }

    @Test
    void defref_convertsText() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v(no.toll.jsondoc.JsonDocNames.REF, "#/$defs/foobar")
                    .endObject()
                    .object("$defs")
                        .object("foobar")
                            .v("aKey", "aValue")
                        .endObject()
                    .endObject()
                .endProperties()
                .toString();
        final var ctx = ctx("HTML");
        final var vals = new HtmlPrinter(new JsonDocParser(ctx).parseString(data), ctx).create();
        assertTrue(vals.contains("<td>($defs)</td>"), "$defs in parens");
        assertTrue(vals.contains("<td>$ref</td>"), "still uses $ref");
        assertTrue(vals.contains("href=\"#$defs__foobar\""), "contains hyperlink");
    }

    @Test @Ignore
    void embedding_correctColumns() {
        final var data = new JsonBuilder()
                .properties()
                    .v("x", "y")
                    .object("fooList")
                        .v("description", "foo")
                        .v("type", "array")
                        .array("x-sample", "oh!").endArray()
                        .object("items")
                            .v("type", "string")
                        .endObject()
                    .endObject()
                .endProperties()
                .toString();
        final var res = runHtml(data);
        System.err.println(res);
        // FIXME fail();
    }
}