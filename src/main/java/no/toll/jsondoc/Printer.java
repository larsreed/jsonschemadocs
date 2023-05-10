package no.toll.jsondoc;

import org.apache.commons.text.StringEscapeUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;


/** General functionality for the ...Printer classes. */
abstract class Printer {
    protected final StringBuilder buffer = new StringBuilder();
    protected final Node rootNode;
    protected static final Pattern USER_LINK_REGEXP = // links written by user
            Pattern.compile(JsonDocNames.USER_LINK_RE);

    Printer(final Node rootNode) { this.rootNode = rootNode; }

    protected static String keyToTitle(final String key) {
        final var no_ = Node.removePrefix(key, JsonDocNames.XDOC_PREFIX).replaceAll("_", " ");
        if (no_.length()>=1)  return no_.substring(0, 1).toUpperCase() + no_.substring(1);
        return key;
    }

    protected boolean isLocalRef(final Node node) {
        if (!JsonDocNames.REF.equals(node.name)) return false;
        return node.values.toString().startsWith(JsonDocNames.REF_LOCAL_PFX);
    }

    protected final String replaceNextLink(final String content) {
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

    protected final void doneIfNotTable(final Node node) { if (!node.isTable())  node.done(); }
    protected String createUrlLink(final String url, final String linkText) { return url; }
    protected String q(final String s) { return s; }

    protected void handleException(final Node node, final Throwable t) {
        if (t instanceof final HandledException he)  throw he;
        System.err.println(node.qName() + ": " + t.toString());
        throw new HandledException(t);
    }
}

/** Print out structure (for debugging). */
class DebugPrinter extends Printer {

    DebugPrinter(final Node rootNode) { super(rootNode); }

    private String makeIndent(final int level) { return "  ".repeat(level); }

    public String create() {
        handleNode(rootNode, 0);
        return buffer.toString();
    }

    private void handleNode(final Node node, final int level) {
        buffer.append(makeIndent(level))
                   .append(node.toString());
        buffer.append("\n");
        for (final var v : node.values.all())
            buffer.append(makeIndent(level +1))
                        .append("Val ")
                        .append(v)
                        .append("\n");
        for (final var c : node.children) handleNode(c, level +2);
    }
}

/** Output documentation in a standalone HTML file. */
@SuppressWarnings("UnnecessaryToStringCall")
class HtmlPrinter extends Printer {
    private static final String STYLE = """
        <style>
          h1, h2, h3, h4, h5, h6 {font-family : "Roboto Black", serif; }
          table, th, td {border: 1px solid #ddd; padding: 8px; }
          table {border-collapse: collapse; font-family: Roboto, Calibri, Helvetica, Arial, sans-serif; }
          tr:nth-child(even){ background-color: #f2f2f2; }
          tr:hover { background-color: #ddd; }
          th { padding-top: 12px; padding-bottom: 12px; text-align: left; background-color: #12404F; color: white; }
        </style>
        """;
    private final Context context;


    HtmlPrinter(final Node rootNode, final Context context) {
        super(rootNode);
        this.context = context;
    }

    @Override
    protected String q(final String s) {
        return StringEscapeUtils.escapeXml11(s)
                .replaceAll("\t", "&nbsp;&nbsp;")
                .replaceAll("\n", "<br/>");
    }

    public String create() {
        head();
        handleTableNode(rootNode, 0);
        tail();
        return buffer.toString();
    }

    protected void head() {
        buffer.append("<!doctype html>\n")
              .append("<html lang='")
              .append(context.value(Context.LANG).orElse(""))
              .append("'>\n")
              .append("  <head title='")
              .append(rootNode.displayName())
              .append("'>\n")
              .append("  <meta charset=utf-8>\n")
              .append(STYLE)
              .append("""
                      </head>
                      <body>
                      """);
    }

    protected void tail() {
        buffer.append("""
                    </body>
                  </html>
                  """);
    }

