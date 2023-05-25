package no.toll.jsondoc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class ValidationTests {

    private String simpleSchema = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "foo": {
                      "type": "string",
                      "description": "fox"
                    },
                    "bar": {
                      "type": "integer",
                      "description": "box"
                    }
                  },
                  "required": [
                    "foo"
                  ]
                }""";

    @Test
    void validation_non_strict_ok() {
        final var data = """
                {
                  "foo": "bad",
                  "bar": 1,
                  "baz": "bam"
                }""";
        final var schemaFile = GeneralJSONValidator.makeTempSchema(simpleSchema);
        final var result = new GeneralJSONValidator().validateString(schemaFile, data);
        assertTrue(result.isOk(), result.toString());
    }

    @Test
    void validation_failsOnWrongType() {
        final var data = """
                {
                  "foo": "bad",
                  "bar": "one",
                  "baz": "bam"
                }""";
        final var schemaFile = GeneralJSONValidator.makeTempSchema(simpleSchema);
        final var result = new GeneralJSONValidator().validateString(schemaFile, data);
        assertFalse(result.isOk(), result.toString());
    }

    @Test
    void validation_failsOnMissingRequired() {
        final var data = """
                {
                  "bar": 1
                }""";
        final var schemaFile = GeneralJSONValidator.makeTempSchema(simpleSchema);
        final var result = new GeneralJSONValidator().validateString(schemaFile, data);
        assertFalse(result.isOk(), result.toString());
    }

    @Test
    void validation_fails_extra_attribute_on_strict() {
        final var data = """
                {
                  "foo": "bad",
                  "bar": 1,
                  "baz": "bam"
                }""";
        final var schemaFile = GeneralJSONValidator.makeTempSchema(simpleSchema);
        final var dataFile = GeneralJSONValidator.makeTempSchema(data);
        final var context = new Context("VALIDATE")
                .add(Context.STRICT, "true")
                .add(Context.FILES, dataFile);
        final var result = GeneralJSONValidator.validate(schemaFile, context);
        assertFalse(result.isOk(), result.toString());
    }
}