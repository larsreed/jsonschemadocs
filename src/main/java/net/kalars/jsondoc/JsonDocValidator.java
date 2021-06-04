package net.kalars.jsondoc;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class JsonDocValidator {

    private ValidationResult result = new ValidationResult("root");

    ValidationResult validateString(final String schemaFile, final String data) {
        try (final FileInputStream schemaStream = new FileInputStream(schemaFile)) {
            final JSONObject rawSchema = new JSONObject(new JSONTokener(schemaStream));
            final Schema schema = SchemaLoader.load(rawSchema);
            schema.validate(new JSONObject(data)); // throws a ValidationException if this object is invalid
            return result;
        }
        catch (final IOException | JSONException e) {
            result.add(e.getMessage());
            result.fail();
            return result;
        }
        catch (final ValidationException e) {
            final var causes = e.getCausingExceptions();
            causes.forEach(this::handle);
            result.fail();
            return result;
        }
    }

    ValidationResult validateFile(final String schemaFile, final String dataFile) {
        result = new ValidationResult(dataFile, result);
        try (final FileInputStream dataStream = new FileInputStream(dataFile)) {
            final var br = new BufferedReader(new InputStreamReader(dataStream));
            String line;
            final StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return validateString(schemaFile, sb.toString());
        }
        catch (final IOException e) {
            result.add(e.getMessage());
            return result;
        }
    }

    private void handle(final ValidationException e) {
        final StringBuilder buf = new StringBuilder();
        buf.append(e.getMessage())
                .append(" Keyword: ")
                .append(e.getKeyword())
                .append("  Pointer: ")
                .append(e.getPointerToViolation())
                .append("  Definition: ")
                .append(e.getSchemaLocation());
        result.add(buf.toString());
        final var causes = e.getCausingExceptions();
        if (causes!=null) causes.forEach(this::handle);
    }
}

class ValidationResult {
    private boolean ok = true;
    private final Map<String, List<String>> messages = new LinkedHashMap<>();
    private final String thisFile;

    ValidationResult(final String file) { thisFile = file; }
    void fail() { ok = false; }
    boolean isOk() { return ok; }

    ValidationResult(final String file, final ValidationResult copy) {
        this(file);
        if (!copy.ok) ok = false;
        messages.putAll(copy.messages);
    }

    void add(final String err) {
        messages.putIfAbsent(thisFile, new LinkedList<>());
        messages.get(thisFile).add(err);
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