    private void handleTableNode(final Node node, final int level) {
        try {
            if (!node.isVisible()) return;
            if (node.rows().size() > 0) {
                if (level == 0) buffer.append(headingWithId(node)); // not embedded
                buffer.append(tableHead(node));
                for (final var row : node.rows()) handleRowNode(row, level);
                buffer.append(tableEnd());
            }
            for (final var sub : node.subTables())
                if (level == 0) handleTableNode(sub, 0);
                else node.parent().add(sub);
        }
        catch (final Throwable t) { handleException(node, t); }
    }

    protected String headingWithId(final Node node) {
        return "\n\n<h" +
                node.level() +
                " id=\"" +
                node.extId() +
                "\">" +
                q(node.qName()) +
                "</h" +
                node.level() +
                ">\n";
    }

    private String tableHead(final Node node) {
        return "<table><thead>\n<tr>" +
               headerRow(node) +
               "</tr>\n</thead>\n<tbody>\n" ;
    }

    private String headerRow(final Node node) {
        final var sb = new StringBuilder();
        for (final var c : node.columns())
            sb.append("<th>")
              .append(q(keyToTitle(c)))
              .append("</th>");
        return sb.toString();
    }

    @SuppressWarnings("SameReturnValue")
    private String tableEnd() { return "</tbody></table>\n"; }

    private void handleRowNode(final Node rowNode, final int level) {
        if (!rowNode.isVisible()) return; // Hidden or already processed
        doneIfNotTable(rowNode);
        final var isDefs = JsonDocNames.DEFS.equals(rowNode.name);
        buffer.append("<tr>") .append("<td>")
              .append(isDefs? "(" : "")
              .append(q(rowNode.name))
              .append(isDefs? ")" : "")
              .append("</td>");
        for (final var col : rowNode.parent().columns()) {
            if (JsonDocNames.FIELD.equals(col)) continue; // handled above
            buffer.append("<td>");
            final var cellNode = rowNode.getChild(col);
            if (cellNode.isPresent()) {
                createCell(cellNode.get());
                doneIfNotTable(cellNode.get());
            }
            if (JsonDocNames.DESCRIPTION.equals(col)) {
                if (isLocalRef(rowNode)) {
                    // If this is a local $ref, link to it
                    lineBreakIfNeeded();
                    buffer.append(createRefLink(rowNode));
                }
                else if (rowNode.values.isNonEmpty()) {
                    // If there are values directly on the row node, add it to description
                    lineBreakIfNeeded();
                    createCell(rowNode);
                }
                if (rowNode.isTable()) {
                    // Possible embedding
                    lineBreakIfNeeded();
                    if (shouldEmbed(rowNode, level)) {
                        handleTableNode(rowNode, level + 1);
                        rowNode.done();
                    }
                    else if (rowNode.rows().size() > 0) {
                        buffer.append(createInternalLink(rowNode));
                    }
                }
            }
            buffer.append("</td>");
        }
        buffer.append("</tr>\n");
    }

    private void lineBreakIfNeeded() {
        if (!buffer.toString().endsWith("<td>") && !buffer.toString().endsWith("<br/>"))
            buffer.append("<br/>");
    }

    private boolean shouldEmbed(final Node node, final int level) {return level==0 && node.isEmbeddable(); }

    private void createCell(final Node node) {
        final var cellVal = NodeValues.listToString(node.values.all(), "", "\n", "");
        buffer.append(replaceNextLink(cellVal));
        if (node.nodeType.equals(NodeType.Array)) {
            lineBreakIfNeeded();
            final var elements = node.children.stream()
                    .map(n -> n.values.all())
                    .map(l-> NodeValues.listToString(l, "", " ", ""))
                    .toList();
            buffer.append(replaceNextLink(NodeValues.listToString(elements, "[", ", ", "]")));
        }
    }

    @Override
    protected String createUrlLink(final String url, final String optText) {
        return "<a href=\"" + url + "\">" + (optText==null? url : optText) + "</a>";
    }

