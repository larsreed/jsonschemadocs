package net.kalars.jsondoc;

import org.junit.jupiter.api.Test;

class DocParserTest {
    private static final String fileName =
            "C:\\data\\projects\\json-doc\\src\\test\\resources\\local-sample.json";
//            "C:\\data\\projects\\json-doc\\src\\test\\resources\\sample.json";

    private Context ctx(final String mode) {
        return new Context(mode)
                .add("test", "true")
//                .add("variant", "tags")
//                .add(JsonContext.EXCLUDED_COLUMNS, "description, custom")
                ;
    }

    @Test
    void htmlOutput() {
        final var context = ctx("HTML");
        final var visitor3 = new JsonDocHtmlVisitor(context);
        new JsonSchemaParser().parseFile(fileName).visit(visitor3);
        System.out.println(visitor3);
    }


    @Test
    void wikiOutput() {
        final var context = ctx("WIKI");
        final var visitor2 = new JsonDocWikiVisitor(context);
        new JsonSchemaParser().parseFile(fileName).visit(visitor2);
        System.out.println(visitor2);
    }

    @Test
    void dotOutput() {
        final var context = ctx("GRAPH");
        final var visitor4 = new JsonDocDotVisitor(context);
        new JsonSchemaParser().parseFile(fileName).visit(visitor4);
        System.out.println(visitor4);
    }

    @Test
    void schemaOutput() {
        final var context = ctx("SCHEMA");
        final var visitor1 = new JsonSchemaPrintVisitor(context);
        new JsonGenParser().parseFile(fileName).visit(visitor1);
        System.out.println(visitor1);
    }

    @Test
    void debugOutput() {
        final var context = ctx("DEBUG");
        final var visitor0 = new DebugVisitor(context);
        new JsonGenParser().parseFile(fileName).visit(visitor0);
        System.out.println(visitor0);
    }
}