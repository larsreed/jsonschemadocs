package net.kalars.jsondoc;

import org.apache.commons.text.StringEscapeUtils;

import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
    protected final StringBuilder buffer = new StringBuilder();
    protected final Context context;

    AbstractPrintVisitor(final Context context) { this.context = context; }
    @Override public String toString() { return this.buffer.toString(); }

    String makeIndent(final JsonBasicNode node, final int extra) {
        return " ".repeat(extra + 2 * node.tokenDepth);
    }
}


@SuppressWarnings({"UnnecessaryReturnStatement", "EmptyMethod", "SameReturnValue"})
class DebugVisitor extends AbstractPrintVisitor {

    DebugVisitor(final Context context) { super(context); }

    private boolean printer(final JsonBasicNode node, final String s1, final String s2, final String s3) {
        this.buffer.append(makeIndent(node, 0))
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
            props.iterateOver((k, v) ->
                    this.buffer.append(makeIndent(node, 1))
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
            Arrays.asList(JsonDocNames.IGNORE_PREFIX, JsonDocNames.XDOC_PREFIX, JsonDocNames.XIF_PREFIX,
                    JsonDocNames.XIFNOT_PREFIX);

    JsonSchemaPrintVisitor(final Context context) { super(context); }

    @Override
    public void value(final JsonValue value) {
        if (EXCLUDE_PREFIXES.stream().anyMatch(value.value::startsWith)) return;
        this.buffer.append(makeIndent(value, 2))
                .append(value.value)
                .append(",\n");
    }

    @Override
    public void keyValue(final JsonKeyValue node) {
        if (EXCLUDE_PREFIXES.stream().anyMatch(node.key::startsWith)) return;
        this.buffer.append(makeIndent(node, 0))
                .append("\"")
                .append(node.key)
                .append("\": ")
                .append(node.value)
                .append(",\n");
    }

    @Override public boolean topNode(final JsonTopNode topNode) { throw new RuntimeException("Not expected"); }

    @Override public void topNodeLeave(final JsonTopNode topNode) { throw new RuntimeException("Not expected"); }

    @Override
    public boolean object(final JsonObject object) {
        if (EXCLUDE_PREFIXES.stream().anyMatch(object.name::startsWith)) return false;
        else if ("".equals(object.name)) {
            this.buffer.append("{\n");
            return true;
        }
        this.buffer.append(makeIndent(object, 0))
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
            object.props.propCopy().forEach((k, v) ->
                    this.buffer.append(indent)
                            .append(" ")
                            .append(k)
                            .append(" = ")
                            .append(v)
                            .append("\n"));
        }
        this.buffer.setLength((this.buffer.length()-2));
        if ( "".equals(object.name)) this.buffer.append("\n}\n");
        else this.buffer.append("\n").append(indent) .append("},\n");
    }

    @Override
    public boolean array(final JsonArray array) {
        if (EXCLUDE_PREFIXES.stream().anyMatch(array.name::startsWith)) return false;
        this.buffer.append(makeIndent(array, 0))
                .append("\"")
                .append(array.name)
                .append("\": [\n");
        return true;
    }

    @Override
    public void arrayLeave(final JsonArray array) {
        if (EXCLUDE_PREFIXES.stream().anyMatch(array.name::startsWith)) return;
        this.buffer.setLength((this.buffer.length()-2));
        this.buffer.append("\n")
                .append(makeIndent(array, 0))
                .append("],\n");
    }
}


/** Prints schema documentation. */
@SuppressWarnings("FeatureEnvy")
abstract class JsonDocPrintVisitor extends AbstractPrintVisitor {

    protected static final String SEPARATOR = " > "; // Separator in headings

    protected static final String SEE_PFX = "§§§/"; // Prefixes a reference injected in the Description column
    protected static final String SEE_SFX = "/§§§"; // Suffixes the same
    protected static final String SEE_QUICK_RE = SEE_PFX+"([^§]+)"+SEE_SFX; // Regex to find an extract such links
    protected static final Pattern SEE_REGEXP = Pattern.compile(".*"+SEE_QUICK_RE+".*", Pattern.DOTALL);

    protected static final Pattern USER_LINK_REGEXP = // links written by user
            Pattern.compile(JsonDocNames.USER_LINK_RE);

    private static final List<String> EXCLUDE_PREFIXES =
            Arrays.asList(JsonDocNames.IGNORE_PREFIX, JsonDocNames.XIFNOT_PREFIX, JsonDocNames.XIF_PREFIX);

