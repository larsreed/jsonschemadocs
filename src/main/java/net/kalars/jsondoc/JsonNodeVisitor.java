package net.kalars.jsondoc;

import org.apache.commons.text.StringEscapeUtils;

import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

// This file defines visitors to traverse the generated data structures to generate output

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
            final var props = ((JsonSchemaObject) object).props.copyProps();
            props.forEach((k, v) ->
                    this.sb.append(indent)
                            .append(" ")
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

    protected static final String SEPARATOR = " > ";
    protected static final String SEE_TOKEN = "§§§";
    private static final List<String> EXCLUDE_PREFIXES = Arrays.asList(JsonDocNames.IGNORE_PREFIX);

    /** One documentation table instance. */
    final static class DocTable {
        private static final List<String> ALWAYS_COLUMNS = Arrays.asList(
                JsonDocNames.FIELD,
                JsonDocNames.DESCRIPTION,
                JsonDocNames.TYPE);
        final List<String> fields = new LinkedList<>(ALWAYS_COLUMNS);
        int currentRow = -1;
        final String name;
        boolean done = false;
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

    protected final int embedUpToRows;
    protected final Deque<DocTable> tables = new LinkedList<>();
    private final Deque<DocTable> stack = new LinkedList<>();
    protected Map<String, DocTable> tableMap = new HashMap<>();

    JsonDocPrintVisitor(final int embedUpToRows) { this.embedUpToRows = embedUpToRows; }
    @Override public boolean topNode(final JsonTopNode topNode) { return object(topNode); }
    @Override public void topNodeLeave(final JsonTopNode topNode) { objectLeave(topNode); }

    protected String nameToId(final String name) { return name.replaceAll("[^a-zA-Z0-9]", "_"); }

    protected String keyToTitle(final String key) {
        final var no_ = key.replaceAll("_", " ");
        if (no_.length()>=1)  return no_.substring(0,1 ).toUpperCase() + no_.substring(1);
        return key;
    }


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
                top.addValue(JsonDocNames.DESCRIPTION, SEE_TOKEN + tableName);
            if (object.isRequired()) {
                if (object instanceof JsonSchemaObject){
                    ((JsonSchemaObject) object).addProp(JsonDocNames.REQUIRED, "true");
                }
                else top.addValue(JsonDocNames.REQUIRED, "true");
            }
        }
        this.tables.add(docTable);
        this.tableMap.put(tableName, docTable);
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
        final var contents = new StringBuilder();
        contents.append("[ ");
        array.childList().stream()
                .filter(c -> c instanceof JsonNode)
                .forEach(child -> contents.append(((JsonNode)child).name).append(", "));
        contents.setLength(contents.length()-2);
        contents.append("]");
        table.addValue(JsonDocNames.DESCRIPTION, contents.toString());
        return false;
    }

    @Override public void arrayLeave(final JsonArray array) { /*EMPTY*/ }
}

class JsonDocWikiVisitor extends JsonDocPrintVisitor {

    JsonDocWikiVisitor() { super(0); }

    @Override
    public String toString() {
        final var buffer = new StringBuilder();
        this.tables.stream().filter(t -> t.currentRow >= 0).forEach(t -> formatTable(buffer, t));
        return buffer.toString();
    }

    private void formatTable(final StringBuilder buffer, final DocTable t) {
        buffer.append(heading(t));
        buffer.append(headingRow(t));
        for (int i = 0; i <= t.currentRow; i++) {
            buffer.append("|");
            for (final var c : t.fields) {
                var cell = t.data.getOrDefault(DocTable.toKey(i, c), " ");
                if (cell.matches(SEE_TOKEN + ".*")) {
                    final var key = cell.substring(SEE_TOKEN.length());
                    cell = "[" + key + "|#" + nameToId(key) + "]";
                }
                else cell = quote(cell);
                buffer.append(cell).append("|");
            }
            buffer.append("\n");
        }
    }

    private String quote(final String s) {
        return s.replaceAll("([]\\[{}!\\\\])", "\\\\$1");
    }

