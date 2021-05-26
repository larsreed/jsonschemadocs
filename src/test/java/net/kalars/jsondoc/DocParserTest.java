package net.kalars.jsondoc;

import net.kalars.jsondoc.tools.JsonBuilder;
import org.junit.jupiter.api.Test;

class DocParserTest {
    private static final String fileName =
            "C:\\data\\projects\\json-doc\\src\\test\\resources\\local-sample2.json";
//            "C:\\data\\projects\\json-doc\\src\\test\\resources\\sample4.json";

    private Context ctx(final String mode) {
        return new Context(mode)
                .add("test", "true")
                .add("sampleColumns", "eksempel")
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
        final var printer = new MarkdownPrinter(root, context);
        System.out.println(printer);
    }

    @Test
    void graphOutput() {
        final var context = ctx("GRAPH");
        final var root = new JsonDocParser(context).parseFile(fileName);
        final var printer = new GraphPrinter(root, context);
        System.out.println(printer);
    }

//    @Test
//    void sampleOutput() {
//        final var visitor = new JsonSamplePrintVisitor(ctx("SAMPLE"));
//        new JsonSchemaParser().parseFile(fileName).visit(visitor);
//        System.out.println(visitor);
//    }
//
//    @Test
//    void schemaOutput() {
//        final var visitor = new JsonSchemaPrintVisitor(ctx(Context.SCHEMA_MODE));
//        new JsonGenParser().parseFile(fileName).visit(visitor);
//        System.out.println(visitor);
//    }
//
    @Test
    void debugOutput() {
        final var ctx = ctx("DEBUG");
        final var node = new JsonDocParser(ctx).parseFile(fileName);
        final var printer = new DebugPrinter(node, ctx);
        System.out.println(printer);
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