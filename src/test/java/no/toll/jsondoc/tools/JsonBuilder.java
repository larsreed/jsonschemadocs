package no.toll.jsondoc.tools;

import java.util.Arrays;

@SuppressWarnings("unused")
public class JsonBuilder {
    private final StringBuilder buffer = new StringBuilder();
    private int currentLevel = 1;

    public JsonBuilder() { buffer.append("{"); }
    private StringBuilder indent() { return buffer.append(" ".repeat(currentLevel * 2)); }
    private void newLine() { buffer.append((buffer.toString().endsWith("{")? "\n" : ",\n")); }
    public JsonBuilder v(final String key, final String s) { return addRaw(key, '"' + s + '"'); }
    public JsonBuilder vo(final String key, final String s) { return addRaw(key, s); }
    public JsonBuilder v(final String key, final long l) { return addRaw(key, "" + l); }
    public JsonBuilder v(final String key, final double d) { return addRaw(key, "" + d); }
    public JsonBuilder v(final String key, final boolean b) { return addRaw(key, "" + b); }
    public JsonBuilder object(final String key) { return addRaw(key, "{", 1); }
    public JsonBuilder properties() { return object("properties"); }
    public JsonBuilder endProperties() { return endObject(); }

    private JsonBuilder addRaw(final String key, final String s) {
        newLine();
        indent().append('"').append(key).append('"').append(": ").append(s);
        return this;
    }

    @SuppressWarnings("SameParameterValue")
    private JsonBuilder addRaw(final String key, final String s, final int delta) {
        final var v = addRaw(key, s);
        currentLevel += delta;
        return v;
    }

    public JsonBuilder array(final String key) {
        addRaw(key, "[", 1);
        return this;
    }

    public JsonBuilder array(final String key, final String... strings) {
        array(key);
        for (final var s : strings) indent().append('"').append(s).append('"').append(",\n");
        if (strings.length>0) buffer.setLength(buffer.length()-2);
        return this;
    }

    public JsonBuilder array(final String key, final int... ints) {
        array(key);
        for (final var i : ints) indent().append(i).append(",\n");
        if (ints.length>0) buffer.setLength(buffer.length()-2);
        return this;
    }

    public JsonBuilder endArray() {
        currentLevel--;
        buffer.append("\n");
        indent().append("]");
        return this;
    }

    public JsonBuilder endObject() {
        currentLevel--;
        buffer.append("\n");
        indent().append("}");
        return this;
    }

    public JsonBuilder required(final String... keys) {
        array("required");
        Arrays.stream(keys).forEach(k -> {
            buffer.append('\n');
            indent().append('"').append(k).append('"').append(",");
        });
        buffer.setLength(buffer.length()-1);
        return endArray();
    }

    @Override
    public String toString() {
        buffer.append("\n}");
        return buffer.toString();
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