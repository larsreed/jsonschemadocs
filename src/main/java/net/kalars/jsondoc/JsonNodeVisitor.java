package net.kalars.jsondoc;

import org.apache.commons.text.StringEscapeUtils;

import java.util.*;

/** Callbacks for iterating over a JSON structure. */
interface JsonNodeVisitor {
    boolean topNode(JsonTopNode jsonTopNode);
    void topNodeLeave(JsonTopNode jsonTopNode);

    boolean object(JsonObject jsonObject);
    void objectLeave(JsonObject jsonObject);

    boolean array(JsonArray jsonArray);
    void arrayLeave(JsonArray jsonArray);

    void value(JsonValue jsonValue);
    void keyValue(JsonKeyValue jsonKeyValue);
}


/** Base class for print visitors. */
abstract class AbstractPrintVisitor implements JsonNodeVisitor {
    protected final StringBuilder sb = new StringBuilder();

    @Override public String toString() { return this.sb.toString(); }

    String makeIndent(final JsonBasicNode node, final int extra) {
        return " ".repeat(extra + 2 * node.tokenDepth);
    }
}


@SuppressWarnings("UnnecessaryReturnStatement")
class DebugVisitor extends AbstractPrintVisitor {

    private boolean printer(final JsonBasicNode node, final String s1, final String s2, final String s3) {
        this.sb.append(makeIndent(node, 0))
                .append(s1)
                .append(":")
                .append(node.isRequired()? " * " : " ")
                .append(node.qName)
                .append(" > ")
                .append(s2)
                .append("  ")
                .append(s3)
                .append("\n");
        if (node instanceof JsonSchemaObject) {
            final var props = ((JsonSchemaObject) node).props;
            props.forEach((k, v) ->
                    this.sb.append(makeIndent(node, 1))
                            .append("[")
                            .append(k)
                            .append("=")
                            .append(v)
                            .append("]\n"));
        }
        return true;
    }

    @Override public void topNodeLeave(final JsonTopNode n) { return; }
    @Override public void objectLeave(final JsonObject n) { return; }
    @Override public void arrayLeave(final JsonArray n) { return; }

    @Override public boolean topNode(final JsonTopNode n) { return printer(n,"TopNode", n.name, ""); }
    @Override public boolean object(final JsonObject n) { return printer(n,"Object", n.name, ""); }
    @Override public boolean array(final JsonArray n) { return printer(n,"Array", n.name, ""); }
    @Override public void value(final JsonValue n) { printer(n,"Value", n.value, ""); }
    @Override public void keyValue(final JsonKeyValue n) { printer(n,"KeyValue", n.key, n.value); }
}


/** Copies the input, except for elements with a given set of prefixes. */
class JsonSchemaPrintVisitor extends AbstractPrintVisitor {
    private static final List<String> EXCLUDE_PREFIXES =
            Arrays.asList(JsonDocNames.IGNORE_PREFIX, JsonDocNames.XDOC_PREFIX);

    @Override
    public void value(final JsonValue value) {
        if (EXCLUDE_PREFIXES.stream().anyMatch(value.value::startsWith)) return;
        this.sb.append(makeIndent(value, 2))
                .append(value.value)
                .append(",\n");
    }

    @Override
    public void keyValue(final JsonKeyValue node) {
        if (EXCLUDE_PREFIXES.stream().anyMatch(node.key::startsWith)) return;
        this.sb.append(makeIndent(node, 0))
                .append("\"")
                .append(node.key)
                .append("\": ")
                .append(node.value)
                .append(",\n");
    }

    @Override
    public boolean topNode(final JsonTopNode topNode) {
        this.sb.append("{\n");
        return true;
    }

    @Override
    public void topNodeLeave(final JsonTopNode topNode) {
        this.sb.setLength((this.sb.length()-2));
        this.sb.append("\n}\n");
    }

    @Override
    public boolean object(final JsonObject object) {
        if (EXCLUDE_PREFIXES.stream().anyMatch(object.name::startsWith)) return false;
        this.sb.append(makeIndent(object, 0))
                .append("\"")
                .append(object.name)
                .append("\": {\n");
        return true;
    }