    protected String createInternalLink(final Node node) {
        return "<a href=\"#" + node.extId() + "\">" + node.name + "&gt;</a>";
    }

    protected String createRefLink(final Node node) {
        if (!isLocalRef(node)) return node.values.toString();
        final var ref = node.values.toString().substring(JsonDocNames.REF_LOCAL_PFX.length());
        return "<a href=\"#$defs__" + ref + "\">[" + node.values.toString() + "]</a>";
    }
}

/** Output XHTML documentation adapted for Confluence wiki storage format. */
@SuppressWarnings("UnnecessaryToStringCall")
class WikiPrinter extends HtmlPrinter {
    WikiPrinter(final Node rootNode, final Context context) { super(rootNode, context); }

    @Override protected void head() {}
    @Override protected void tail() {}

    @Override
    protected String headingWithId(final Node node) {
        return "\n\n<p><ac:structured-macro ac:name=\"anchor\" ac:schema-version=\"1\">" +
                "<ac:parameter ac:name=\"\">" + node.extId() + "</ac:parameter>" +
                "</ac:structured-macro></p>\n" +
                "<h" + node.level() + ">" +
                q(node.qName()) +
                "</h" + node.level() + ">\n";
    }

    @Override
    protected String createInternalLink(final Node node) {
        return "<ac:link ac:anchor=\"" + node.extId() + "\">" +
                "<ac:plain-text-link-body><![CDATA[" + node.name + ">]]></ac:plain-text-link-body>" +
                "</ac:link>";
    }

    @Override
    protected String createRefLink(final Node node) {
        if (!isLocalRef(node)) return node.values.toString();
        final var ref = node.values.toString().substring(JsonDocNames.REF_LOCAL_PFX.length());
        return "<ac:link ac:anchor=\"$defs__" + ref + "\">" +
                "<ac:plain-text-link-body><![CDATA[[" + node.values.toString() + "]>]]></ac:plain-text-link-body>" +
                "</ac:link>";
    }
}

/** Create documentation in Markdown format. */
@SuppressWarnings("UnnecessaryToStringCall")
class MarkdownPrinter extends Printer {

    private static final String BR = "<br />";

    MarkdownPrinter(final Node rootNode) { super(rootNode); }

    @Override
    protected String q(final String s) {
        return s.replaceAll("[-`*|_{}()#+]", "\\\\$0")
                .replaceAll("[]\\[]", "\\\\$0")
                .replaceAll("\t", "&nbsp;&nbsp;")
                .replaceAll("\n", BR);
    }

    public String create() {
        handleTableNode(rootNode, 0);
        return buffer.toString();
    }

    private void handleTableNode(final Node node, final int level) {
        try {
            if (!node.isVisible()) return;
            if (node.rows().size() > 0) {
                if (level == 0) buffer.append(headingWithId(node)); // not embedded
                buffer.append(tableHead(node));
                for (final var row : node.rows()) handleRowNode(row, level);
            }
            if (level > 0) return; // No embedding in embedding...
            for (final var sub : node.subTables()) handleTableNode(sub, 0);
        }
        catch (final Throwable t) { handleException(node, t); }
    }

    private String headingWithId(final Node node) {
        return "\n\n<a name=\"" +
                node.extId() +
                "\"></a>\n" +
                "#".repeat(node.level()) +
                ' ' +
                q(node.qName()) +
                '\n';
    }

    private String tableHead(final Node node) {
        final var sb = new StringBuilder();
        sb.append("| ");
        for (final var c : node.columns()) sb.append(q(keyToTitle(c))).append(" |");
        sb.append("\n| ");
        for (final var ignored : node.columns()) sb.append(" ----- |");
        return sb.append("\n").toString();
    }

