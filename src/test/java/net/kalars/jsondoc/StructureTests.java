package net.kalars.jsondoc;

import net.kalars.jsondoc.tools.JsonBuilder;
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
        return new HtmlPrinter(new JsonDocParser(context).parseString(data)).toString();
    }

    private String runSchema(final String data) {
        final var context = ctx("SCHEMA");
        final var root = new JsonDocParser(context).parseString(data);
        return new SchemaPrinter(root).toString();
    }

    @Test
    void attributesOnTopNode_areShown() {
        final var data = new JsonBuilder().v("description", "Top!").v("title", "X").toString();
        assertTrue(runHtml(data).contains("Top!"));
    }

    @Test
    void required_areHiddenForHtmlAndShownForSchema() {
        final var data = new JsonBuilder().v("title", "X").required("A").toString();
        assertFalse(runHtml(data).contains(JsonDocNames.REQUIRED));
        assertTrue(runSchema(data).contains(JsonDocNames.REQUIRED));
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
        assertFalse(runHtml(data).contains(JsonDocNames.PROPERTIES));
        assertTrue(runSchema(data).contains(JsonDocNames.PROPERTIES));
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
        for (final var c: JsonDocNames.ALWAYS_COLUMNS) assertTrue(res.contains("<TH>" + c.toUpperCase() + "</TH>"), c);
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
        assertTrue(vals.contains(JsonDocNames.REQUIRED));
    }

    @Test
    void cardinality_minOnly() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v("title", "x")
                        .v(JsonDocNames.MIN_ITEMS, 11)
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
                        .v(JsonDocNames.MAX_ITEMS, 12)
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
    void cardinality_minMax() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v("title", "x")
                        .v(JsonDocNames.MIN_ITEMS, 30)
                        .v(JsonDocNames.MAX_ITEMS, 20) // illogical ...
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
                        .v(JsonDocNames.MIN_ITEMS, 3)
                        .v(JsonDocNames.MAX_ITEMS, 3)
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
                        .v(JsonDocNames.MIN_LENGTH, 11)
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
                        .v(JsonDocNames.MAX_LENGTH, 12)
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
                        .v(JsonDocNames.MIN_LENGTH, 30)
                        .v(JsonDocNames.MAX_LENGTH, 20) // illogical ...
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
                        .v(JsonDocNames.MIN_LENGTH, 3)
                        .v(JsonDocNames.MAX_LENGTH, 3)
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
                        .v(JsonDocNames.MINIMUM, 11)
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
                        .v(JsonDocNames.MAXIMUM, 12)
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
                        .v(JsonDocNames.MINIMUM, 20)
                        .v(JsonDocNames.MAXIMUM, 30)
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
                        .v(JsonDocNames.MINIMUM, 3)
                        .v(JsonDocNames.MAXIMUM, 3)
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
                        .v(JsonDocNames.EXCLUSIVE_MINIMUM, 11)
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
                        .v(JsonDocNames.EXCLUSIVE_MAXIMUM, 12)
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
                        .v(JsonDocNames.EXCLUSIVE_MINIMUM, 20)
                        .v(JsonDocNames.EXCLUSIVE_MAXIMUM, 30)
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
                        .v(JsonDocNames.FORMAT, "date-time")
                        .v(JsonDocNames.TYPE, "string")
                        .v(JsonDocNames.UNIQUE_ITEMS, true)
                        .v(JsonDocNames.READ_ONLY, true)
                        .v(JsonDocNames.WRITE_ONLY, true)
                        .v(JsonDocNames.DEPRECATED, true)
                        .v(JsonDocNames.PATTERN, "^parrot$")
                        .v(JsonDocNames.PROPERTY_NAMES, "^bird$")
                        .v(JsonDocNames.CONST, 42)
                        .v(JsonDocNames.MULTIPLE_OF, 13)
                        .v(JsonDocNames.DEFAULT, "q1")
                        .v(JsonDocNames.ADDITIONAL_PROPERTIES, Boolean.FALSE)
                        .array(JsonDocNames.ENUM, "1", "2", "3").endArray()
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
        assertTrue(vals.contains(JsonDocNames.UNIQUE_ITEMS), "unique");
        assertTrue(vals.contains(JsonDocNames.READ_ONLY), "RO");
        assertTrue(vals.contains(JsonDocNames.WRITE_ONLY), "WO");
        assertTrue(vals.contains(JsonDocNames.DEPRECATED.toUpperCase()), "deprecated");
        assertTrue(vals.matches("(?s).*" + JsonDocNames.PATTERN + ".*=.*parrot.*"), "pattern");
        assertTrue(vals.contains("==42"), "const");
        assertTrue(vals.contains(JsonDocNames.MULTIPLE_OF + " 13"), "multiple");
        assertTrue(vals.contains(JsonDocNames.DEFAULT + "=q1"), "default");
        assertTrue(vals.contains(JsonDocNames.ADDITIONAL_PROPERTIES_DISP), vals);
        assertTrue(vals.contains(JsonDocNames.PROPERTY_NAMES_DISP), vals);
    }

    @Test
    void type_ignoreBooleanSwitches() {
        final var data = new JsonBuilder()
                .properties()
                    .object("foo")
                        .v(JsonDocNames.FORMAT, "x")
                        .v(JsonDocNames.UNIQUE_ITEMS, false)
                        .v(JsonDocNames.READ_ONLY, false)
                        .v(JsonDocNames.WRITE_ONLY, false)
                        .v(JsonDocNames.DEPRECATED, false)
                    .endObject()
                .endProperties()
                .toString();
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertFalse(vals.contains(JsonDocNames.UNIQUE_ITEMS), "unique");
        assertFalse(vals.contains(JsonDocNames.READ_ONLY), "RO");
        assertFalse(vals.contains(JsonDocNames.WRITE_ONLY), "WO");
        assertFalse(vals.contains(JsonDocNames.DEPRECATED.toUpperCase()), "deprecated");
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
                        .v(JsonDocNames.ID, "bar")
                    .endObject()
                .endProperties()
                .toString();
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild(JsonDocNames.DESCRIPTION))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertTrue(vals.contains("id="), "format");
        assertTrue(vals.contains("bar"), vals);
        assertFalse(vals.contains(JsonDocNames.ID), vals);
    }
}