package net.kalars.jsondoc;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Node {
    private static final Random random = new Random();
    final String name;
    private boolean visible = true;
    private boolean hideChildren = false;
    private NodeType nodeType;
    final DataType dataType;
    final List<Node> children = new LinkedList<>();
    private final Node parent;
    final List<Object> values = new ArrayList<>();
    private final Context context;
    private boolean required = false;
    NodeRepresentation representation;
    String generatedSample = "";

    Node(final String name, final NodeType nodeType, final DataType dataType, final Object value, final Node parent,
         final Context context) {
        this.name = name;
        this.nodeType = nodeType;
        this.dataType = dataType;
        this.parent = parent;
        if (value!=null) this.values.add(value);
        this.context = context;
        if (parent!=null) parent.add(this);
        setDefaultRepresentation();
    }

    Node(final String name, final NodeType nodeType, final Node parent, final Context context) {
        this(name, nodeType, DataType.NA, null, parent, context);
    }

    boolean isVisible() { return this.visible; }
    boolean isRequired() { return this.required; }
    boolean hasHiddenChildren() { return this.hideChildren; }
    NodeType nodeType() { return this.nodeType; }

    // TODO test: default rep for Object, Value , Array
    private void setDefaultRepresentation() {
        switch (this.nodeType) {
            case Object -> this.representation = NodeRepresentation.Table;
            case Array, Value -> this.representation = NodeRepresentation.Row;
        }
    }

    /** Track parent chain. */
    private List<Node> heritage() {
        if (this.parent==null) return List.of(this);
        final var upToThis = this.parent.heritage();
        upToThis.add(this);
        return upToThis;
    }

    private String qName() {
        final var sb = new StringBuilder();
        heritage().forEach(n-> sb.append(n.name));
        return sb.toString();
    }

    private String displayName() {
        return ""; // TODO
    }

    private int level() {
        return heritage().size();
    }

    private String extId() {
        final var names = heritage().stream()
                .map(n -> n.name.replaceAll("[^a-zA-Z0-9-]", "_"))
                .collect(Collectors.toList());
        return listToString(names, "", "-", "");
    }

    /** Add a child node. */
    private Node add(final Node node) {
        if (!(this.nodeType.equals(NodeType.Object) || this.nodeType.equals(NodeType.Array)))
            Logger.error("Adding to simple type", this.nodeType);
        this.children.add(node);
        return this;
    }

    // TODO implicit test: required & properties removed from tree
    /** Remove a child node. */
    private boolean remove(final Node node) { return this.children.remove(node); }

    /** Find a named child under same parent. */
    private Object getSiblingValue(final String name, final Object defVal) {
        if (this.parent==null) return defVal;
        final var child = this.parent.getChild(name);
        if (child==null) return defVal;
        final var childVal = child.values;
        return (childVal==null || childVal.isEmpty())? defVal : childVal.get(0);
    }

    /** Find a named child under this node. */
    private Node getChild(final String name) {
        for (final var node : this.children) if (name.equals(node.name)) return node;
        return null;
    }

    /** Remove a named child under this node. */
    private void removeChild(final String name) {
        final var node = getChild(name);
        if (node!=null) this.children.remove(node);
    }


    // TODO test: PROPERTIES are merged into parents and removed
    // TODO test: REQUIRED are merged into required-attributes and removed
    // TODO test: XIF-/XIFNOT- => hidden parent, hidden subtree
    // TODO test: XIF-/XIFNOT- => no Context => no effect
    // TODO test: x- ... => column
    // TODO test: excludeColumn
    /** Called when this instance is complete from the parser. */
    Node finalized() { // TODO break this up...
        if (this.nodeType.equals(NodeType.Object) && !this.context.isSchemaMode()) {
            // Attach "properties" directly to parent
            if (JsonDocNames.PROPERTIES.equals(this.name)) return removeThisNode();
        }
        else if (this.nodeType.equals(NodeType.Array) && !this.context.isSchemaMode()) {
            // Convert "required" to attributes
            if (JsonDocNames.REQUIRED.equals(this.name)) return convertRequired();
        }
        else if ( this.nodeType.equals(NodeType.Value)) {
            // Handle variants
            if (this.name.startsWith(JsonDocNames.XIFNOT_PREFIX)) {
                final var key = removePrefix(this.name, JsonDocNames.XIFNOT_PREFIX);
                final var values = this.values.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(","));
                final var matches = this.context.anyMatch(key, values);
                if (matches) {
                    this.visible = false;
                    this.parent.visible = false;
                    this.parent.hideChildren = true;
                }
            }
            else if (this.name.startsWith(JsonDocNames.XIF_PREFIX)) {
                final var key = removePrefix(this.name, JsonDocNames.XIF_PREFIX);
                final var values = this.values.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(","));
                final var matches = this.context.anyMatch(key, values);
                if (!matches) {
                    this.visible = false;
                    this.parent.visible = false;
                    this.parent.hideChildren = true;
                }
            }
            else if (this.name.startsWith(JsonDocNames.XDOC_PREFIX))  this.representation = NodeRepresentation.Column;
            if ( this.context.isExcluded(this.name))  this.visible = false;
        }
        if (hasChildren() && !this.context.isSchemaMode()) { // Create sample values for each child
            this.children.stream().filter(n -> n.nodeType.equals(NodeType.Value)).forEach(Node::defaultSample);
        }
        if (hasChildren() && !this.context.isSchemaMode()) { // Define cardinality
            final var cardinal = cardinality();
            if (!cardinal.isEmpty()) { // TODO is the value
                final var node = new Node(JsonDocNames.CARDINALITY, NodeType.Value, DataType.StringType, cardinal,
                        this, this.context);
                node.visible = false;
            }
            this.children.stream().filter(n -> n.nodeType.equals(NodeType.Value)).forEach(Node::defaultSample);
        }
        if (hasChildren() && !this.context.isSchemaMode()) { // Transform known properties to columns
            convertKnownProperties();
        }
        if (hasChildren()) { // Evaluate this after evaluating possible child attributes
            if (this.hideChildren) this.children.clear();
        }
        if (!this.visible) { // Fix representation if hidden
            switch (this.representation) {
                case Row ->  this.representation = NodeRepresentation.HiddenRow;
                case Column -> this.representation = NodeRepresentation.HiddenColumn;
                case Table, EmbeddedTable -> this.representation = NodeRepresentation.HiddenTable;
            }
        }
        return this;
    }

    private boolean hasChildren() {
        return (this.nodeType.equals(NodeType.Object) || this.nodeType.equals(NodeType.Array))
                && this.children.size()>0;
    }

    String defaultSample() {
        switch (this.dataType) {
            case StringType -> {
                final var minLen = Integer.parseInt(getSiblingValue(JsonDocNames.MIN_LENGTH, "0").toString());
                final var maxLen = Integer.parseInt(getSiblingValue(JsonDocNames.MAX_LENGTH, "20").toString());
                final var midLen = (minLen + maxLen) / 2;
                String tst = "ABCD0123EFGH4567IJKL89MNOPQRSTUVWXYZ";
                final var offs = random.nextInt(tst.length());
                while (tst.length() < (midLen+offs+1)) tst+=tst;
                this.generatedSample = "\"" + tst.substring(offs, midLen+offs) + "\"";
            }
            case IntType -> { this.generatedSample = sampleInt(); }
            case DoubleType -> { this.generatedSample = sampleInt() + ".0"; }
            case BooleanType -> { this.generatedSample = "true"; }
            case NullValue -> { this.generatedSample = "null"; }
        }
        return "";
    }

    private String sampleInt() {
        final var mult = getSiblingValue(JsonDocNames.MULTIPLE_OF, null);
        if (mult!=null) return ""+ mult;
        final var min = Integer.parseInt(getSiblingValue(JsonDocNames.MINIMUM, "0").toString());
        final var max = Integer.parseInt(getSiblingValue(JsonDocNames.MAXIMUM, "1024").toString());
        final var rnd = min + random.nextInt((max-min+1));
        return "" + rnd;
    }

    private Node removeThisNode() {
        if (this.parent==null) throw new RuntimeException("Wrong structure, cannot remove child without parent");
        this.children.forEach(this.parent::add);
        this.parent.remove(this);
        return this.parent;
    }

    private Node convertRequired() {
        final var names = this.children.stream().map(n -> n.name).collect(Collectors.toList());
        this.parent.children.forEach(n -> {
            if (names.contains(n.name)) n.required = true;
        });
        this.parent.remove(this);
        return this.parent;
    }

    // TODO test: conversion of format, several variants
    private void convertKnownProperties() {
        convertFormat();
        minMax();
        minMaxLen();
        minMaxItems();
        enums();
        miscProps();
        idToDesc();
    }

    private static String listToString(final List<?> list, final String pfx, final String sep, final String sfx) {
        if (list==null || list.size()==0) return "";
        final var sb = new StringBuilder().append(pfx);
        for (final var m : list) sb.append(m.toString()).append(sep);
        sb.setLength(sb.length()-sep.length());
        sb.append(pfx);
        return sb.toString().trim();
    }

    static String removePrefix(final String s, final String pfx) { return s.replaceAll("^" + pfx, ""); }

    private List<Object> extract(final String key) {
        final var other = getChild(key);
        if (other!=null) {
            other.visible = false;
            other.representation = NodeRepresentation.HiddenColumn;
            return other.values;
        }
        return List.of();
    }

    private String extractString(final String s) { return listToString(extract(s), "", " ", ""); }

    private boolean extractBoolean(final String key) {
        final var found = extract(key);
        if (!found.isEmpty()) {
            for (final var o : found) if (o.toString().equalsIgnoreCase("TRUE")) return true;
        }
        return false;
    }

    private void moveProperties(final String key) {
        final var other = extract(key);
        if (other.size()>0)  addToType(other);
    }

    private void addToType(final List<Object> other) {
        var type = getChild(JsonDocNames.TYPE);
        if (type==null)  type =
                new Node(JsonDocNames.TYPE, NodeType.Value, DataType.StringType, null, this, this.context);
        type.values.addAll(other);
    }

    private void addToType(final String s) { if (s!=null && !s.isEmpty()) addToType(List.of(s)); }

    private void convertFormat() { moveProperties(JsonDocNames.FORMAT); }

    private void minMax() {
        var min = extractString(JsonDocNames.MINIMUM);
        var max = extractString(JsonDocNames.MAXIMUM);

        if (min.isEmpty()) {
            min = extractString(JsonDocNames.EXCLUSIVE_MINIMUM);
            if (!min.isEmpty()) min = "<" + min;
        }
        else if (!max.isEmpty() && min.equals(max)) { // Exact value, min=max
            min = "[" + min + "]";
            max = "";
        }
        else min = "[" + min;

        if (max.isEmpty()) {
            max = extractString(JsonDocNames.EXCLUSIVE_MAXIMUM);
            if (!max.isEmpty()) max = max + ">";
            else  if (!min.isEmpty()) max = " ...]";
        }
        else max =max + "]";

        if (!min.isEmpty()) min = min + ", ";
        else if (!max.isEmpty()) min = "[0, ";
        else return;

        addToType(min + max);
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
        else if (!min.isEmpty()) min = "[" + min;
        if (!max.isEmpty()) max = max + "]";

        if (!min.isEmpty() && !exact) min = min + "..";
        else if (!max.isEmpty()) min = "[0..";
        else if (min.isEmpty()) return;

        addToType(min + max);
    }

    String cardinality() {
        final var minNode = getChild(JsonDocNames.MIN_ITEMS);
        final var maxNode = getChild(JsonDocNames.MAX_ITEMS);
        var min = (minNode==null)? "" : listToString(minNode.values, "", " ", "");
        var max = (maxNode==null)? "" : listToString(maxNode.values, "", " ", "");

        if (!min.isEmpty() && !max.isEmpty() && min.equals(max)) { // Exact value
            min = "[" + min + "]";
            max = "";
        }
        else {
            if (!min.isEmpty()) {
                min = "[" + min + ", ";
                if (max.isEmpty()) max = " ...";
            }
            if (!max.isEmpty()) {
                max = max + "]";
                if (this.required) min = "[1, ";
                else min = "[0, ";
            }
        }

        if (!min.isEmpty()) return min + max;
        else if (this.required) return JsonDocNames.REQUIRED;
        else return "";
    }

    private void minMaxItems() {
        final var cardinality = cardinality();
        addToType(cardinality);
        // hide these
        extract(JsonDocNames.MIN_ITEMS);
        extract(JsonDocNames.MAX_ITEMS);
        extract(JsonDocNames.REQUIRED);
    }

    private void enums() {
        final var cvt = listToString(extract(JsonDocNames.ENUM), "{", ", ", "}");
        if (!cvt.isEmpty()) addToType(cvt);
    }

    private void miscProps() {
        if (extractBoolean(JsonDocNames.UNIQUE_ITEMS)) addToType(JsonDocNames.UNIQUE_ITEMS);
        if (extractBoolean(JsonDocNames.WRITE_ONLY)) addToType(JsonDocNames.WRITE_ONLY);
        if (extractBoolean(JsonDocNames.READ_ONLY)) addToType(JsonDocNames.READ_ONLY);

        extract(JsonDocNames.PATTERN).forEach(s -> addToType(JsonDocNames.PATTERN + "=" + s));
        extract(JsonDocNames.CONST).forEach(s -> addToType("=" + s));
        extract(JsonDocNames.MULTIPLE_OF).forEach(s -> addToType(JsonDocNames.MULTIPLE_OF + " " + s));
        extract(JsonDocNames.DEFAULT).forEach(s -> addToType(JsonDocNames.DEFAULT + "=" + s));
        extract(JsonDocNames.DEPRECATED).forEach(s -> addToType(JsonDocNames.DEPRECATED.toUpperCase() + "!"));
        extract(JsonDocNames.CONTENT_ENCODING).forEach(s -> addToType(JsonDocNames.CONTENT_ENCODING + "=" + s));
        extract(JsonDocNames.CONTENT_MEDIA_TYPE).forEach(s -> addToType(JsonDocNames.CONTENT_MEDIA_TYPE + "=" + s));
        extract(JsonDocNames.CONTENT_SCHEMA).forEach(s -> addToType(JsonDocNames.CONTENT_SCHEMA + "=" + s));
    }

    private void idToDesc() {
        final var idf = listToString(extract(JsonDocNames.ID), "", ", ", "");
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