    /** One documentation table instance. */
    final static class DocTable {
        private static final List<String> ALWAYS_COLUMNS = Arrays.asList(
                JsonDocNames.FIELD,
                JsonDocNames.DESCRIPTION,
                JsonDocNames.TYPE);
        final List<String> fields = new LinkedList<>();
        int currentRow = -1;
        final String name;
        private final Context context;
        boolean done = false;
        String cardinality;
        final Map<String, String> data = new LinkedHashMap<>();

        private DocTable(final String name, final Context context) {
            this.name = name;
            this.context = context;
            // Add non-excluded default columns
            ALWAYS_COLUMNS.forEach(c -> { if (!this.context.isExcluded(c)) this.fields.add(c); });
            // Skip this table if excluded
            final var searchFor = ("".equals(name))? "_" : nameToId(name);
            if (context.anyMatch(Context.SKIP_TABLES, searchFor)) this.done = true;
        }

        void addRow() { this.currentRow++; }
        static String toKey(final int r, final String c) { return String.format("%d\t%s", r, c); }
        int level() { return 1 + (int) this.name.chars().filter(c-> c=='>').count(); }
        String localName() { return this.name.replaceAll(".*"+SEPARATOR, ""); }

        void addValue(final String colName, final String orgValue) {
            if (this.context.isExcluded(colName)) return;
            final var colVal = JsonProps.unquote(orgValue);
            if (this.currentRow < 0) this.currentRow++;
            if (!this.fields.contains(colName)) this.fields.add(colName);
            this.data.merge(toKey(this.currentRow, colName), colVal,
                    (org, add) ->  org + " " + add);
        }

        void hasCardinality(final String cardinality) { this.cardinality = cardinality; }
    }

    protected final int embedUpToRows;
    protected String topTitle = "";
    protected final Deque<DocTable> tables = new LinkedList<>();
    private final Deque<DocTable> stack = new LinkedList<>();
    protected final Map<String, DocTable> tableMap = new HashMap<>();

    JsonDocPrintVisitor(final Context context) {
        super(context);
        this.embedUpToRows = Integer.parseInt(context.value(Context.EMBED_ROWS).orElse("0"));
    }

    @Override public void topNodeLeave(final JsonTopNode topNode) { objectLeave(topNode); }

    @Override public boolean topNode(final JsonTopNode topNode) {
        this.topTitle = topNode.props.getProp(JsonDocNames.TITLE, null);
        if (this.topTitle == null) {
            final var any =
                    topNode.childList().stream()
                            .filter(n-> n.name.equals(JsonDocNames.TITLE))
                            .map(n -> {
                                if (n instanceof JsonValue) return ((JsonValue) n).value;
                                return "";
                            })
                            .findAny();
            this.topTitle = any.orElse("").replaceAll("\"", "");
        }
        return object(topNode);
    }

    protected static String nameToId(final String name) { return name.replaceAll("[^a-zA-Z0-9]", "_"); }

    protected String keyToTitle(final String key) {
        final var no_ = key.replaceAll("_", " ");
        if (no_.length()>=1)  return no_.substring(0,1 ).toUpperCase() + no_.substring(1);
        return key;
    }

    @Override
    public boolean object(final JsonObject object) {
        final var exclude = object.props.propCopy().entrySet().stream()
                .filter(e -> e.getKey().startsWith(JsonDocNames.XIF_PREFIX) ||
                        e.getKey().startsWith(JsonDocNames.XIFNOT_PREFIX))
                .anyMatch(this::excludeThis);
        if (exclude) return false;
        if (EXCLUDE_PREFIXES.stream().anyMatch(object.name::startsWith)) return false;

        objectCleanup(object);

        final var current = this.stack.peek();
        final String pfx = (current==null || current.name.equals(""))? "" : current.name + SEPARATOR;

        final var tableName = pfx + object.name;
        final var docTable = new DocTable(tableName, this.context);
        if (current != null)  addToCurrent(object, current, tableName);
        docTable.hasCardinality(object.props.cardinality());
        this.tables.add(docTable);
        this.tableMap.put(tableName, docTable);
        this.stack.push(docTable);

        final var target = (current==null)? docTable : current;
        object.props.iterateOver(target::addValue);

        return true;
    }