    private void handleRowNode(final Node rowNode, final int level) {
        if (!rowNode.isVisible()) return; // Hidden or already processed
        doneIfNotTable(rowNode);
        final var isDefs = JsonDocNames.DEFS.equals(rowNode.name);
        buffer.append("| ")
                .append(isDefs? "(" : "")
                .append(q(rowNode.name))
                .append(isDefs? ")" : "")
                .append(" |");
        for (final var row : rowNode.parent().columns()) {
            if (JsonDocNames.FIELD.equals(row)) continue; // handled above
            buffer.append(" ");
            final var cellNode = rowNode.getChild(row);
            if (cellNode.isPresent()) {
                createCell(cellNode.get());
                doneIfNotTable(cellNode.get());
            }
            if (JsonDocNames.DESCRIPTION.equals(row)) {
                // FIXME $ref here as well
                if (isLocalRef(rowNode)) {
                    // If this is a local $ref, link to it
                    lineBreakIfNeeded();
                    buffer.append(createRefLink(rowNode));
                }
                else if (rowNode.values.isNonEmpty()) {
                    // If there are values directly on the row node, add it to description
                    lineBreakIfNeeded();
                    createCell(rowNode);
                }
                if (rowNode.isTable()) {
                    // Possible embedding
                    lineBreakIfNeeded();
                    //noinspection ConstantConditions
                    if (shouldEmbed(rowNode, level)) {
                        handleTableNode(rowNode, level + 1);
                        rowNode.done();
                    }
                    else if (rowNode.rows().size() > 0) buffer.append(createInternalLink(rowNode));
                }
            }
            buffer.append(" |");
        }
        buffer.append("\n");
    }

    private void lineBreakIfNeeded() {
        if (!buffer.toString().endsWith("| ") && !buffer.toString().endsWith(BR))
            buffer.append(BR);
    }

    // Embedding currently not supported
    @SuppressWarnings({"PointlessBooleanExpression", "unused"})
    private boolean shouldEmbed(final Node node, final int level) { return false && node.isEmbeddable(); }

    private void createCell(final Node node) {
        final var cellVal = NodeValues.listToString(node.values.all(), "", "\n", "");
        buffer.append(replaceNextLink(cellVal));
        if (node.nodeType.equals(NodeType.Array)) {
            lineBreakIfNeeded();
            final var elements = node.children.stream()
                    .map(n -> n.values.all())
                    .map(l-> NodeValues.listToString(l, "", " ", ""))
                    .toList();
            buffer.append(replaceNextLink(NodeValues.listToString(elements, "[", ", ", "]")));
        }
    }

    @Override
    protected String createUrlLink(final String url, final String optText) {
        if (optText==null || "".equals(optText)) return "[" + url + "]";
        return "[" + optText + "](" + url + ") ";
    }

    private String createInternalLink(final Node node) { return "[" + node.name + ">](#" + node.extId() + ")"; }

    protected String createRefLink(final Node node) {
        if (!isLocalRef(node)) return node.values.toString();
        final var ref = node.values.toString().substring(JsonDocNames.REF_LOCAL_PFX.length());
        return "[&#91;" + node.values.toString() + "&#93;](#$defs__" + ref +")";
    }
}

/** Output a graph description for Graphviz' dot generator. */
class GraphPrinter extends Printer {
    private static final String PREAMBLE = """
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

                """;


    GraphPrinter(final Node rootNode) { super(rootNode); }
    @Override protected String q(final String s) { return "\"" + s + "\""; }

    public String create() {
        buffer.append(PREAMBLE);
        handleTableNode(rootNode);
        buffer.append("}\n");
        return buffer.toString();
    }

    private void handleTableNode(final Node node) {
        try {
            if (!node.isVisible()) return;
            if (node.rows().size() > 0) {
                makeNode(node);
                for (final var row : node.rows()) handleRowNode(row);
            }
            for (final var sub : node.subTables()) handleTableNode(sub);
        }
        catch (final Throwable t) { handleException(node, t); }
    }

    private void makeNode(final Node node) {
        buffer.append("        ").append(q(node.qName())).append(" [\n")
              .append("                ").append("label = \"{").append(node.displayName()).append("\\n|}\"\n")
              .append("        ").append("]\n\n");
    }


