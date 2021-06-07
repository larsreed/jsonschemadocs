package net.kalars.jsondoc;

import net.kalars.jsondoc.tools.JsonBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class ValidationTests {

    private String simpleSchema() {
        return new JsonBuilder()
                .v("$schema", "http://json-schema.org/draft-07/schema")
                .v("type", "object")
                .properties()
                    .object("foo")
                        .v(JsonDocNames.TYPE, "string")
                        .v(JsonDocNames.DESCRIPTION, "fox")
                    .endObject()
                    .object("bar")
                        .v(JsonDocNames.TYPE, "integer")
                        .v(JsonDocNames.DESCRIPTION, "box")
                    .endObject()
                .endProperties()
                .required("foo")
                .toString();
    }

    @Test
    void validation_non_strict_ok() {
        final var data = new JsonBuilder()
                .v("foo", "bad")
                .v("bar", 1)
                .v("baz", "bam")
                .toString();
        final var schemaFile = Validator.makeTempSchema(simpleSchema());
        final var result = new Validator().validateString(schemaFile, data);
        assertTrue(result.isOk(), result.toString());
    }

    @Test
    void validation_failsOnWrongType() {
        final var data = new JsonBuilder()
                .v("foo", "bad")
                .v("bar", "one")
                .v("baz", "bam")
                .toString();
        final var schemaFile = Validator.makeTempSchema(simpleSchema());
        final var result = new Validator().validateString(schemaFile, data);
        assertFalse(result.isOk(), result.toString());
    }

    @Test
    void validation_failsOnMissingRequired() {
        final var data = new JsonBuilder()
                .v("bar", 1)
                .toString();
        final var schemaFile = Validator.makeTempSchema(simpleSchema());
        final var result = new Validator().validateString(schemaFile, data);
        assertFalse(result.isOk(), result.toString());
    }

    @Test
    void validation_fails_extra_attribute_on_strict() {
        final var data = new JsonBuilder()
                .v("foo", "bad")
                .v("bar", 1)
                .v("baz", "bam")
                .toString();
        final var schemaFile = Validator.makeTempSchema(simpleSchema());
        final var dataFile = Validator.makeTempSchema(data);
        final var context = new Context("VALIDATE")
                .add(Context.STRICT, "true")
                .add(Context.FILES, dataFile);
        final var result = Validator.validate(schemaFile, context);
        assertFalse(result.isOk(), result.toString());
    }
}