    private void objectCleanup(final JsonObject object) {
        // 'enum' should be a property, not a child
        convertToProp(object, JsonDocNames.ENUM);
        // 'examples' should be a property, not a child
        convertToProp(object, JsonDocNames.EXAMPLES);
        object.props.addSampleValue(object.props.defaultSample());
    }

    private void convertToProp(final JsonObject object, final String propName) {
        object.childList().stream().filter(c -> propName.equals(c.name)).forEach(c -> {
            object.removeChild(c);
            if (c instanceof JsonArray a) {
                a.childList().forEach(child -> {
                    object.props.add(propName, child.toString());
                    object.props.addSampleValue(child.toString());
                });
                a.props.iterateOver((k, v) ->  { if (! "".equals(v)) object.props.add(propName, k + "=" + v); });
            }
            else object.props.add(propName, c.toString());
        });
    }

    private void addToCurrent(final JsonObject object, final DocTable current, final String tableName) {
        current.addRow();
        current.addValue(JsonDocNames.FIELD, object.name);
        if (typeAsAnArray(object)) {
            object.addProp(JsonDocNames.TYPE, object.childList().get(0).toString());
        }
        else if (object.hasChildren()) current.addValue(JsonDocNames.DESCRIPTION, SEE_PFX + tableName + SEE_SFX);
        if (object.isRequired()) {
            if (object instanceof JsonSchemaObject) object.addProp(JsonDocNames.REQUIRED, "true");
            else current.addValue(JsonDocNames.REQUIRED, "true");
        }
    }

    private boolean excludeThis(final Map.Entry<String, String> e) {
        final var values = e.getValue().split(", *");
        if (e.getKey().startsWith(JsonDocNames.XIF_PREFIX)) {
            final var guard = e.getKey().substring(JsonDocNames.XIF_PREFIX.length()); // e.g. target
            return !this.context.matches(guard, true, values); // excluded
        }
        else if (e.getKey().startsWith(JsonDocNames.XIFNOT_PREFIX)) {
            final var guard = e.getKey().substring(JsonDocNames.XIFNOT_PREFIX.length());
            return this.context.matches(guard, false, values);
        }
        return false; // i.e. excluded
    }

    // hack... to handle "type" : [ "string", "null" ] type constructs
    private boolean typeAsAnArray(final JsonObject object) {
        return object instanceof JsonSchemaObject
                && object.childList().size() == 1
                && object.childList().get(0) instanceof JsonArray
                && JsonDocNames.TYPE.equals(object.childList().get(0).name);
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
        if (JsonDocNames.TYPE.equals(array.name)) return false;

        final var table = this.stack.peek();
        if (table == null) return false;

        table.addRow();
        table.addValue(JsonDocNames.FIELD, array.name);
        table.addValue(JsonDocNames.TYPE, JsonDocNames.ARRAY);

        final var contents = new StringBuilder();
        contents.append("[ ");
        array.childList().forEach(child -> contents.append(child.toString()).append(", "));
        if (!array.childList().isEmpty()) contents.setLength(contents.length()-2);
        contents.append(array.props);
        contents.append("]");
        table.addValue(JsonDocNames.DESCRIPTION, contents.toString());
        return false;  // do not process further
    }

    @Override public void arrayLeave(final JsonArray array) { /*EMPTY*/ }
}


@SuppressWarnings("SameParameterValue")
class JsonDocHtmlVisitor extends JsonDocPrintVisitor {

    private static final String STYLE = """
        <style>
          h1, h2, h3, h4, h5, h6 {font-family : "Roboto Black", serif; }
          table, th, td {border: 1px solid #ddd; padding: 8px; }
          table {border-collapse: collapse; font-family: Roboto, Calibri, Helvetica, Arial, sans-serif; }
          tr:nth-child(even){ background-color: #f2f2f2; }
          tr:hover { background-color: #ddd; }
          th { padding-top: 12px; padding-bottom: 12px; text-align: left; background-color: #12404F; color: white; }
        </style>
        </head>
        <body>
        """;

    JsonDocHtmlVisitor(final Context context) { super(context); }

    protected String q(final String s) {
        return StringEscapeUtils.escapeXml11(s)
                .replaceAll("\t", "&nbsp;&nbsp;")
                .replaceAll("\n", "<br/>");
    }

