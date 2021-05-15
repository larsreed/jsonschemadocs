package net.kalars.jsondoc;

//private final String name;
//private boolean visible = true;
//private boolean hideChildren = false;
//private NodeType nodeType;
//private final DataType dataType;
//private final List<Node> children = new LinkedList<>();
//private final Node parent;
//private final List<Object> values = new ArrayList<>();
//private final Context context;
//private boolean required = false;
//private NodeRepresentation representation;
//private String generatedSample = "";


class Printer {
    protected final StringBuilder buffer = new StringBuilder();
}

class DebugPrinter extends Printer {
    private final Node rootNode;
    private final Context context;

    DebugPrinter(final Node rootNode, final Context context) {
        this.rootNode = rootNode;
        this.context = context;
    }

    private String makeIndent(final int level) { return "  ".repeat(level); }

    @Override
    public String toString() {
        handleNode(this.rootNode, 0);
        return this.buffer.toString();
    }

    private void handleNode(final Node node, final int level) {
        this.buffer.append(makeIndent(level))
                   .append(node.name)
                   .append(" ")
                   .append(node.nodeType())
                   .append(" ")
                   .append(node.dataType)
                   .append(" ")
                   .append(node.representation)
                   .append(" ")
                   .append(node.generatedSample);
        if (node.isRequired()) this.buffer.append(" required");
        if (!node.isVisible()) this.buffer.append(" invisible");
        if (node.hasHiddenChildren()) this.buffer.append(" hiddenChildren");
        this.buffer.append("\n");
        for (final var v : node.values)
            this.buffer.append(makeIndent(level +1))
                        .append("Val ")
                        .append(v)
                        .append("\n");
        for (final var c : node.children)
            handleNode(c, level +2);
    }
}