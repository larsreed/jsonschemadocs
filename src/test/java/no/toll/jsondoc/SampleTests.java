package no.toll.jsondoc;

import no.toll.jsondoc.tools.JsonBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleTests {

    private Context ctx(final String mode) {
        return new Context(mode)
                .add("test", "true")
//                .add("sampleColumns", "eksempel")
//                .add(Context.SKIP_TABLES,"metadata,data")
//                .add("variant", "tags")
//                .add(Context.EXCLUDED_COLUMNS, "x-ICS2_XML")
                ;
    }

    @Test
    void sample_willCreateIfNotPresent() {
        final var data = new JsonBuilder()
                .properties()
                    .v("title", "X")
                    .object("foo")
                        .v("type", "number")
                    .endObject()
                    .object("bar")
                        .v("type", "string")
                    .endObject()
                .endProperties()
                .toString();
        final var context = ctx("SAMPLE").add(Context.SAMPLE_COLUMNS, "x-smp");
        final var res = new SamplePrinter(new JsonDocParser(context).parseString(data), context).toString();
        assertTrue(res.matches("(?s).*foo.: [-.0-9]+.*"), res);
        assertTrue(res.matches("(?s).*bar.:.*"), res);
    }

    @Test
    void sample_willUseNamedColumn() {
        final var data = new JsonBuilder()
                .properties()
                    .v("title", "X")
                    .object("foo")
                        .v("type", "number")
                        .v("x-smp", 8048.6)
                    .endObject()
                    .object("bar")
                        .v("type", "string")
                        .v("x-smp", "smpl")
                    .endObject()
                .endProperties()
                .toString();
        final var context = ctx("SAMPLE").add(Context.SAMPLE_COLUMNS, "x-smp");
        final var res = new SamplePrinter(new JsonDocParser(context).parseString(data), context).toString();
        assertFalse(res.matches("(?s).*x-smp.*"), res);
        assertTrue(res.matches("(?s).*foo.: 8048.6.*"), res);
        assertTrue(res.matches("(?s).*bar.:.*smpl.*"), res);
    }

    @Test
    void sample_canUseExamples() {
        final var data = new JsonBuilder()
                .v("title", "X")
                .properties()
                    .object("foo")
                        .v("type", "number")
                        .v("x-smp", 8048.6)
                        .array(JsonDocNames.EXAMPLES, "747", "757", "777").endArray()
                    .endObject()
                    .object("bar")
                        .v("type", "string")
                        .array(JsonDocNames.EXAMPLES, "747", "757", "777").endArray()
                    .endObject()
                .endProperties()
                .toString();
        final var context = ctx("SAMPLE").add(Context.SAMPLE_COLUMNS, "x-smp");
        final var rootNode = new JsonDocParser(context).parseString(data);
        final var res = new SamplePrinter(rootNode, context).toString();
        assertFalse(res.matches("(?s).*x-smp.*"), res);
        assertTrue(res.matches("(?s).*foo.: 8048.6.*"), res);
        assertTrue(res.matches("(?s).*bar.:.*747.*"), res);
    }

    @Test
    void sample_canUseConstants() {
        final var data = new JsonBuilder()
                .v("title", "X")
                .properties()
                    .object("foo")
                        .v("type", "number")
                        .v(JsonDocNames.CONST, 42)
                    .endObject()
                    .object("bar")
                        .v("type", "string")
                        .v(JsonDocNames.CONST, "forty-two")
                    .endObject()
                .endProperties()
                .toString();
        final var context = ctx("SAMPLE");
        final var rootNode = new JsonDocParser(context).parseString(data);
        final var res = new SamplePrinter(rootNode, context).toString();
        assertTrue(res.matches("(?s).*foo.: 42.*"), res);
        assertTrue(res.matches("(?s).*bar.:.*forty-two.*"), res);
    }

    @Test
    void sample_canUseEnum() {
        final var data = new JsonBuilder()
                .v("title", "X")
                .properties()
                    .object("foo")
                        .v("type", "number")
                        .array(JsonDocNames.ENUM, 42, 43, 44).endArray()
                    .endObject()
                    .object("bar")
                        .v("type", "string")
                        .array(JsonDocNames.ENUM, "alfa", "bravo", "charlie").endArray()
                    .endObject()
                .endProperties()
                .toString();
        final var context = ctx("SAMPLE");
        final var rootNode = new JsonDocParser(context).parseString(data);
        final var res = new SamplePrinter(rootNode, context).toString();
        assertTrue(res.matches("(?s).*foo.: 42.*"), res);
        assertTrue(res.matches("(?s).*bar.:.*alfa.*"), res);
    }

    @Test
    void sample_generatesBigNumbers() {
        final var data = new JsonBuilder()
                .v("title", "X")
                .properties()
                    .object("foo")
                        .v("type", "number")
                        .vo(JsonDocNames.MINIMUM, "9999999999999990")
                        .vo(JsonDocNames.MAXIMUM, "9999999999999999")
                    .endObject()
                .endProperties()
                .toString();
        final var context = ctx("SAMPLE");
        final var rootNode = new JsonDocParser(context).parseString(data);
        final var res = new SamplePrinter(rootNode, context).toString();
        assertTrue(res.matches("(?s).*foo.: 999999999999999[0-9].*"), res);
    }

    @Test
    void sample_emptyExamplesOmitsSample() {
        final var data = new JsonBuilder()
                .v("title", "X")
                .properties()
                    .object("foo")
                        .v("type", "array")
                        .vo("examples", "[]")
                    .endObject()
                    .object("bar")
                        .v("type", "string")
                        .array(JsonDocNames.ENUM, "alfa", "bravo", "charlie").endArray()
                    .endObject()
                .endProperties()
                .toString();
        final var context = ctx("SAMPLE");
        final var rootNode = new JsonDocParser(context).parseString(data);
        final var res = new SamplePrinter(rootNode, context).toString();
        assertFalse(res.matches("(?s).*foo.*"), res);
    }
}