    @Override
    public String toString() {
        this.buffer.append("""
                <!doctype html>
                <html><head><meta charset=utf-8>""")
                .append(STYLE);
        this.tables.stream()
                .filter(t -> t.currentRow >= 0)
                .forEach(t -> this.buffer.append(formatTable(t, 0)));
        return this.buffer.append("</body></html>").toString();
    }

    protected String formatTable(final DocTable t, final int level) {
        if (t.done) return ""; // already processed recursively
        t.done = true;
        t.fields.remove(JsonProps.DEFAULT_SAMPLE); // TODO Shouldn't be necessary

        final var sb = new StringBuilder();
        if (level==0) sb.append(headingWithId(t));// Only heading for tables that are not embedded
        sb.append("<table><thead><tr>\n  ")
              .append(headerRow(t))
              .append("\n</tr></thead><tbody>\n");

        for (int i = 0; i <= t.currentRow; i++) {
            sb.append("  <tr>");
            for (final var c : t.fields) {
                final var cell = t.data.getOrDefault(DocTable.toKey(i, c), " ");
                sb.append("<td>").append(createCell(level, cell)).append("</td>");
            }
            sb.append("</tr>\n");
        }

        return sb.append("</tbody></table>\n\n").toString();
    }

    private String createCell(final int level, final String content) {
        final var matchTableLink = SEE_REGEXP.matcher(content);
        if (matchTableLink.matches()) return createTableLink(level, content, matchTableLink.group(1));
        return replaceNextLink(content);
    }

    private String replaceNextLink(final String content) {
        final var matchUserLink = USER_LINK_REGEXP.matcher(content);
        if (matchUserLink.find()) {
            final var url = matchUserLink.group(1);
            final var linkText= matchUserLink.group(4);
            return q(content.substring(0, matchUserLink.start()))
                 + createUrlLink(url, linkText)
                 + replaceNextLink(content.substring(matchUserLink.end()));
        }
        return q(content);
    }

    private String createTableLink(final int level, final String content, final String key) {
        final var tbl = this.tableMap.get(key);
        final var cell = createCell(level, content.replaceAll(SEE_QUICK_RE, " ").trim() + " ");
        // if we have a reference to a single line table, optionally embed it
        if (tbl!=null && (embeddableRows(tbl) || justSingleItems(tbl)))  return cell + formatTable(tbl, level +1);
        return cell + createInternalLink(key);
    }

    private boolean embeddableRows(final DocTable tbl) { return tbl.currentRow < this.embedUpToRows; }

    private boolean justSingleItems(final DocTable tbl) {
        return tbl.currentRow == 0
                && JsonDocNames.ITEMS.equals(tbl.data.getOrDefault(DocTable.toKey(0, JsonDocNames.FIELD), ""));
    }

    protected String createInternalLink(final String key) {
        return "<a href=\"#" + nameToId(key) + "\">" + key + "&gt;</a>";
    }

    protected String createUrlLink(final String url, final String optText) {
        return "<a href=\"" + url + "\">" + (optText==null? url : optText) + "</a>";
    }

    protected String headingWithId(final DocTable t) {
        final var name = "".equals(t.name)? this.topTitle : t.name;
        return "\n\n<h" +
                t.level() +
                " id=\"" +
                nameToId(name) +
                "\">" +
                q(name) +
                "</h" +
                t.level() +
                ">\n";
    }

    private String headerRow(final DocTable t) {
        final var sb = new StringBuilder();
        for (final var c : t.fields) sb.append("<th>")
                    .append(q(keyToTitle(c)))
                    .append("</th>");
        return sb.toString();
    }
}

class JsonDocWikiHtmlVisitor extends JsonDocHtmlVisitor {

    JsonDocWikiHtmlVisitor(final Context context) { super(context); }

    @Override
    public String toString() {
        this.tables.stream()
                .filter(t -> t.currentRow >= 0)
                .forEach(t -> this.buffer.append(formatTable(t, 0)));
        return this.buffer.toString();
    }

    @Override
    protected String createInternalLink(final String key) {
        return "<ac:link ac:anchor=\"" + nameToId(key) + "\">" +
               "<ac:plain-text-link-body><![CDATA[" + key + "]]></ac:plain-text-link-body>" +
               "</ac:link>";
    }

    @Override
    protected String headingWithId(final DocTable t) {
        final var name = "".equals(t.name)? this.topTitle : t.name;
        return "\n\n<p><ac:structured-macro ac:name=\"anchor\" ac:schema-version=\"1\">" +
               "<ac:parameter ac:name=\"\">" + nameToId(name) + "</ac:parameter>" +
               "  </ac:structured-macro></p>\n" +
               "<h" + t.level() + ">" +
               q(name) +
               "</h" + t.level() + ">\n";
    }
}

class JsonDocDotVisitor extends JsonDocPrintVisitor {

