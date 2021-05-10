package net.kalars.jsondoc;

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

    @Test
    void htmlOutput() {
        final var visitor = new JsonDocHtmlVisitor(ctx("HTML"));
        new JsonSchemaParser().parseFile(fileName).visit(visitor);
        System.out.println(visitor);
    }

    @Test
    void wikiHtmlOutput() {
        final var visitor = new JsonDocWikiHtmlVisitor(ctx("HTML"));
        new JsonSchemaParser().parseFile(fileName).visit(visitor);
        System.out.println(visitor);
    }

    @Test
    void markdownOutput() {
        final var visitor = new JsonDocMarkdownVisitor(ctx("MARKDOWN"));
        new JsonSchemaParser().parseFile(fileName).visit(visitor);
        System.out.println(visitor);
    }

    @Test
    void dotOutput() {
        final var visitor = new JsonDocDotVisitor(ctx("GRAPH"));
        new JsonSchemaParser().parseFile(fileName).visit(visitor);
        System.out.println(visitor);
    }

    @Test
    void sampleOutput() {
        final var visitor = new JsonSamplePrintVisitor(ctx("SAMPLE"));
        new JsonSchemaParser().parseFile(fileName).visit(visitor);
        System.out.println(visitor);
    }

    @Test
    void schemaOutput() {
        final var visitor = new JsonSchemaPrintVisitor(ctx("SCHEMA"));
        new JsonGenParser().parseFile(fileName).visit(visitor);
        System.out.println(visitor);
    }

    @Test
    void debugOutput() {
        final var visitor = new DebugVisitor(ctx("DEBUG"));
        new JsonGenParser().parseFile(fileName).visit(visitor);
        System.out.println(visitor);
    }
}