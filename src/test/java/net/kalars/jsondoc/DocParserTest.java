package net.kalars.jsondoc;

import org.junit.jupiter.api.Test;

class DocParserTest {
    private static final String fileName = "C:\\data\\projects\\json-doc\\src\\test\\resources\\local-sample.json";

    @Test
    void htmlOutput() {
        final var visitor3 = new JsonDocHtmlVisitor(1);
        new JsonSchemaParser().parseFile(fileName).visit(visitor3);
        System.out.println(visitor3);
    }

    @Test
    void wikiOutput() {
        final var visitor2 = new JsonDocWikiVisitor();
        new JsonSchemaParser().parseFile(fileName).visit(visitor2);
        System.out.println(visitor2);
    }

    @Test
    void dotOutput() {
        final var visitor4 = new JsonDocDotVisitor();
        new JsonSchemaParser().parseFile(fileName).visit(visitor4);
        System.out.println(visitor4);
    }

    @Test
    void schemaOutput() {
        final var visitor1 = new JsonSchemaPrintVisitor();
        new JsonGenParser().parseFile(fileName).visit(visitor1);
        System.out.println(visitor1);
    }

    @Test
    void debugOutput() {
        final var visitor0 = new DebugVisitor();
        new JsonGenParser().parseFile(fileName).visit(visitor0);
        System.out.println(visitor0);
    }
}