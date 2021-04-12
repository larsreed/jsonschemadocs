package net.kalars.jsondoc;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// This file contains all the "parsers" (actual parsing done by imported code)

/** A general parser class -- translating output for general JSON. */
class JsonGenParser {

    protected final JsonFactory jFactory = new JsonFactory();
    protected final List<JsonNode> stackBackingList = new LinkedList<>();
    protected final Deque<JsonNode> parseStack = (LinkedList<JsonNode>) this.stackBackingList;
    protected final Map<String, JsonBasicNode> qNameMap = new LinkedHashMap<>();

    protected enum ArrayMode {
        Std,
        ReadingRequiredArray,
        ReadingRegularArray
    }

    protected int tokenDepth = 0;
    protected int arrayDepth = 0;
    protected String nextName = "";
    protected ArrayMode arrayMode = ArrayMode.Std;

    protected JsonNode currentNode() { return this.parseStack.peek(); }

    protected String qualifiedName() {
        final var delimiter = ".";
        final var names = this.stackBackingList.stream()
                .map(node -> node.name)
                .collect(Collectors.toList());
        Collections.reverse(names); // Why is this not a stream/list function!?!
        names.add(this.nextName);
        return String.join(delimiter, names)
                .replaceAll("^[.]", "");
    }

    JsonNode parseFile(final String fileName) {
        try (final JsonParser jParser = this.jFactory.createParser(new File(fileName))) {
            do {
                final JsonToken token = jParser.nextToken();
                final var optNode = handleToken(token, jParser);
                if (optNode.isPresent()) return optNode.get();
            } while (true);
        }
        catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<JsonNode> handleToken(final JsonToken token, final JsonParser jParser) throws IOException {
        switch (token) {
            case START_OBJECT -> startObject();
            case END_OBJECT -> {
                final Optional<JsonNode> res = endObject();
                if (res.isPresent()) return res;
            }
            case START_ARRAY -> startArray();
            case END_ARRAY -> endArray();
            case FIELD_NAME -> fieldName(jParser);
            case VALUE_STRING -> valueString(jParser);
            case VALUE_NUMBER_INT -> valueNumberInt(jParser);
            case VALUE_NUMBER_FLOAT -> valueNumberFloat(jParser);
            case VALUE_TRUE -> valueTrue();
            case VALUE_FALSE -> valueFalse();
            case VALUE_NULL -> valueNull();
            default -> throw new RuntimeException("Didn't recognize " + token);
        }
        return Optional.empty();
    }

    protected void fieldName(final JsonParser jParser) throws IOException { this.nextName = jParser.getCurrentName(); }

    protected void startObject() {
        final JsonObject node = createObject();
        this.parseStack.push(node);
        this.qNameMap.put(node.qName, node);
        this.tokenDepth++;
    }

    protected JsonObject createObject() {
        final JsonObject node = new JsonObject(this.nextName, qualifiedName(), this.tokenDepth);
        if (currentNode()!=null) currentNode().addChild(node);
        return node;
    }

    protected Optional<JsonNode> endObject() {
        this.tokenDepth--;
        final var res = this.parseStack.pop();
        if (this.tokenDepth == 0) return Optional.of(res);
        return Optional.empty();
    }

    protected void startArray() {
        final JsonNode node = new JsonArray(this.nextName, qualifiedName(), this.tokenDepth);
        this.arrayMode = ArrayMode.ReadingRegularArray;
        this.arrayDepth++;
        currentNode().addChild(node);
        this.parseStack.push(node);
        this.qNameMap.put(node.qName, node);
    }

    protected void endArray() {
        this.arrayDepth--;
        if (this.arrayDepth == 0 ) this.arrayMode = ArrayMode.Std;
        this.parseStack.pop();
    }

    protected void addKeyVal(final String vs) {
        final JsonValue node;
        if (this.arrayMode == ArrayMode.ReadingRegularArray) {
            final var unq = vs.replaceAll("\"", "");
            final var qn = qualifiedName().replaceAll("[.][^.]+$", "") + "." + unq;
            node = new JsonValue(unq, vs, qn, this.tokenDepth);
        }
        else node = new JsonKeyValue(this.nextName, vs, qualifiedName(), this.tokenDepth);
        currentNode().addChild(node);
        this.qNameMap.put(node.qName, node);
    }

    protected void valueNull() { addKeyVal("null"); }
    protected void valueFalse() { addKeyVal("false"); }
    protected void valueTrue() { addKeyVal("true"); }

    protected void valueNumberFloat(final JsonParser jParser) throws IOException {
        addKeyVal(String.format("%f", jParser.getDoubleValue()));
    }

    protected void valueNumberInt(final JsonParser jParser) throws IOException {
        addKeyVal(String.format("%d", jParser.getLongValue()));
    }

    protected void valueString(final JsonParser jParser) throws IOException {
        addKeyVal(String.format("\"%s\"", jParser.getValueAsString()));
    }
}

/** A more specialised parser for JSON Schema. */
class JsonSchemaParser extends JsonGenParser {
    private static final String REQUIRED = "required";
    private static final String PROPERTIES = "properties";

    /** Introduce concept of JsonTopNode. */
    @Override
    protected JsonObject createObject() {
        final JsonObject node;
        if (this.tokenDepth ==0) return new JsonTopNode();
        node = new JsonSchemaObject(this.nextName, qualifiedName(), this.tokenDepth);
        currentNode().addChild(node);
        return node;
    }

    @Override
    protected Optional<JsonNode> endObject() {
        if (PROPERTIES.equals(currentNode().name)) removeThisNode();
        return super.endObject();
    }

    protected void removeThisNode() {
        final var thisNode = this.parseStack.pop(); // Keep this node
        final var prevNode = this.parseStack.peek(); // and access the previous
        if (prevNode != null) {
            thisNode.childList().forEach(prevNode::addChild); // move children
            prevNode.removeChild(thisNode);
        }
        this.parseStack.push(thisNode); // put back temporarily (soon to be popped)
    }

    @Override
    protected void startArray() {
        if (REQUIRED.equals(this.nextName)) this.arrayMode = ArrayMode.ReadingRequiredArray;
        else super.startArray();
    }

    @Override
    protected void endArray() {
        if (this.arrayMode == ArrayMode.ReadingRequiredArray) this.arrayMode = ArrayMode.Std;
        else super.endArray();
    }

    @Override
    protected void addKeyVal(final String vs) {
        if (this.arrayMode == ArrayMode.ReadingRequiredArray) {
            final var qName = (currentNode().qName + "." + PROPERTIES + "." + vs)
                    .replaceAll("\"", "")
                    .replaceFirst("^[.]", "");
            final var fieldNode = this.qNameMap.get(qName);
            if (fieldNode != null) fieldNode.makeRequired();
        }
        else super.addKeyVal(vs);
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