package no.toll.jsondoc;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GeneratorTests {

    private static Path tempDir;

    @SuppressWarnings("unused")
    private final String schema = SchemaTests.SCHEMA;

    @BeforeAll
    static void beforeAll() { tempDir = JsonDoc.tempDir(); }

    @SuppressWarnings("unused")
    @Test()
    void illegalLanguageFails() {
        final var context = new Context("GENERATE");
        final var _unused = new JsonCodeGen(context, tempDir);
        context.add(Context.CODE, "jAVa");
        final var _also_unused = new JsonCodeGen(context, tempDir);
        context.add(Context.CODE, "Fortran");
        assertThrows(RuntimeException.class, () -> new JsonCodeGen(context, tempDir));
    }
}