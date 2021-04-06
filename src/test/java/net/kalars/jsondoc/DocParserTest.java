package net.kalars.jsondoc;

import org.junit.jupiter.api.Test;

class DocParserTest {
    private static final String fileName = "C:\\data\\projects\\json-doc\\src\\test\\resources\\local-sample.json";

    @Test
    void sampleRun() {
        final var parser0 = new JsonGenParser();
        final var parser1 = new JsonSchemaParser();

        final var node0 = parser0.parseFile(fileName);
        final var node1 = parser1.parseFile(fileName);

        final var visitor0 = new DebugVisitor();
        final var visitor1 = new JsonSchemaPrintVisitor();
        final var visitor2 = new JsonDocWikiVisitor();
        final var visitor3 = new JsonDocHtmlVisitor();

        node1.visit(visitor0);
        node1.visit(visitor1);
        node1.visit(visitor2);
        node1.visit(visitor3);

        System.out.println(visitor3);
        System.out.println(visitor2);
        System.out.println(visitor1);
        System.out.println(visitor0);
    }

}