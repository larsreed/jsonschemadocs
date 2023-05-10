package no.toll.jsondoc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class SchemaTests {
    private static final String fileName =
            "C:\\data\\projects\\json-doc\\src\\test\\resources\\local-sample2.json";
//            "C:\\data\\projects\\json-doc\\src\\test\\resources\\sample4.json";

    @Test
    void schema_is_schema() {
        // Second parse should give same output as first
        final var context = new Context("SCHEMA")
                .add("test", "true")
                .add("sampleColumns", "eksempel");
        final var res1 = new SchemaPrinter(new JsonDocParser(context).parseFile(fileName)).create();
        final var res2 = new SchemaPrinter(new JsonDocParser(context).parseString(
                new SchemaPrinter(new JsonDocParser(context).parseFile(fileName)).create()))
                .create();
        assertEquals(res1, res2);
    }
}