    JsonDocDotVisitor(final Context context) { super(context); }

    private String q(final String s) { return "\"" + s + "\""; }
    private String only(final String s) { return s.replaceAll(".*> ", ""); }

    @Override
    public String toString() {
        this.buffer.append("""
digraph G {
        fontname = "Calibri"
        fontsize = 10

        node [
                fontname = "Calibri"
                fontsize = 10
                shape = "record"
        ]

        edge [
                fontname = "Calibri"
                fontsize = 9
        ]

                """);
        this.tables.stream()
                .filter(t -> t.currentRow >= 0)
                .forEach(t -> this.buffer.append(formatTable(t)));
        return this.buffer.append("}").toString();
    }

    private String formatTable(final DocTable t) {
        if (t.done) return ""; // already processed recursively
        t.done = true;

        final var sb = new StringBuilder();
        final var name = "".equals(t.name)? this.topTitle : t.name;
        createNode(sb, name);

        for (int i = 0; i <= t.currentRow; i++) {
            for (final var c : t.fields) {
                final var cell = t.data.getOrDefault(DocTable.toKey(i, c), " ");
                createEdge(sb, name, cell);
            }
        }

        return sb.toString();
    }

    private void createNode(final StringBuilder buffer, final String name) {
        buffer.append("        ").append(q(name)).append(" [\n")
              .append("                ").append("label = \"{").append(only(name)).append("\\n|}\"\n")
              .append("        ").append("]\n\n");
    }

    private void createEdge(final StringBuilder buffer, final String name, final String cell) {
        // TODO drop edges to skipped nodes (skipTable & $defs)
        final var match = SEE_REGEXP.matcher(cell);
        if (!match.matches())  return;
        final var key = match.group(1);
        final var tbl = this.tableMap.get(key);
        if (tbl == null)  return;
        buffer.append(formatTable(tbl))
              .append("\n        ")
              .append(q(name)).append(" -> ").append(q(tbl.name))
              .append(" [ label = \"").append(tbl.cardinality).append("\" ]")
              .append("\n\n");
    }
}


@SuppressWarnings("SameParameterValue")
class JsonDocMarkdownVisitor extends JsonDocPrintVisitor {

    JsonDocMarkdownVisitor(final Context context) { super(context); }

    protected String q(final String s) {
        return s.replaceAll("[-`*|_{}()#+]", "\\\\$0")
                .replaceAll("[]\\[]", "\\\\$0")
                .replaceAll("\t", "&nbsp;&nbsp;")
                .replaceAll("\n", "<br/>");
    }

    @Override
    public String toString() {
        this.tables.stream()
                .filter(t -> t.currentRow >= 0)
                .forEach(t -> this.buffer.append(formatTable(t, 0)));
        return this.buffer.toString();
    }

    protected String formatTable(final DocTable t, final int level) {
        if (t.done) return ""; // already processed recursively
        t.done = true;
        t.fields.remove(JsonProps.DEFAULT_SAMPLE); // TODO Shouldn't be necessary

        final var sb = new StringBuilder();
        if (level==0) sb.append(headingWithId(t));// Only heading for tables that are not embedded
        sb.append(headerRow(t));

        for (int i = 0; i <= t.currentRow; i++) {
            sb.append("|");
            for (final var c : t.fields) {
                final var cell = t.data.getOrDefault(DocTable.toKey(i, c), " ");
                sb.append(" ").append(createCell(level, cell)).append(" |");
            }
            sb.append("\n");
        }

        return sb.append("\n").toString();
    }

    private String createCell(final int level, final String content) {
        final var matchTableLink = SEE_REGEXP.matcher(content);
        if (matchTableLink.matches()) return createTableLink(level, content, matchTableLink.group(1));
        return replaceNextLink(content);
    }

    private String replaceNextLink(final String content) {
        final var matchUserLink = USER_LINK_REGEXP.matcher(content);
        if (matchUserLink.find()) {
            final var url = matchUserLink.group(1);
            final var linkText= matchUserLink.group(4);
            return q(content.substring(0, matchUserLink.start()))
                    + createUrlLink(url, linkText)
                    + replaceNextLink(content.substring(matchUserLink.end()));
        }
        return q(content);
    }

