package net.kalars.jsondoc;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** Builds internal data structure from Jackson parser events. */
class JsonDocParser {

    private final JsonFactory jFactory = new JsonFactory();
    private final Context context;
    private String nextName;
    private Node topNode;
    private final Deque<Node> parseStack = new LinkedList<>();

    public JsonDocParser(final Context context) { this.context = context; }

    private Node currentNode() {  return (this.parseStack.isEmpty())? null : this.parseStack.peek(); }

    Node parseFile(final String fileName) {
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

    Node parseString(final String data) {
        try (final JsonParser jParser = this.jFactory.createParser(data)) {
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

    private Optional<Node> handleToken(final JsonToken token, final JsonParser jParser) throws IOException {
        switch (token) {
            case FIELD_NAME -> fieldName(jParser);
            case START_OBJECT -> startObject();
            case END_OBJECT -> {
                final Optional<Node> res = endObject();
                if (res.isPresent()) return res;
            }
            case START_ARRAY -> startArray();
            case END_ARRAY -> endArray();
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

    private void fieldName(final JsonParser jParser) throws IOException { this.nextName = jParser.getCurrentName(); }

    private void startObject() {
        final Node node = new Node(this.nextName, NodeType.Object, currentNode(), this.context);
        if (this.topNode==null) this.topNode = node;
        this.parseStack.push(node);
    }

    private Optional<Node> endObject() {
        final var res = this.parseStack.pop().finalized();
        if (this.parseStack.isEmpty()) return Optional.of(res);
        return Optional.empty();
    }

    private void startArray() {
        final Node node = new Node(this.nextName, NodeType.Array, currentNode(), this.context);
        this.parseStack.push(node);
    }

    private void endArray() { this.parseStack.pop().finalized(); }

    private void addKeyVal(final Object thisVal, final DataType dataType) {
        new Node(this.nextName, NodeType.Value, dataType, thisVal, currentNode(), this.context).finalized();
    }

    protected void valueNull() { addKeyVal(null, DataType.NullValue); }
    protected void valueFalse() { addKeyVal(Boolean.FALSE, DataType.BooleanType); }
    protected void valueTrue() { addKeyVal(Boolean.TRUE, DataType.BooleanType); }

    protected void valueNumberFloat(final JsonParser jParser) throws IOException {
        addKeyVal(jParser.getDoubleValue(), DataType.DoubleType);
    }

    protected void valueNumberInt(final JsonParser jParser) throws IOException {
        addKeyVal(jParser.getLongValue(), DataType.IntType);
    }

    protected void valueString(final JsonParser jParser) throws IOException {
        addKeyVal(jParser.getValueAsString(), DataType.StringType);
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