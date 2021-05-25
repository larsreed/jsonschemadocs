package net.kalars.jsondoc;

import net.kalars.jsondoc.tools.JsonBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextTests {

    private Context ctx(final String mode) {
        return new Context(mode)
                .add("test", "true")
//                .add("sampleColumns", "eksempel")
//                .add(Context.SKIP_TABLES,"metadata,data")
//                .add("variant", "tags")
//                .add(Context.EXCLUDED_COLUMNS, "x-ICS2_XML")
                ;
    }

    private String runHtml(final String data) {
        final var context = ctx("HTML");
        final var root = new JsonDocParser(context).parseString(data);
        return new HtmlPrinter(root, context).toString();
    }

    private String runSchema(final String data) {
        final var context = ctx("SCHEMA");
        final var root = new JsonDocParser(context).parseString(data);
        return new HtmlPrinter(root, context).toString(); // TODO Change printer
    }

    @Test
    void excludedColumns_excludes() {
        final var data = new JsonBuilder()
                .properties()
                  .v("title", "X")
                  .v("A", 1.0)
                  .v("B", true)
                .endProperties()
                .toString();
        final var context = ctx("HTML").add(Context.EXCLUDE_COLUMNS, "Q,B,W");
        final var res = new HtmlPrinter(new JsonDocParser(context).parseString(data), context).toString();
        assertTrue(res.contains("<td>A</td>"), "A");
        assertFalse(res.contains("<td>B</td>"), "B");
    }
}