    private String createTableLink(final int level, final String content, final String key) {
        // final var tbl = this.tableMap.get(key);
        final var cell = createCell(level, content.replaceAll(SEE_QUICK_RE, " ").trim() + " ");
        // if we have a reference to a single line table, optionally embed it
        // TODO Consider support for embedding in markup
        //     if (tbl!=null && tbl.currentRow < this.embedUpToRows)  return cell + formatTable(tbl, level +1);
        return cell + createInternalLink(key);
    }

    protected String createInternalLink(final String key) { return "[" + key + ">](#" + nameToId(key) + ")"; }

    protected String createUrlLink(final String url, final String optText) {
        if (optText==null || "".equals(optText)) return "[" + url + "]";
        return "[" + optText + "](" + url + ")";
    }

    protected String headingWithId(final DocTable t) {
        final var name = "".equals(t.name)? this.topTitle : t.name;
        return "\n\n<a name=\"" +
                nameToId(name) +
                "\"></a>\n" +
                "#".repeat(t.level()) +
                " " +
                q(name) +
                "\n";
    }

    private String headerRow(final DocTable t) {
        final var sb = new StringBuilder();
        sb.append("| ");
        for (final var c : t.fields) sb.append(q(keyToTitle(c))).append(" |");
        sb.append("\n| ");
        for (final var ignored : t.fields) sb.append(" ----- |");
        return sb.append("\n").toString();
    }
}


/** Tries to create sample output by means of a given (set of) column name(s). Sample column names must be
 * defined by setting sampleColumns=... */
class JsonSamplePrintVisitor extends JsonDocPrintVisitor {

    final List<String> sampleCols = new LinkedList<>();

    JsonSamplePrintVisitor(final Context context) {
        super(context);
        final var samples = context.value(Context.SAMPLE_COLUMNS);
        if (samples.isEmpty() || "".equals(samples.get()))
            throw new IllegalArgumentException("No " + Context.SAMPLE_COLUMNS + " defined");
        samples.ifPresent(sample -> Arrays.stream(sample.split(", *")).forEach(s-> {
            this.sampleCols.add(s.toUpperCase());
            this.sampleCols.add(JsonProps.removePrefix(s, Context.SAMPLE_COLUMNS).toUpperCase());
        }));
        this.sampleCols.add(JsonProps.DEFAULT_SAMPLE.toUpperCase());
    }

    private String makeIndent(final int n) { return " ".repeat(n * 2); }

    @Override
    public String toString() {
        this.tables.stream()
                .filter(t -> t.currentRow >= 0)
                .forEach(t -> formatTable(t, 0));
        return this.buffer.toString()
                .replaceAll(",(\n *})", "$1")
                .replaceAll(",[ \n]*$", "");
    }

    @SuppressWarnings("FeatureEnvy")
    private void formatTable(final DocTable t, final int level) {
        if (t.done) return;
        t.done = true;
        if (!t.fields.contains(JsonProps.DEFAULT_SAMPLE))
            t.fields.add(JsonProps.DEFAULT_SAMPLE); // Need to include generated sample here

        final var name = t.localName();
        if (name.length()>0)
            this.buffer.append(makeIndent(level))
                    .append("\"")
                    .append(name)
                    .append("\": ");
        this.buffer.append("{\n");

        for (int i = 0; i <= t.currentRow; i++) {
            final var cellName = t.data.get(DocTable.toKey(i, JsonDocNames.FIELD));
            boolean seen = false;
            for (final var key : t.fields) {
                final var cell = t.data.get(DocTable.toKey(i, key));
                if (cell != null && this.sampleCols.contains(key.toUpperCase()) && !seen) {
                    seen = true;
                    this.buffer.append(makeIndent(level + 1))
                            .append("\"")
                            .append(cellName)
                            .append("\": ")
                            .append(cell.contains("\"") ? "" : "\"")
                            .append(cell)
                            .append(cell.contains("\"") ? "" : "\"")
                            .append(",\n");
                }
                else if (cell != null) {
                    final var matchTableLink = SEE_REGEXP.matcher(cell);
                    if (matchTableLink.matches()) {
                        final var tbl = this.tableMap.get(matchTableLink.group(1));
                        formatTable(tbl, level + 1);
                    }
                }
            }
        }

        this.buffer.append(makeIndent(level)).append("},\n");
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