    @Override
    public void objectLeave(final JsonObject object) {
        if (EXCLUDE_PREFIXES.stream().anyMatch(object.name::startsWith)) return;
        final var indent = makeIndent(object, 0);
        if (object instanceof JsonSchemaObject ) {
            final var props = ((JsonSchemaObject) object).props;
            props.forEach((k, v) ->
                    this.sb.append(indent)
                            .append(" //")
                            .append(k)
                            .append(" = ")
                            .append(v)
                            .append("\n"));
        }
        this.sb.setLength((this.sb.length()-2));
        this.sb.append("\n")
                .append(indent)
                .append("},\n");
    }

    @Override
    public boolean array(final JsonArray array) {
        if (EXCLUDE_PREFIXES.stream().anyMatch(array.name::startsWith)) return false;
        this.sb.append(makeIndent(array, 0))
                .append("\"")
                .append(array.name)
                .append("\": [\n");
        return true;
    }

    @Override
    public void arrayLeave(final JsonArray array) {
        if (EXCLUDE_PREFIXES.stream().anyMatch(array.name::startsWith)) return;
        this.sb.setLength((this.sb.length()-2));
        this.sb.append("\n")
                .append(makeIndent(array, 0))
                .append("],\n");
    }
}


/** Prints schema documentation. */
abstract class JsonDocPrintVisitor extends AbstractPrintVisitor {

    public static final String SEPARATOR = " > ";

    /** One documentation table instance. */
    final static class DocTable {
        private static final List<String> ALWAYS_COLUMNS = Arrays.asList(
                JsonDocNames.FIELD,
                JsonDocNames.DESCRIPTION,
                JsonDocNames.TYPE);
        final List<String> fields = new LinkedList<>(ALWAYS_COLUMNS);
        int currentRow = -1;
        final String name;
        final Map<String, String> data = new LinkedHashMap<>();

        private DocTable(final String name) { this.name = name;}

        void addRow() { this.currentRow++; }
        static String toKey(final int r, final String c) { return String.format("%d\t%s", r, c); }
        int level() { return 1 + (int) this.name.chars().filter(c-> c=='>').count(); }

        void addValue(final String colName, final String orgValue) {
            final var colVal = JsonProps.unquote(orgValue);
            if (this.currentRow < 0) this.currentRow++;
            if (!this.fields.contains(colName)) this.fields.add(colName);
            this.data.put(toKey(this.currentRow, colName), colVal);
        }
    }

    private static final List<String> EXCLUDE_PREFIXES = Arrays.asList(JsonDocNames.IGNORE_PREFIX);

    protected final Deque<DocTable> tables = new LinkedList<>();
    private final Deque<DocTable> stack = new LinkedList<>();

    @Override public boolean topNode(final JsonTopNode topNode) { return object(topNode); }
    @Override public void topNodeLeave(final JsonTopNode topNode) { objectLeave(topNode); }

    @Override
    public boolean object(final JsonObject object) {
        if (EXCLUDE_PREFIXES.stream().anyMatch(object.name::startsWith)) return false;

        final var top = this.stack.peek();
        final String pfx;
        if (top==null || top.name.equals("")) pfx = "";
        else pfx = top.name + SEPARATOR;
        final var tableName = pfx + object.name;
        final var docTable = new DocTable(tableName);
        if (top != null) {
            top.addRow();
            top.addValue(JsonDocNames.FIELD, object.name);
            if (object.hasChildren())
                top.addValue(JsonDocNames.DESCRIPTION,
                    "See " + tableName + SEPARATOR + object.name);
            if (object.isRequired()) {
                if (object instanceof JsonSchemaObject){
                    ((JsonSchemaObject) object).addProp(JsonDocNames.REQUIRED, "true");
                }
                else top.addValue(JsonDocNames.REQUIRED, "true");
            }
        }
        this.tables.add(docTable);
        this.stack.push(docTable);

        if (object instanceof JsonSchemaObject) {
            final var props = ((JsonSchemaObject) object).props;
            final var target = (top==null)? docTable : top;
            props.forEach(target::addValue);
        }

        return true;
    }

