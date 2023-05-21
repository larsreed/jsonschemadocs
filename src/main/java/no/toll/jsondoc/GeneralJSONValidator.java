package no.toll.jsondoc;


import net.jimblackler.jsonschemafriend.GenerationException;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.ValidationException;
import net.jimblackler.jsonschemafriend.Validator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class GeneralJSONValidator {

    private ValidationResult result = new ValidationResult("input");

    static ValidationResult validate(final String inputfile, final Context context) {
        final var parser = new JsonDocParser(context.clone(Context.SCHEMA_MODE));
        final var printer = new SchemaPrinter(parser.parseFile(inputfile));
        final String pureSchema = makeTempSchema(printer.create());

        final var validator = new GeneralJSONValidator();
        final var optFiles = context.value(Context.FILES);
        if (optFiles.isEmpty())
            return new ValidationResult(inputfile).fail().add("No " + Context.FILES + "= specified");
        final var files = Arrays.stream(optFiles.get().split(", *")).toList();
        if (files.isEmpty()) return new ValidationResult(inputfile).fail().add("No " + Context.FILES + "= specified");

        ValidationResult res = new ValidationResult(""); // forgotten on first file
        for (final var file : files)  res = validator.validateFile(pureSchema, file);
        return res;
    }

    static String makeTempSchema(final String data)  {
        try {
            final Path jschema = Files.createTempFile("jschema", ".json");
            jschema.toFile().deleteOnExit();
            try (final OutputStream out = Files.newOutputStream(jschema)) {
                out.write(data.getBytes());
                return jschema.toString();
            }
        }
        catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    ValidationResult validateString(final String schemaFile, final String data) {
        try {
            final SchemaStore schemaStore = new SchemaStore(); // Initialize a SchemaStore.
            final Schema schema = schemaStore.loadSchema(Paths.get(schemaFile).toUri());
            final Validator validator = new Validator(); // Create a validator.
            validator.validateJson(schema, data);
            return result;
        }
        catch (final GenerationException e) {
            return result.add(e.getMessage()).fail();
        }
        catch (final ValidationException e) {
            handle(e);
            return result.fail();
        }
    }

    ValidationResult validateFile(final String schemaFile, final String dataFile) {
        result = new ValidationResult(dataFile, result);
        try (final FileInputStream dataStream = new FileInputStream(dataFile)) {
            final var br = new BufferedReader(new InputStreamReader(dataStream));
            String line;
            final StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null)  sb.append(line);
            return validateString(schemaFile, sb.toString());
        }
        catch (final IOException e) {
            return result.add(e.getMessage()).fail();
        }
    }

    private void handle(final ValidationException e) {
        final String buf = e.getMessage();
        result.add(buf);
    }
}

class ValidationResult {
    private boolean ok = true;
    private final Map<String, List<String>> messages = new LinkedHashMap<>();
    private final String thisFile;

    ValidationResult(final String file) { thisFile = file; }
    ValidationResult fail() { ok = false; return this; }
    boolean isOk() { return ok; }

    ValidationResult(final String file, final ValidationResult copy) {
        this(file);
        if (!copy.ok) ok = false;
        messages.putAll(copy.messages);
    }

    ValidationResult add(final String err) {
        messages.putIfAbsent(thisFile, new LinkedList<>());
        messages.get(thisFile).add(err);
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        if (isOk()) buf.append("ok\n");
        messages.keySet().forEach(k -> {
            buf.append(k).append(":\n");
            final var strings = messages.get(k);
            strings.forEach(s -> buf.append("  ").append(s).append('\n'));
        });
        return buf.toString();
    }
}