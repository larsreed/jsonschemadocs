package net.kalars.jsondoc;

import java.util.LinkedList;
import java.util.List;

// This file contains all the node types resulting from parsing


/** Common for all nodes -- depth in the parse tree, name, qualified name (parent.child.child...)
 *  and required/optional. */
abstract class JsonBasicNode {
    final String name;
    final int tokenDepth;
    final String qName;
    private boolean required = false;

    protected JsonBasicNode(final String name, final String qName, final int tokenDepth) {
        this.name = name;
        this.qName = qName;
        this.tokenDepth = tokenDepth;
    }

    void makeRequired() { this.required = true; }
    boolean isRequired() { return this.required; }
    abstract void visit(final JsonNodeVisitor visitor);

    @Override
    public String toString() {
        return "JsonBasicNode{ qName='" + this.qName + "', required=" + this.required + '}';
    }
}

/** Parent node for objects/arrays - has 0 or more children
 *  (the children are attributes/arrays/array entries/objects). */
abstract class JsonNode extends JsonBasicNode {
    final JsonProps props = new JsonProps();
    private final List<JsonBasicNode> children = new LinkedList<>();

    JsonNode(final String name, final String qName, final int tokenDepth) { super(name, qName, tokenDepth); }

    void addProp(final String key, final String value) { this.props.add(key, value); }
    void addChild(final JsonBasicNode node) { this.children.add(node); }
    @SuppressWarnings("UnusedReturnValue")
    boolean removeChild(final JsonBasicNode node) { return this.children.remove(node); }
    List<JsonBasicNode> childList() { return new LinkedList<>(this.children); }

    void visit(final JsonNodeVisitor visitor) {
        if (!visitThis(visitor)) return;
        this.children.forEach(c -> c.visit(visitor));
        visitLeave(visitor);
    }

    abstract boolean visitThis(final JsonNodeVisitor visitor);
    abstract void visitLeave(final JsonNodeVisitor visitor);

    boolean hasChildren() { return !this.children.isEmpty(); }
}

/** A standard JSON object. */
class JsonObject extends JsonNode {

    JsonObject(final String name, final String qName, final int tokenDepth) { super(name, qName, tokenDepth); }
    @Override boolean visitThis(final JsonNodeVisitor visitor) { return visitor.object(this); }
    @Override void visitLeave(final JsonNodeVisitor visitor) { visitor.objectLeave(this); }

}

/** An object in JsonSchema. */
class JsonSchemaObject extends JsonObject {

    JsonSchemaObject(final String name, final String qName, final int tokenDepth) {
        super(name, qName, tokenDepth);
    }

    @SuppressWarnings("SameParameterValue")
    private static String removePrefix(final String s, final String pfx) { return s.replaceAll("^" + pfx, ""); }

    @Override
    void addChild(final JsonBasicNode orgNode) {
        if (orgNode instanceof JsonKeyValue) {
            final JsonKeyValue node = (JsonKeyValue) orgNode;
            if (JsonDocNames.PROP_KEYWORDS.contains(node.key)) addProp(node.key, node.value);
            else if (node.key.startsWith(JsonDocNames.XDOC_PREFIX)) {
                addProp(removePrefix(node.key, JsonDocNames.XDOC_PREFIX), node.value);
            }
            else super.addChild(orgNode);
            return;
        }
        super.addChild(orgNode);
    }

    @Override
    public String toString() {
        final var sb= new StringBuilder().append("JsonDocNode{ qName='").append(this.qName);
        this.props.forEach((k, v) -> sb.append(" ").append(k).append("=").append(v));
        return sb.toString();
    }
}

/** The (unnamed) top node in JSON Schema. */
class JsonTopNode extends JsonSchemaObject {

    JsonTopNode() { super("", "", 0); }

    @Override
    void addChild(final JsonBasicNode orgNode) {
        if (orgNode instanceof JsonKeyValue) { // Creates dependency cycle within this file, we can live with that
            final JsonKeyValue node = (JsonKeyValue) orgNode;
            if (JsonDocNames.PROP_KEYWORDS.contains(node.key)) addProp(node.key, node.value);
            else super.addChild(orgNode);
            return;
        }
        super.addChild(orgNode);
    }

    @Override boolean visitThis(final JsonNodeVisitor visitor) { return visitor.topNode(this); }
    @Override void visitLeave(final JsonNodeVisitor visitor) { visitor.topNodeLeave(this); }
    @Override
    public String toString() {
        return super.toString().replaceAll("^JsonDocNode", "JsonTopNode");
    }
}


/** An array containing simple entries. */
class JsonArray extends JsonNode {
    JsonArray(final String name, final String qName, final int tokenDepth) { super(name, qName, tokenDepth); }

    @Override
    void addChild(final JsonBasicNode node) {
        final JsonValue jVal = (JsonValue) node;
        final var nodeVal = jVal.value;
        final var unq = nodeVal.replaceAll("\"", "");
        final var fullName = jVal.qName.replaceAll("[.][^.]+$", "") + "." + unq;
        final var newNode = new JsonValue(unq, nodeVal, fullName, jVal.tokenDepth);
        super.addChild(newNode);
    }

    @Override boolean visitThis(final JsonNodeVisitor visitor) {
        return visitor.array(this);
    }
    @Override void visitLeave(final JsonNodeVisitor visitor) {
        visitor.arrayLeave(this);
    }

    @Override
    public String toString() {
        // Lists children as {a, b, c, d } (without quotes)
        final var sb = new StringBuilder();
        sb.append("{ ");
        childList().forEach(n -> sb.append(n.name.replaceAll("(^\")|(\"$)", "")).append(", "));
        sb.setLength(sb.length()-2);
        sb.append(" }");
        return sb.toString();
    }
}

/** A value in an array. */
class JsonValue extends JsonBasicNode {
    final String value;

    JsonValue(final String name, final String value, final String qName, final int tokenDepth) {
        super(name, qName, tokenDepth);
        this.value = value;
    }

    @Override void visit(final JsonNodeVisitor visitor) { visitor.value(this); }
    @Override public String toString() { return "JsonValue{ qName='" + this.qName + "', value='" + this.value + "'}"; }
}

/** A named attribute in an object. */
class JsonKeyValue extends JsonValue {
    final String key;

    JsonKeyValue(final String key, final String value, final String qName, final int tokenDepth) {
        super(key, value, qName, tokenDepth);
        this.key = key;
    }

    @Override void visit(final JsonNodeVisitor visitor) { visitor.keyValue(this); }
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