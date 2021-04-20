package net.kalars.jsondoc;

import org.junit.jupiter.api.Test;

class DocParserTest {
    private static final String fileName =
//            "C:\\data\\projects\\json-doc\\src\\test\\resources\\local-sample.json";
            "C:\\data\\projects\\json-doc\\src\\test\\resources\\sample.json";


    @Test
    void htmlOutput() {
        final var context = new JsonContext("HTML");
        context.add("variant","tags");
        final var visitor3 = new JsonDocHtmlVisitor(context);
        new JsonSchemaParser().parseFile(fileName).visit(visitor3);
        System.out.println(visitor3);
    }

    @Test
    void wikiOutput() {
        final var context = new JsonContext("WIKI");
        final var visitor2 = new JsonDocWikiVisitor(context);
        new JsonSchemaParser().parseFile(fileName).visit(visitor2);
        System.out.println(visitor2);
    }

    @Test
    void dotOutput() {
        final var context = new JsonContext("GRAPH");
        final var visitor4 = new JsonDocDotVisitor(context);
        new JsonSchemaParser().parseFile(fileName).visit(visitor4);
        System.out.println(visitor4);
    }

    @Test
    void schemaOutput() {
        final var context = new JsonContext("SCHEMA");
        final var visitor1 = new JsonSchemaPrintVisitor(context);
        new JsonGenParser().parseFile(fileName).visit(visitor1);
        System.out.println(visitor1);
    }

    @Test
    void debugOutput() {
        final var context = new JsonContext("DEBUG");
        final var visitor0 = new DebugVisitor(context);
        new JsonGenParser().parseFile(fileName).visit(visitor0);
        System.out.println(visitor0);
    }
}