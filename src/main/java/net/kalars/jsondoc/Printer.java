package net.kalars.jsondoc;

import org.apache.commons.text.StringEscapeUtils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;


abstract class Printer {
    protected final StringBuilder buffer = new StringBuilder();
    protected final Node rootNode;
    protected final Context context;
    protected static final Pattern USER_LINK_REGEXP = // links written by user
            Pattern.compile(JsonDocNames.USER_LINK_RE);

    Printer(final Node rootNode, final Context context) {
        this.rootNode = rootNode;
        this.context = context;
    }

    protected static String keyToTitle(final String key) {
        final var no_ = Node.removePrefix(key, JsonDocNames.XDOC_PREFIX).replaceAll("_", " ");
        if (no_.length()>=1)  return no_.substring(0, 1).toUpperCase() + no_.substring(1);
        return key;
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

    protected final void doneIfNotTable(final Node node) {
        if (!NodeRepresentation.Table.equals(node.representation))  node.done();
    }

    protected String createUrlLink(final String url, final String linkText) { return url; }
    protected String q(final String s) { return s; }
}

class DebugPrinter extends Printer {

    DebugPrinter(final Node rootNode, final Context context) { super(rootNode, context); }

    private String makeIndent(final int level) { return "  ".repeat(level); }

    @Override
    public String toString() {
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
        </head>
        <body>
        """;


    HtmlPrinter(final Node rootNode, final Context context) { super(rootNode, context); }

    @Override
    protected String q(final String s) {
        return StringEscapeUtils.escapeXml11(s)
                .replaceAll("\t", "&nbsp;&nbsp;")
                .replaceAll("\n", "<br/>");
    }

    @Override
    public String toString() {
        head();
        handleTableNode(rootNode, 0);
        tail();
        return buffer.toString();
    }

    protected void head() {
        buffer.append("""
                <!doctype html>
                <html>
                  <head>
                    <meta charset=utf-8>
                    """)
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
        if (!node.isVisible()) return;
        if (node.rows().size() > 0) {
            if (level == 0) buffer.append(headingWithId(node)); // not embedded
            buffer.append(tableHead(node));
            for (final var row : node.rows()) handleRowNode(row, level);
            buffer.append(tableEnd());
        }
        if (level>0) return; // No embedding in embedding...
        for (final var sub : node.subTables()) handleTableNode(sub, 0);
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

    private String tableEnd() { return "</tbody></table>\n"; }

    private void handleRowNode(final Node rowNode, final int level) {
       if (!rowNode.isVisible()) return; // Hidden or already processed
        doneIfNotTable(rowNode);
        buffer.append("<tr>")
                   .append("<td>")
                   .append(q(rowNode.name))
                   .append("</td>");
        for (final var row : rowNode.parent().columns()) {
            if (JsonDocNames.FIELD.equals(row)) continue; // handled above
            buffer.append("<td>");
            final var cellNode = rowNode.getChild(row);
            if (cellNode.isPresent()) {
                createCell(cellNode.get());
                doneIfNotTable(cellNode.get());
            }
            if (JsonDocNames.DESCRIPTION.equals(row) && !rowNode.values.isEmpty()) {
                // If there are values directly on the row node, add it to description
                lineBreakIfNeeded();
                createCell(rowNode);
            }
            if (JsonDocNames.DESCRIPTION.equals(row) && rowNode.isTable()) {
                // Possible embedding
                lineBreakIfNeeded();
                if (shouldEmbed(rowNode, level)) {
                    handleTableNode(rowNode, level+1);
                    rowNode.done();
                }
                else if (rowNode.rows().size()>0) buffer.append(createInternalLink(rowNode));
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
}

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
                "<ac:plain-text-link-body><![CDATA[" + node.name + "]]></ac:plain-text-link-body>" +
                "</ac:link>";
    }
}

class MarkdownPrinter extends Printer {

    private static final String BR = "<br />";

    MarkdownPrinter(final Node rootNode, final Context context) { super(rootNode, context); }

    @Override
    protected String q(final String s) {
        return s.replaceAll("[-`*|_{}()#+]", "\\\\$0")
                .replaceAll("[]\\[]", "\\\\$0")
                .replaceAll("\t", "&nbsp;&nbsp;")
                .replaceAll("\n", BR);
    }

    @Override
    public String toString() {
        handleTableNode(rootNode, 0);
        return buffer.toString();
    }

    private void handleTableNode(final Node node, final int level) {
        if (!node.isVisible()) return;
        if (node.rows().size() > 0) {
            if (level == 0) buffer.append(headingWithId(node)); // not embedded
            buffer.append(tableHead(node));
            for (final var row : node.rows()) handleRowNode(row, level);
        }
        if (level>0) return; // No embedding in embedding...
        for (final var sub : node.subTables()) handleTableNode(sub, 0);
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
        buffer.append("| ")
                .append(q(rowNode.name))
                .append(" |");
        for (final var row : rowNode.parent().columns()) {
            if (JsonDocNames.FIELD.equals(row)) continue; // handled above
            buffer.append(" ");
            final var cellNode = rowNode.getChild(row);
            if (cellNode.isPresent()) {
                createCell(cellNode.get());
                doneIfNotTable(cellNode.get());
            }
            if (JsonDocNames.DESCRIPTION.equals(row) && !rowNode.values.isEmpty()) {
                // If there are values directly on the row node, add it to description
                lineBreakIfNeeded();
                createCell(rowNode);
            }
            if (JsonDocNames.DESCRIPTION.equals(row) && rowNode.isTable()) {
                // Possible embedding
                lineBreakIfNeeded();
                //noinspection ConstantConditions
                if (shouldEmbed(rowNode, level)) {
                    handleTableNode(rowNode, level+1);
                    rowNode.done();
                }
                else if (rowNode.rows().size()>0) buffer.append(createInternalLink(rowNode));
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
    @SuppressWarnings({"PointlessBooleanExpression", "ConstantConditions", "unused"})
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
        return "[" + optText + "](" + url + ")";
    }

    private String createInternalLink(final Node node) { return "[" + node.name + ">](#" + node.extId() + ")"; }
}

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


    GraphPrinter(final Node rootNode, final Context context) { super(rootNode, context); }
    @Override protected String q(final String s) { return "\"" + s + "\""; }

    @Override
    public String toString() {
        buffer.append(PREAMBLE);
        handleTableNode(rootNode);
        buffer.append("}\n");
        return buffer.toString();
    }

    private void handleTableNode(final Node node) {
        if (!node.isVisible()) return;
        if (node.rows().size() > 0) {
            makeNode(node);
            for (final var row : node.rows()) handleRowNode(row);
        }
        for (final var sub : node.subTables()) handleTableNode(sub);
    }

    private void makeNode(final Node node) {
        buffer.append("        ").append(q(node.name)).append(" [\n")
              .append("                ").append("label = \"{").append(node.displayName()).append("\\n|}\"\n")
              .append("        ").append("]\n\n");
    }


    private void handleRowNode(final Node rowNode) {
        if (!rowNode.isVisible()) return; // Hidden or already processed
        doneIfNotTable(rowNode);
        if (rowNode.isTable() && rowNode.rows().size()>0) createEdge(rowNode);
    }

    private void createEdge(final Node node) {
        buffer.append("        ")
              .append(q(node.parent().name)).append(" -> ").append(q(node.name))
              .append(" [ label = \"").append(node.cardinality()).append("\" ]")
              .append("\n\n");
    }
}

/** Copies the input, except for elements with a given set of prefixes. */
class SchemaPrinter extends Printer {
    private static final List<String> EXCLUDE_PREFIXES =
            Arrays.asList(JsonDocNames.IGNORE_PREFIX, JsonDocNames.XDOC_PREFIX, JsonDocNames.XIF_PREFIX,
                          JsonDocNames.XIFNOT_PREFIX);
    private static final List<NodeRepresentation> HIDDEN = Arrays.asList(NodeRepresentation.HiddenColumn,
            NodeRepresentation.HiddenRow, NodeRepresentation.HiddenTable);

    SchemaPrinter(final Node rootNode, final Context context) { super(rootNode, context); }
    private boolean exclude(final Node node) { return EXCLUDE_PREFIXES.stream().anyMatch(node.name::startsWith); }
    private String makeIndent(final Node node, final int extra) { return " ".repeat(extra + -2 + 2*node.level());}
    private void skipLastComma() { buffer.setLength((buffer.length()-2)); }

    @Override
    public String toString() {
        handleNode(rootNode);
        return buffer.toString();
    }

    private void handleNode(final Node node) {
        final var visible = node.isVisible() && !exclude(node)
                && HIDDEN.stream().noneMatch(nr-> node.representation.equals(nr));
        final var vals = NodeValues.listToString(node.values.all(), "", "", "");

        if (visible) switch (node.nodeType) {
            case Object -> {
                if (node.parent()!=null) appendName(node);
                buffer.append("{\n");
                if (!vals.isEmpty()) Logger.warn("Values directly on object", node.qName(), vals);
            }
            case Array -> {
                appendName(node).append("[\n");
                if (!vals.isEmpty()) Logger.warn("Values directly on array", node.qName(), vals);
            }
            case Value -> {
                if (node.parent().nodeType.equals(NodeType.Array)) buffer.append(makeIndent(node, 0));
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
                buffer.append('\n').append(makeIndent(node, 0)).append("},\n");
                if (node.parent()==null) skipLastComma();
            }
            case Array -> {
                skipLastComma();
                buffer.append('\n').append(makeIndent(node, 0)).append("],\n");
            }
            case Value -> { }
        }
    }

    private StringBuilder appendName(final Node node) {
        buffer.append(makeIndent(node, 0))
                .append('"').append(node.name).append('"')
                .append(": ");
        return buffer;
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