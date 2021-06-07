package net.kalars.jsondoc;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Node {
    final String name;
    private boolean visible = true;
    final NodeType nodeType;
    final DataType dataType;
    final List<Node> children = new LinkedList<>();
    private Node parent;
    final NodeValues values = new NodeValues();
    private final Context context;
    private boolean required = false;
    NodeRepresentation representation;
    private final Node topRow;

    Node(final String name, final NodeType nodeType, final DataType dataType, final Object value, final Node parent,
         final Context context) {
        this.name = (name==null)? "" : name;
        this.nodeType = nodeType;
        this.dataType = dataType;
        this.parent = parent;
        if (value!=null) this.values.add(value);
        this.context = context;
        if (parent!=null) {
            parent.add(this);
            topRow = null;
        }
        else { // A node we might need to keep root node properties
            topRow = new Node("", NodeType.Object, this, context);
            topRow.representation = NodeRepresentation.HiddenRow;
            if (!context.isSchemaMode()) children.add(topRow); // leave schema untouched
        }
        setDefaultRepresentation();
    }

    Node(final String name, final NodeType nodeType, final Node parent, final Context context) {
        this(name, nodeType, DataType.NA, null, parent, context);
    }

    /** Node not hidden or processed? */ boolean isVisible() { return this.visible; }
    /** Mark as processed. */ void done() { this.visible = false; }
    boolean isTable() { return NodeRepresentation.Table.equals(this.representation)
            || NodeRepresentation.EmbeddedTable.equals(this.representation); }
    boolean isColumn() { return NodeRepresentation.Column.equals(this.representation); }
    boolean isRow() { return NodeRepresentation.Row.equals(this.representation)
            || NodeRepresentation.EmbeddedTable.equals(this.representation); }
    boolean isRequired() { return required; }
    Node parent() { return this.parent; }

    @Override
    public String toString() {
        return "Node {" +
                this.displayName() +
                (this.visible ? " " : " invisible ") +
                this.nodeType + ' ' +
                this.dataType + ' ' +
                this.representation + ' ' +
                this.children.size() + ' ' +
                (this.required ? "req" : "") +
                '}';
    }

    private void setDefaultRepresentation() {
        switch (this.nodeType) {
            case Object -> this.representation = NodeRepresentation.Table;
            case Array, Value -> this.representation = NodeRepresentation.Row;
        }
    }

    /** Track parent chain. */
    private List<Node> heritage() {
        final List<Node> list = new LinkedList<>();
        if (this.parent!=null) list.addAll(this.parent.heritage());
        list.add(this);
        return list;
    }

    /** Unique name -- qualified by ancestor names. */
    String qName() {
        if (parent==null) return displayName();
        final var list = heritage().stream()
                .skip(1)
                .map(Node::displayName)
                .toList();
        return NodeValues.listToString(list,
                "", " > ", "");
    }

    /** Title for tables etc. */
    String displayName() {
        if (!name.isEmpty())  return name;
        final var titleNode = getChild(JsonDocNames.TITLE);
        final var vals = titleNode.map(n -> n.values).orElse(new NodeValues());
        if (vals.isNonEmpty() && !vals.first().toString().isEmpty())  return vals.first().toString();
        final var descVals = titleNode
                .flatMap(n -> n.getChild(JsonDocNames.DESCRIPTION))
                .map(n -> n.values)
                .orElse(new NodeValues());
        if (descVals.isNonEmpty())  return descVals.first().toString();
        return "";
    }

    /** Depth in tree. */
    int level() { return heritage().size(); }

    /** Unique id for use as anchor etc (not for display). */
    String extId() {
        final var names = heritage().stream()
                .map(n -> n.name.replaceAll("[^a-zA-Z0-9-]", "_"))
                .toList();
        return NodeValues.listToString(names, "", "__", "")
                .replaceAll("^_+", "")
                .replaceAll("^$", "_");
    }

    /** Column names for this node (should be of type object). */
    List<String> columns() {
        final Set<String> list = new LinkedHashSet<>();
        list.addAll(JsonDocNames.ALWAYS_COLUMNS);
        list.addAll(this.children.stream()
                .filter(Node::isColumn)
                .map(n -> n.name)
                .filter(n -> ! list.contains(n))
                .toList());
        list.addAll(this.children.stream()
                .filter(Node::isRow)
                .flatMap(n -> n.children.stream())
                .filter(Node::isColumn)
                .map(n -> n.name)
                .filter(n -> ! list.contains(n))
                .toList());
        return list.stream().toList();
    }

    /** Rows in this node (should be of type object). */
    List<Node> rows() {
        if (!NodeType.Object.equals(this.nodeType)) Logger.warn(qName(), "is not an object");
        return this.children.stream()
                .filter(n -> n.visible && (n.isRow() || n.isTable()))
                .toList();
    }

    /** Subtables in this node (should be of type object). */
    List<Node> subTables() {
        if (!NodeType.Object.equals(this.nodeType)) Logger.warn(qName(), "is not an object");
        return this.children.stream()
                .filter(n -> n.isVisible() && n.isTable())
                .toList();
    }

    /** Add a child node. */ void add(final Node node) { this.children.add(node); }
    /** Remove a child node. */ private void remove(final Node node) { this.children.remove(node); }

    /** Find a named child under this node. */
    Optional<Node> getChild(final String name) {
        for (final var node : this.children) if (name.equals(node.name)) return Optional.of(node);
        return Optional.empty();
    }

    /** Called when this instance is complete from the parser. */
    Node finalized() {
        if (notSchemaMode()) {
            // Attach "properties" members directly to parent
            if (this.nodeType.equals(NodeType.Object) && JsonDocNames.PROPERTIES.equals(name)) return removeThisNode();
            // Convert "required" to attributes
            if (nodeType.equals(NodeType.Array) && JsonDocNames.REQUIRED.equals(name)) return convertRequired();
        }
        if (context.isSchemaMode() && context.contains(Context.STRICT)
                && this.nodeType.equals(NodeType.Object) && JsonDocNames.PROPERTIES.equals(name) ) {
            @SuppressWarnings("unused")
            final var add = new Node(JsonDocNames.ADDITIONAL_PROPERTIES, NodeType.Value, DataType.BooleanType,
                    Boolean.FALSE, parent, context); // Automatically inserted
        }
        if ( nodeType.equals(NodeType.Value))  handleXif();
        defineColumns();
        if (context.isExcluded(name)) visible = false;
        if (notSchemaMode()) convertKnownProperties(); // Transform known properties to columns
        if (isTable() && notSchemaMode()) finalizeTable();
        if (!visible) { // Fix representation if hidden
            switch (representation) {
                case Row ->  representation = NodeRepresentation.HiddenRow;
                case Column -> representation = NodeRepresentation.HiddenColumn;
                case Table, EmbeddedTable -> representation = NodeRepresentation.HiddenTable;
            }
        }
        return this;
    }

    private void finalizeTable() {
        // Convert table to row if no separate content
        final long hasRows = children.stream()
                .filter(n -> n.isRow() || (n.isTable() && !n.isEmbeddable()))
                .count();
        if (hasRows == 0) {
            if (hasChildren()) representation = NodeRepresentation.EmbeddedTable;
            else representation = NodeRepresentation.Row;
        }
        if (parent == null) {
            // Are there any attributes directly on the top node?  Create a row for these
            final var orgList = new LinkedList<>(children);
            for (final Node ch : orgList) {
                if (ch.representation.equals(NodeRepresentation.Column)) {
                    topRow.add(ch);
                    ch.parent.remove(ch);
                    ch.parent = topRow;
                }
            }
            if (topRow.hasChildren()) topRow.representation = NodeRepresentation.Row; // unhide
            else topRow.visible = false;
        }
    }

    private void defineColumns() {
        if (name.startsWith(JsonDocNames.XDOC_PREFIX)) representation = NodeRepresentation.Column;
        else if (JsonDocNames.ALWAYS_COLUMNS.contains(name)) representation = NodeRepresentation.Column;
        else if (JsonDocNames.PROP_KEYWORDS.contains(name)) representation = NodeRepresentation.Column;
    }

    private void handleXif() {
        // Handle variants
        if (name.startsWith(JsonDocNames.XIFNOT_PREFIX)) {
            final var key = removePrefix(name, JsonDocNames.XIFNOT_PREFIX);
            final var vals = values.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(","));
            final var matches = context.anyMatch(key, vals);
            if (matches.isPresent() && matches.get()) parent.visible = false;
            visible = false;
        }
        else if (name.startsWith(JsonDocNames.XIF_PREFIX)) {
            final var key = removePrefix(name, JsonDocNames.XIF_PREFIX);
            final var vals = values.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(","));
            final var matches = context.anyMatch(key, vals);
            if (matches.isPresent() && ! matches.get()) parent.visible = false;
            visible = false;
        }
    }

    private boolean notSchemaMode() { return !this.context.isSchemaMode(); }

    private boolean hasChildren() {
        return (nodeType.equals(NodeType.Object) || nodeType.equals(NodeType.Array))
                && !children.isEmpty();
    }

    boolean isEmbeddable() {
        // Embed single 'items' (array content)
        if (rows().size()==1 && JsonDocNames.ITEMS.equals(rows().get(0).name)) return true;
        // Embed if within limit set in context
        final int embedUpTo = Integer.parseInt(context.value(Context.EMBED_ROWS).orElse("-1"));
        return rows().size() <= embedUpTo;
    }

    private Node removeThisNode() {
        if (parent==null) throw new RuntimeException("Wrong structure, cannot remove child without parent");
        children.forEach(n -> {
            n.parent = this.parent;
            parent.add(n);
        });
        parent.remove(this);
        return parent;
    }

    private Node convertRequired() {
        final var names = children.stream().map(n -> n.values.first()).toList();
        new LinkedList<>(parent.children).forEach(n -> {
            if (names.contains(n.name)) {
                n.required = true;
                n.addToType(JsonDocNames.REQUIRED);
            }
        });
        parent.remove(this);
        return parent;
    }

    private void convertKnownProperties() {
        convertFormat();
        strict();
        minMax();
        minMaxLen();
        minMaxItems();
        enums();
        miscProps();
        idToDesc();
    }

    static String removePrefix(final String s, final String pfx) { return s.replaceAll("^" + pfx, ""); }

    private List<Object> extract(final String key) {
        final var child = getChild(key);
        if (child.isEmpty()) return List.of();
        final var other = child.get();
        other.visible = false;
        other.representation = NodeRepresentation.HiddenColumn;
        return other.values.all();
    }

    private String extractString(final String s) { return NodeValues.listToString(extract(s), "", " ", ""); }

    private boolean extractBoolean(final String key) {
        final var found = extract(key);
        if (!found.isEmpty()) {
            for (final var o : found) if (o.toString().equalsIgnoreCase("TRUE")) return true;
        }
        return false;
    }

    private void addToType(final List<Object> other) {
        var node = getChild(JsonDocNames.TYPE);
        if (node.isEmpty())  {
            node = Optional.of(
                    new Node(JsonDocNames.TYPE, NodeType.Value, DataType.StringType, null, this, context));
            if (parent !=null) parent.add(node.get());
        }
        node.get().values.addAll(other);
    }

    private void addToType(final String s) { if (s!=null && !s.isEmpty()) addToType(List.of(s)); }

    private void convertFormat() {
        final var other = extract(JsonDocNames.FORMAT);
        if (other.size()>0)  addToType(other);
    }

    private void strict() {
        final var addProp = extract(JsonDocNames.ADDITIONAL_PROPERTIES);
        if (addProp.isEmpty()) return;
        if (addProp.get(0).toString().equalsIgnoreCase(Boolean.TRUE.toString())) return;
        if (addProp.get(0).toString().equalsIgnoreCase(Boolean.FALSE.toString()))
            addToType(JsonDocNames.ADDITIONAL_PROPERTIES_DISP);
        else addToType(JsonDocNames.ADDITIONAL_PROPERTIES + " " + addProp);
    }

    private void minMax() {
        boolean minDefault = false;
        var min = extractString(JsonDocNames.MINIMUM);
        var max = extractString(JsonDocNames.MAXIMUM);

        if (!max.isEmpty() && !min.isEmpty() && min.equals(max)) { // Exact value, min=max
            addToType("[" + min + "]");
            return;
        }

        if (min.isEmpty()) {
            min = extractString(JsonDocNames.EXCLUSIVE_MINIMUM);
            if (min.isEmpty()) {
                min = "[0";
                minDefault = true;
            }
            else min = "<" + min;
        }
        else min = "[" + min;

        if (max.isEmpty()) {
            max = extractString(JsonDocNames.EXCLUSIVE_MAXIMUM);
            if (max.isEmpty()) {
                if (minDefault) return; // neither given
                max = "...]";
            }
            else max = max + ">";
        }
        else max = max + "]";

        addToType(min + ", " + max);
    }

    private void minMaxLen() {
        var min = extractString(JsonDocNames.MIN_LENGTH);
        var max = extractString(JsonDocNames.MAX_LENGTH);
        var exact = false;

        if (!min.isEmpty() && !max.isEmpty() && min.equals(max)) { // Exact value
            min = "[" + min + "]";
            max = "";
            exact = true;
        }
        else if (!min.isEmpty()) min = "[" + min + "..";
        if (!max.isEmpty()) max = max + "]";

        if (min.isEmpty() && !max.isEmpty()) min = "[0..";
        else if (min.isEmpty()) return;
        else if (max.isEmpty() && !exact) max = ".]";

        addToType(min + max);
    }

    String cardinality() {
        final var minNode = getChild(JsonDocNames.MIN_ITEMS);
        final var maxNode = getChild(JsonDocNames.MAX_ITEMS);
        var min = minNode.isEmpty()? "" : NodeValues.listToString(minNode.get().values.all(), "", " ", "");
        var max = maxNode.isEmpty()? "" : NodeValues.listToString(maxNode.get().values.all(), "", " ", "");

        if (min.isEmpty() && max.isEmpty()) { // Unspecified
            return required? JsonDocNames.REQUIRED : "";
        }
        if (!min.isEmpty() && !max.isEmpty() && min.equals(max)) { // Exact value
            min = "[" + min + "]";
            max = "";
        }
        else {
            if (!min.isEmpty()) {
                min = "[" + min + ", ";
            }
            else if (required) min = "[1, ";
            else min = "[0, ";
            if (max.isEmpty()) max = "...]";
            else max = max + "]";
        }

        return min + max;
    }

    private void minMaxItems() {
        if (hasChildren()) addToType(cardinality());
        // hide these
        extract(JsonDocNames.MIN_ITEMS);
        extract(JsonDocNames.MAX_ITEMS);
    }

    private void enums() {
        if (!JsonDocNames.ENUM.equals(name) || !NodeType.Array.equals(nodeType)) return;
        final var vx = children.stream().map(n -> n.values).flatMap(v -> v.all().stream()).toList();
        visible = false;
        final var cvt = NodeValues.listToString(vx, "{", ", ", "}");
        if (!cvt.isEmpty()) parent.addToType(cvt);
    }

    private void miscProps() {
        if (extractBoolean(JsonDocNames.UNIQUE_ITEMS)) addToType(JsonDocNames.UNIQUE_ITEMS);
        if (extractBoolean(JsonDocNames.WRITE_ONLY)) addToType(JsonDocNames.WRITE_ONLY);
        if (extractBoolean(JsonDocNames.READ_ONLY)) addToType(JsonDocNames.READ_ONLY);

        extract(JsonDocNames.PATTERN).forEach(s -> addToType(JsonDocNames.PATTERN + "=" + s));
        extract(JsonDocNames.PROPERTY_NAMES).forEach(s -> addToType(JsonDocNames.PROPERTY_NAMES_DISP + ": " + s));
        extract(JsonDocNames.CONST).forEach(s -> addToType("==" + s));
        extract(JsonDocNames.MULTIPLE_OF).forEach(s -> addToType(JsonDocNames.MULTIPLE_OF + " " + s));
        extract(JsonDocNames.DEFAULT).forEach(s -> addToType(JsonDocNames.DEFAULT + "=" + s));
        extract(JsonDocNames.CONTENT_ENCODING).forEach(s -> addToType(JsonDocNames.CONTENT_ENCODING + "=" + s));
        extract(JsonDocNames.CONTENT_MEDIA_TYPE).forEach(s -> addToType(JsonDocNames.CONTENT_MEDIA_TYPE + "=" + s));
        extract(JsonDocNames.CONTENT_SCHEMA).forEach(s -> addToType(JsonDocNames.CONTENT_SCHEMA + "=" + s));

        if (extractBoolean(JsonDocNames.DEPRECATED)) addToType(JsonDocNames.DEPRECATED.toUpperCase() + "!");
    }

    private void idToDesc() {
        final var idf = NodeValues.listToString(extract(JsonDocNames.ID), "", ", ", "");
        if (!idf.isEmpty()) addToType(idf);
    }
}

enum NodeType {
    Object,
    Array,
    Value
}

enum DataType {
    NA,
    NullValue,
    StringType,
    IntType,
    DoubleType,
    BooleanType
}

enum NodeRepresentation {
    Row,
    HiddenRow,
    Column,
    HiddenColumn,
    Table,
    HiddenTable,
    EmbeddedTable
}

class NodeValues {
    private final List<Object> values = new ArrayList<>();

    static String listToString(final List<?> list, final String pfx, final String sep, final String sfx) {
        if (list==null || list.size()==0) return "";
        final var sb = new StringBuilder().append(pfx);
        for (final var m : list) sb.append(m.toString()).append(sep);
        sb.setLength(sb.length()-sep.length());
        sb.append(sfx);
        return sb.toString().trim();
    }

    void add(final Object value) { values.add(value); }
    boolean isNonEmpty() { return !values.isEmpty(); }
    Object first() { return values.get(0); }
    Stream<Object> stream() { return values.stream(); }
    List<Object> all() { return values; }
    void addAll(final List<Object> other) { values.addAll(other); }
    @Override public String toString() { return listToString(values, "", "\n", ""); }
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