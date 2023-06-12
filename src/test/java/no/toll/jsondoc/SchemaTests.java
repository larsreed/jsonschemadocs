package no.toll.jsondoc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class SchemaTests {
    /** Shared with {@link no.toll.jsondoc.GeneratorTests }*/
    static final String SCHEMA = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "title": "TEST",
              "type": "object",
              "properties": {
                "metadata": {
                  "description": "Metadata for denne hendelsen",
                  "type": "object",
                  "properties": {
                    "eventId": {
                      "description": "Unik identifikator for denne hendelsen med UUID v4 format",
                      "type": "string",
                      "pattern": "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
                      "examples": [
                        "c7b80696-1e25-4092-9e3f-42c2719ac438"
                      ]
                    },
                    "correlationId": {
                      "type": "string",
                      "description": "Identifikator som brukes for korrelasjon av meldingen på tvers av systemer",
                      "examples": [
                        "c05a4000-f455-4876-90f9-3b237374a651"
                      ]
                    },
                    "created": {
                      "description": "ISO 8601 UTC timestamp for når \\nlinkTo(URL)\\ndenne hendelsen ble opprettet",
                      "type": "string",
                      "format": "date-time",
                      "examples": [
                        "2023-03-01T07:19:10Z"
                      ]
                    },
                    "version": {
                      "type": "string",
                      "description": "Hendelsesversjonen, brukes typisk for kompatibilitet",
                      "const": "10"
                    }
                  },
                  "required": [
                    "eventId",
                    "correlationId",
                    "created",
                    "version"
                  ]
                }
              },
              "required": [
                "metadata"
              ]
            }""";

    @Test
    void schema_is_schema() {
        // Second parse should give same output as first
        final var context = new Context("SCHEMA")
                .add("test", "true")
                .add("sampleColumns", "eksempel");
        final var res1 = new SchemaPrinter(new JsonDocParser(context).parseString(SCHEMA)).create();
        final var res2 = new SchemaPrinter(new JsonDocParser(context).parseString(
                new SchemaPrinter(new JsonDocParser(context).parseString(SCHEMA)).create()))
                .create();
        assertEquals(res1, res2);
    }
}