    @Override
    public void objectLeave(final JsonObject object) {
        if (!(object instanceof JsonTopNode)) {
            if (EXCLUDE_PREFIXES.stream().anyMatch(object.name::startsWith)) return;
        }
        this.stack.pop();
    }

    @Override
    public void keyValue(final JsonKeyValue node) {
        if (EXCLUDE_PREFIXES.stream().anyMatch(node.key::startsWith)) return;
        final var table = this.stack.peek();
        if (table == null) return;
        table.addRow();
        table.addValue(JsonDocNames.FIELD, node.key);
        table.addValue(JsonDocNames.DESCRIPTION, node.value);
    }

    @Override
    public void value(final JsonValue value) {
        final var table = this.stack.peek();
        if (table == null) return;
        table.addRow();
        table.addValue(JsonDocNames.FIELD, value.value);
    }

    @Override
    public boolean array(final JsonArray array) {
        if (EXCLUDE_PREFIXES.stream().anyMatch(array.name::startsWith)) return false;
        final var table = this.stack.peek();
        if (table == null) return false;
        table.addRow();
        table.addValue(JsonDocNames.FIELD, array.name);
        table.addValue(JsonDocNames.TYPE, JsonDocNames.ARRAY);
        final StringBuffer contents = new StringBuffer();
        contents.append("[ ");
        array.childList().stream()
                .filter(c -> c instanceof JsonNode)
                .forEach(child -> contents.append(((JsonNode)child).name).append(", "));
        contents.setLength(contents.length()-2);
        contents.append("]");
        table.addValue(JsonDocNames.DESCRIPTION, contents.toString());
        return false;
    }

    @Override
    public void arrayLeave(final JsonArray array) { /*EMPTY*/ }
}

class JsonDocWikiVisitor extends JsonDocPrintVisitor {

    JsonDocWikiVisitor() { super(); }
    @Override
    public String toString() {
        final StringBuffer buffer = new StringBuffer();
        this.tables.stream().filter(t -> t.currentRow >= 0).forEach(t -> {
            buffer.append("\n\nh").append(t.level()).append(". ").append(t.name).append("\n||");
            for (final var c : t.fields) buffer.append(c).append("||");
            buffer.append("\n");
            for (int i = 0; i <= t.currentRow; i++) {
                buffer.append("|");
                for (final var c : t.fields) {
                    final var content = t.data.get(DocTable.toKey(i, c));
                    buffer.append(content==null? " " : content).append("|");
                }
                buffer.append("\n");
            }
        });
        return buffer.toString();
    }
}

class JsonDocHtmlVisitor extends JsonDocPrintVisitor {

    private String q(final String s) { return StringEscapeUtils.escapeXml11(s); }

    @Override
    public String toString() {
        final StringBuffer buffer = new StringBuffer();
        buffer.append("<!doctype html>\n"+
                  "<html><head><meta charset=utf-8>\n"+
                  "<style>\n" +
                  "table, th, td {border: 1px solid black;}\n" +
                  "table {border-collapse: collapse;}\\n" +
                  "</style>\n" +
                  "</head>\n" +
                  "<body>");
        this.tables.stream().filter(t -> t.currentRow >= 0).forEach(t -> {
            buffer.append("\n\n<h").append(t.level()).append(">")
                    .append(q(t.name))
                    .append("</h2>\n<table><thead><tr>\n  ");
            for (final var c : t.fields) buffer.append("<th>").append(q(c)).append("</th>");
            buffer.append("\n</tr></thead><tbody>\n");
            for (int i = 0; i <= t.currentRow; i++) {
                buffer.append("  <tr>");
                for (final var c : t.fields) {
                    buffer.append("<td>")
                            .append(q(t.data.getOrDefault(DocTable.toKey(i, c), " ")))
                            .append("</td>");
                }
                buffer.append("</tr>\n");
            }
            buffer.append("</tbody></table>\n\n");
        });
        buffer.append("</body></html>");
        return buffer.toString();
    }
}