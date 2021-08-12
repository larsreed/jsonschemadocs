package net.kalars.jsondoc;

import net.kalars.jsondoc.tools.JsonBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;


class OutputTest {
    private static final String fileName =
            "C:\\data\\projects\\json-doc\\src\\test\\resources\\local-sample8.json";
    private static final String schemaEx =
        "C:\\data\\projects\\json-doc\\src\\test\\resources\\local-sample3-schema.json";
    private static final String fileEx =
            "C:\\data\\projects\\json-doc\\src\\test\\resources\\local-sample3.json";

    private Context ctx(final String mode) {
        return new Context(mode)
                .add("test", "true")
                .add("sampleColumns", "x-eksempel")
//                .add(Context.SKIP_TABLES,"metadata,data")
//                .add("variant", "tags")
//                .add(Context.EXCLUDED_COLUMNS, "x-ICS2_XML")
                ;
    }
//
    @Test
    void htmlOutput() {
        final var context = ctx("HTML");
        final var root = new JsonDocParser(context).parseFile(fileName);
        final var printer = new HtmlPrinter(root, context);
        System.out.println(printer);
    }

    @Test
    void wikiHtmlOutput() {
        final var context = ctx("HTML");
        final var root = new JsonDocParser(context).parseFile(fileName);
        final var printer = new WikiPrinter(root, context);
        System.out.println(printer);
    }

    @Test
    void markdownOutput() {
        final var context = ctx("MARKDOWN");
        final var root = new JsonDocParser(context).parseFile(fileName);
        final var printer = new MarkdownPrinter(root);
        System.out.println(printer);
    }

    @Test
    void graphOutput() {
        final var context = ctx("GRAPH");
        final var root = new JsonDocParser(context).parseFile(fileName);
        final var printer = new GraphPrinter(root);
        System.out.println(printer);
    }

    @Test
    void sampleOutput() {
        final var context = ctx("SAMPLE");
        final var root = new JsonDocParser(context).parseFile(fileName);
        final var printer = new SamplePrinter(root, context);
        System.out.println(printer);
    }

    @Test
    void schemaOutput() {
        final var context = ctx("SCHEMA").add(Context.STRICT, Boolean.TRUE+"");
        final var root = new JsonDocParser(context).parseFile(fileName);
        final var printer = new SchemaPrinter(root);
        System.out.println(printer);
    }

    @Test
    void debugOutput() {
        final var ctx = ctx("DEBUG");
        final var node = new JsonDocParser(ctx).parseFile(fileName);
        final var printer = new DebugPrinter(node);
        System.out.println(printer);
    }

    @Test
    void validationOutput() {
        final var validator = new Validator();
        final var ok = validator.validateFile(schemaEx, fileEx);
        assertTrue(ok.isOk(), ok.toString());
    }

    @Test
    void builderOutput() {
        final var s = new JsonBuilder()
                .v("title", "x")
                .object("properties")
                  .v("x-test", 4)
                  .v("x-y", false)
                .endObject()
                .required("x-test")
                .toString();
        System.out.println(s);
    }
}