    private void handleRowNode(final Node rowNode) {
        if (!rowNode.isVisible()) return; // Hidden or already processed
        doneIfNotTable(rowNode);
        if (rowNode.isTable() && rowNode.rows().size()>0) createEdge(rowNode);
    }

    private void createEdge(final Node node) {
        final var isDefs = JsonDocNames.DEFS.equals(node.name);
        final var indent = "        ";
        buffer.append(indent)
              .append(q(node.parent().qName())).append(" -> ").append(q(node.qName()))
              .append(" [ ");
        if (isDefs) buffer.append('\n')
                          .append(indent).append("  ").append("style = dashed").append('\n')
                          .append(indent).append("  ");
        buffer.append("label = \"").append(node.cardinality()).append("\"");
        if (isDefs) buffer.append('\n').append(indent);
        buffer.append(" ]")
              .append("\n\n");
    }
}

/** Copies the input, except for elements with a given set of prefixes. */
class SchemaPrinter extends Printer {
    private static final List<String> EXCLUDE_PREFIXES =
            Arrays.asList(JsonDocNames.IGNORE_PREFIX, JsonDocNames.XDOC_PREFIX, JsonDocNames.XIF_PREFIX,
                          JsonDocNames.XIFNOT_PREFIX);
    static final List<NodeRepresentation> HIDDEN = Arrays.asList(NodeRepresentation.HiddenColumn,
            NodeRepresentation.HiddenRow, NodeRepresentation.HiddenTable);

    SchemaPrinter(final Node rootNode) { super(rootNode); }
    boolean include(final Node node) { return EXCLUDE_PREFIXES.stream().noneMatch(node.name::startsWith); }
    protected String makeIndent(final Node node) { return " ".repeat(2* (node.level()-1));}
    void skipLastComma() { /*Delete when sure it won't be missed...:  buffer.setLength((buffer.length()-2)); */}

    protected static String schemaClean(final String s) {
        return s.replaceAll("(?s),(\\s*[\\]\\[}])","$1")
                .replaceAll("\n\n+", "\n");
    }

    public String create() {
        handleNode(rootNode);
        return schemaClean(buffer.toString());
    }

    protected void handleNode(final Node node) {
        try {
            final var visible = node.isVisible() && include(node)
                    && HIDDEN.stream().noneMatch(nr -> node.representation.equals(nr));
            final var vals = NodeValues.listToString(node.values.all(), "", "", "");

            if (visible) switch (node.nodeType) {
                case Object -> {
                    if (node.parent() != null) appendName(node);
                    buffer.append("{\n");
                    if (!vals.isEmpty()) Logger.warn("Values directly on object", node.qName(), vals);
                }
                case Array -> {
                    appendName(node).append("[\n");
                    if (!vals.isEmpty()) Logger.warn("Values directly on array", node.qName(), vals);
                }
                case Value -> {
                    if (node.parent().nodeType.equals(NodeType.Array)) buffer.append(makeIndent(node));
                    else appendName(node);
                    switch (node.dataType) {
                        case NA -> Logger.error("Unknown data type for", node.qName());
                        case NullValue -> buffer.append("null,\n");
                        case StringType -> buffer.append('"').append(vals).append('"').append(",\n");
                        case IntType, DoubleType, BooleanType -> buffer.append(vals).append(",\n");
                    }
                }
            }

            for (final var child : node.children) handleNode(child);

            if (visible) switch (node.nodeType) {
                case Object -> {
                    skipLastComma();
                    buffer.append('\n').append(makeIndent(node)).append("},\n");
                    if (node.parent() == null) skipLastComma();
                }
                case Array -> {
                    skipLastComma();
                    buffer.append('\n').append(makeIndent(node)).append("],\n");
                }
                case Value -> {
                }
            }
        }
        catch (final Throwable t) { handleException(node, t); }
    }

