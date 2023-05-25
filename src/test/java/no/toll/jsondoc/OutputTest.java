package no.toll.jsondoc;

import org.junit.jupiter.api.Test;


class OutputTest {
    private static final String schema = """
            {
              "$id": "https://example.com/person.schema.json",
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "title": "Person",
              "type": "object",
              "properties": {
                "firstName": {
                  "type": "string",
                  "description": "The person's first name."
                },
                "lastName": {
                  "type": "string",
                  "description": "The person's last name."
                },
                "age": {
                  "description": "Age in years which must be equal to or greater than zero.",
                  "type": "integer",
                  "minimum": 0
                }
              }
            }""";

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
        final var root = new JsonDocParser(context).parseString(schema);
        final var printer = new HtmlPrinter(root, context);
        System.out.println(printer.create());
    }

    @Test
    void wikiHtmlOutput() {
        final var context = ctx("HTML");
        final var root = new JsonDocParser(context).parseString(schema);
        final var printer = new WikiPrinter(root, context);
        System.out.println(printer.create());
    }

    @Test
    void markdownOutput() {
        final var context = ctx("MARKDOWN");
        final var root = new JsonDocParser(context).parseString(schema);
        final var printer = new MarkdownPrinter(root);
        System.out.println(printer.create());
    }

    @Test
    void graphOutput() {
        final var context = ctx("GRAPH");
        final var root = new JsonDocParser(context).parseString(schema);
        final var printer = new GraphPrinter(root);
        System.out.println(printer.create());
    }

    @Test
    void sampleOutput() {
        final var context = ctx("SAMPLE");
        final var root = new JsonDocParser(context).parseString(schema);
        final var printer = new SamplePrinter(root, context);
        System.out.println(printer.create());
    }

    @Test
    void schemaOutput() {
        final var context = ctx("SCHEMA").add(Context.STRICT, Boolean.TRUE+"");
        final var root = new JsonDocParser(context).parseString(schema);
        final var printer = new SchemaPrinter(root);
        System.out.println(printer.create());
    }

    @Test
    void debugOutput() {
        final var ctx = ctx("DEBUG");
        final var node = new JsonDocParser(ctx).parseString(schema);
        final var printer = new DebugPrinter(node);
        System.out.println(printer.create());
    }
}