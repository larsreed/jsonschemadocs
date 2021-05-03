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

    @Override public String toString() { return this.qName + (this.required? "*" : ""); }
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
    @Override public String toString() { return this.name + "(" + childrenToString() + ")"; }

    protected String childrenToString() {
        final var sb = new StringBuilder();
        sb.append(this.props);
        this.childList().forEach(c -> sb.append(c.toString()).append(","));
        if (!this.childList().isEmpty()) sb.setLength(sb.length()-1); // remove last comma
        return sb.toString();
    }
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
        if (orgNode instanceof final JsonKeyValue node) {
            if (JsonDocNames.PROP_KEYWORDS.contains(node.key) ||
                    node.key.startsWith(JsonDocNames.XIF_PREFIX) ||
                    node.key.startsWith(JsonDocNames.XIFNOT_PREFIX))
                addProp(node.key, node.value);
            else if (node.key.startsWith(JsonDocNames.XDOC_PREFIX)) {
                // Should also have supported array types, but that needs a rework of the props class...
                addProp(removePrefix(node.key, JsonDocNames.XDOC_PREFIX),  node.value);
            }
            else super.addChild(node);
            return;
        }
        super.addChild(orgNode);
    }

}

/** The (unnamed) top node in JSON Schema. */
class JsonTopNode extends JsonSchemaObject {

    JsonTopNode() { super("", "", 0); }
    @Override boolean visitThis(final JsonNodeVisitor visitor) { return visitor.topNode(this); }
    @Override void visitLeave(final JsonNodeVisitor visitor) { visitor.topNodeLeave(this); }

    @Override
    void addChild(final JsonBasicNode orgNode) {
        if (orgNode instanceof final JsonKeyValue node) { // Creates dep. cycle within this file, we can live with that
            if (JsonDocNames.PROP_KEYWORDS.contains(node.key)) addProp(node.key, node.value);
            else super.addChild(orgNode);
            return;
        }
        super.addChild(orgNode);
    }
}


/** An array (list) of entries. */
class JsonArray extends JsonObject {
    JsonArray(final String name, final String qName, final int tokenDepth) { super(name, qName, tokenDepth); }
    @Override boolean visitThis(final JsonNodeVisitor visitor) { return visitor.array(this); }
    @Override void visitLeave(final JsonNodeVisitor visitor) { visitor.arrayLeave(this); }
    @Override public String toString() { return "[" + childrenToString() + "]"; }
}

/** A value in an array. */
class JsonValue extends JsonBasicNode {
    final String value;

    JsonValue(final String name, final String value, final String qName, final int tokenDepth) {
        super(name, qName, tokenDepth);
        this.value = value;
    }

    @Override void visit(final JsonNodeVisitor visitor) { visitor.value(this); }
    @Override public String toString() { return this.value; }
}

/** A named attribute in an object. */
class JsonKeyValue extends JsonValue {
    final String key;

    JsonKeyValue(final String key, final String value, final String qName, final int tokenDepth) {
        super(key, value, qName, tokenDepth);
        this.key = key;
    }

    @Override void visit(final JsonNodeVisitor visitor) { visitor.keyValue(this); }
    @Override public String toString() { return this.key + "=" + this.value; }
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