    protected StringBuilder appendName(final Node node) {
        buffer.append(makeIndent(node))
                .append('"').append(node.name).append('"')
                .append(": ");
        return buffer;
    }
}

/** EXPERIMENTAL - creates a sample JSON file based on given columns and/or generated values. */
class SamplePrinter extends SchemaPrinter {

    /** Explicitly specified (on command line) possible sample column names. */
    final List<String> sampleCols = new LinkedList<>();
    private static final Random random = new Random();

    SamplePrinter(final Node rootNode, final Context context) {
        super(rootNode);
        context.value(Context.SAMPLE_COLUMNS).ifPresent(sample ->
                this.sampleCols.addAll(Arrays.asList(sample.split(", *"))));
    }

    @Override
    public String create() {
        buffer.append("{\n");
        handleSampleNode(rootNode, 0, 0);
        skipLastComma();
        buffer.append("\n}");
        return schemaClean(buffer.toString());
    }

    /** Test output stripped of blanks and quotes. */
    String testString() {
        return create()
                .replaceAll("\\s+", "")
                .replaceAll("[\"']", "");
    }

    private int noOfItems(final Node node) {
        // If this is an array "items" -- return the parent items minItems if it exists, that is the number of
        // array samples to produce. For all other items, or if minItems is not set, return 1
        if (node.parent()==null) return 1;
        final var parent = node.parent();
        if (!parent.declaredAsArray()) return 1;
        final var minVal = parent.getChild(JsonDocNames.MIN_ITEMS)
                .map(n-> n.values.first().toString())
                .map(Integer::parseInt);
        return Math.max(minVal.orElse(1), 1);
    }

    protected boolean handleSampleNode(final Node node, final int nth, final int relativeChild) {
        try {
            // If there is an empty examples array, assume no sample is needed
            final var ex = node.getChild(JsonDocNames.EXAMPLES);
            final var emptyExample = ex.isPresent() && ex.get().children.isEmpty();
            final var arrayItem = node.parent() != null
                    && node.parent().declaredAsArray()
                    && JsonDocNames.ITEMS.equals(node.name);

            final var visible = node.isVisible() && include(node)
                    && HIDDEN.stream().noneMatch(nr-> node.representation.equals(nr))
                    && !node.name.isEmpty()
                    && (node.isRow() || node.isTable())
                    && !node.children.isEmpty()
                    && node.nodeType.equals(NodeType.Object)
                    && !emptyExample;

            if (visible)  {
                if (nth>0 && relativeChild==0)
                    buffer.append(makeIndent(node.parent()))
                            .append("},\n")
                            .append(makeIndent(node.parent()))
                            .append("{\n");
                if (arrayItem) buffer.append(makeIndent(node));
                else if (node.parent()!=null) appendName(node);

                if (node.representation.equals(NodeRepresentation.Table))
                    buffer.append(node.declaredAsArray()? '[' : '{');
                else if (node.declaredAsArray()) buffer.append("[");
                else if (arrayItem) {
                    final var exampleCount = noOfItems(node);
                    for (int i = 0; i < exampleCount; i++)
                        buffer.append(prioritizedSample(node, i)).append(",");
                }
                else buffer.append(prioritizedSample(node, nth)).append(",");
                buffer.append("\n");
            }

            if (!emptyExample) {
                final var exampleCount = noOfItems(node);
                for (int i = 0; i < exampleCount; i++) {
                    int relative = 0;
                    for (final var child : node.children)
                        if (handleSampleNode(child, i, relative)) relative++;
                }
            }

            if (visible) {
                if (node.representation.equals(NodeRepresentation.Table) || node.declaredAsArray()) {
                    skipLastComma();
                    buffer.append('\n')
                            .append(makeIndent(node))
                            .append(node.declaredAsArray()? ']' : '}')
                            .append(",\n");
                }
                if (node.parent()==null) skipLastComma();
            }
            return visible;
        }
        catch (final Throwable t) {
            handleException(node, t);
            return false;
        }
    }