    private String headingRow(final DocTable t) {
        final var buffer = new StringBuilder();
        for (final var c : t.fields) buffer.append(keyToTitle(c)).append("||");
        buffer.append("\n");
        return buffer.toString();
    }

    private String heading(final DocTable t) {
        return "\n\n" +
                "{anchor:" +
                nameToId(t.name) +
                "}\n" +
                "h" +
                t.level() + ". " +
                t.name +
                "\n||";
    }
}

class JsonDocHtmlVisitor extends JsonDocPrintVisitor {

    private static final String STYLE =
            "<style>\n" +
            "  h1, h2, h3, h4, h5, h6 {font-family : \"Roboto Black\", serif; }\n" +
            "  table, th, td {border: 1px solid #ddd; padding: 8px; }\n" +
            "  table {border-collapse: collapse;" +
            " font-family: Roboto, Calibri, Helvetica, Arial, sans-serif; }\n" +
            "  tr:nth-child(even){ background-color: #f2f2f2; }\n" +
            "  tr:hover { background-color: #ddd; }\n" +
            "  th { padding-top: 12px;" +
            " padding-bottom: 12px;" +
            " text-align: left;" +
            " background-color: #12404F;" +
            " color: white; }\n" +
            "</style>\n" +
            "</head>\n" +
            "<body>";

    JsonDocHtmlVisitor(final int embedUpToRows) { super(embedUpToRows); }

    private String q(final String s) { return StringEscapeUtils.escapeXml11(s); }

    @Override
    public String toString() {
        final var buffer = new StringBuilder();
        buffer.append("<!doctype html>\n" + "<html><head><meta charset=utf-8>\n");
        buffer.append(STYLE);
        this.tables.stream()
                .filter(t -> t.currentRow >= 0)
                .forEach(t -> buffer.append(formatTable(t, 0)));
        buffer.append("</body></html>");
        return buffer.toString();
    }

    private String formatTable(final DocTable t, final int level) {
        if (t.done) return ""; // already processed recursively
        t.done = true;

        final var buffer = new StringBuilder();
        if (level==0) buffer.append(headingWithId(t));// Only heading for tables that are not embedded
        buffer.append("<table><thead><tr>\n  ");
        buffer.append(headerRow(t));
        buffer.append("\n</tr></thead><tbody>\n");

        for (int i = 0; i <= t.currentRow; i++) {
            buffer.append("  <tr>");
            for (final var c : t.fields) {
                var cell = t.data.getOrDefault(DocTable.toKey(i, c), " ");
                if (cell.matches(SEE_TOKEN + ".*")) {
                    final var key = cell.substring(SEE_TOKEN.length());
                    final var tbl = this.tableMap.get(key);
                    // if we have a reference to a single line table, optionally embed it
                    if (tbl!=null && tbl.currentRow < this.embedUpToRows)  cell = formatTable(tbl, level+1);
                    else cell = "<a href=\"#" + nameToId(key) + "\">" + key + "&gt;</a";
                }
                else cell = q(cell);
                buffer.append("<td>").append(cell).append("</td>");
            }
            buffer.append("</tr>\n");
        }

        buffer.append("</tbody></table>\n\n");
        return buffer.toString();
    }

    private String headingWithId(final DocTable t) {
        return "\n\n<h" +
                t.level() +
                " id=\"" +
                nameToId(t.name) +
                "\">" +
                q(t.name) +
                "</h" +
                t.level() +
                ">\n";
    }

    private String headerRow(final DocTable t) {
        final var buffer = new StringBuilder();
        for (final var c : t.fields)
            buffer.append("<th>")
                    .append(q(keyToTitle(c)))
                    .append("</th>");
        return buffer.toString();
    }
}

//   Copyright 2021, Lars Reed -- lars-at-kalars.net
//
//           Licensed under the Apache License, Version 2.0 (the "License");
//           you may not use this file except in compliance with the License.
//           You may obtain a copy of the License at
//
//           http://www.apache.org/licenses/LICENSE-2.0
//
//           Unless required by applicable law or agreed to in writing, software
//           distributed under the License is distributed on an "AS IS" BASIS,
//           WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//           See the License for the specific language governing permissions and
//           limitations under the License.