    private String prioritizedSample(final Node node, final int nth) {
        final var possible = new LinkedList<>();
        final var type = node.getChild(JsonDocNames.TYPE).map(n-> n.values.first().toString()).orElse("string");

        // Use explicitly given column names if possible
        for (final var c : sampleCols) {
            final var child = node.getChild(c);
            child.ifPresent(value -> possible.addAll(value.values.all()));
        }
        // Otherwise, use examples attribute
        if (possible.isEmpty()) {
            final var ex = node.getChild(JsonDocNames.EXAMPLES);
            if (ex.isPresent()) for (final var child : ex.get().children) {
                possible.addAll(child.values.all());
            }
        }
        // Otherwise, if this is a const, the sample is given...
        if (possible.isEmpty()) {
            final var fix = node.getChild(JsonDocNames.CONST);
            fix.ifPresent(value -> possible.addAll(value.values.all()));
        }
        // Or if this is an enum, pick a value
        if (possible.isEmpty()) {
            final var enums = node.getChild(JsonDocNames.ENUM);
            if (enums.isPresent()) for (final var child : enums.get().children) {
                possible.addAll(child.values.all());
            }
        }
        // If all else fails, generate a type dependent default
        if (possible.isEmpty()) possible.add(defaultSample(node, type));
        possible.remove(null); // if any have snuck in

        // possibly rotate over the candidates
        final var smp = (possible.isEmpty())? "" : possible.get(nth % possible.size());
        if ("string".equals(type)) return '"' + smp.toString() + '"';
        return smp.toString();
    }


    private static Object defaultSample(final Node node, final String type) {
        switch (type) {
            case "string" -> {
                final var optMinLen = node.getChild(JsonDocNames.MIN_LENGTH).map(n -> n.values.first().toString());
                final var optMaxLen = node.getChild(JsonDocNames.MAX_LENGTH).map(n -> n.values.first().toString());
                final var minLen = Integer.parseInt(optMinLen.orElse("0"));
                final var maxLen = Integer.parseInt(optMaxLen.orElse("20"));
                final var midLen = (minLen + maxLen) / 2;
                String tst = "ABCD0123EFGH4567IJKL89MNOPQRSTUVWXYZ";
                final var offs = random.nextInt(tst.length());
                while (tst.length() < (midLen+offs+1)) tst+=tst;
                return tst.substring(offs, midLen+offs);
            }
            case "integer" -> { return sampleInt(node); }
            case "number" -> { return sampleNumber(node); }
            case "boolean" -> { return Boolean.TRUE; }
            case "null" -> { return "null"; }
        }
        return null;
    }

    private static Integer sampleInt(final Node node) {
        final var optMin = node.getChild(JsonDocNames.MINIMUM).map(n -> n.values.first().toString());
        final var optMax = node.getChild(JsonDocNames.MAXIMUM).map(n -> n.values.first().toString());
        final var min = Integer.parseInt(optMin.orElse("0"));
        final var max = Integer.parseInt(optMax.orElse("1024"));
        return min + random.nextInt((Math.abs(max-min+1)));
    }

    private static BigDecimal sampleNumber(final Node node) {
        final var optMin = node.getChild(JsonDocNames.MINIMUM).map(n -> n.values.first().toString());
        final var optMax = node.getChild(JsonDocNames.MAXIMUM).map(n -> n.values.first().toString());
        final var min = new BigDecimal(optMin.orElse("" + Long.MIN_VALUE));
        final var max = new BigDecimal(optMax.orElse("" + Long.MAX_VALUE));
        final var range = max.subtract(min).abs().doubleValue();
        final var rnd = min.doubleValue() + random.nextDouble(range);
        return new BigDecimal(rnd).setScale(Math.max(min.scale(), max.scale()), RoundingMode.HALF_UP);
    }
}

//   Copyright 2021-2023, Lars Reed -- lars-at-kalars.net
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