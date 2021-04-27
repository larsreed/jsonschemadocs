package net.kalars.jsondoc;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;


/** A class for handling the defined JSON Schema properties for a type. */
class JsonProps {

    private final Map<String, String> props= new LinkedHashMap<>();

    static String unquote(final String s) {
        return s.replaceAll("^\"", "").replaceAll("\"$", "");
    }

    void add(final String key, final String value) {
        this.props.merge(key, unquote(value),  (org, add) -> org.contains(add)? org : org + "\n" + add);
    }

    @SuppressWarnings("SameParameterValue")
    String getProp(final String key) { return this.props.get(key); }

    void iterateOver(final BiConsumer<? super String, ? super String> method) {
        // Redefine the type description before iterating over it
        final var copy = new JsonProps();
        copy.props.putAll(this.props);
        copy.defineType();
        copy.removeIfs();
        copy.props.forEach(method);
    }

    Map<String, String> propCopy() {return new LinkedHashMap<>(this.props);  }

    private void defineType() {
        format();
        minMax();
        minMaxLen();
        minMaxItems();
        uniqueItems();
        pattern();
        enums();
    }

    private void removeIfs() {
        final var keys = new HashSet<>(this.props.keySet());
        keys.stream()
                .filter(t -> t.startsWith(JsonDocNames.XIF_PREFIX) || t.startsWith(JsonDocNames.XIFNOT_PREFIX))
                .forEach(this.props::remove);
    }

    private Optional<String> getOpt(final String key) {
        final var found = this.props.get(key);
        if (found!=null)  return Optional.of(found);
        return Optional.empty();
    }

    private Optional<String> extract(final String key) {
        final var found = getOpt(key);
        if (found.isPresent()) this.props.remove(key);
        return found;
    }


    private void mergeType(final String value) {
        this.props.merge(JsonDocNames.TYPE, value,  (org, add) -> org.contains(add)? org : org + "\t" + add);
    }

    private void minMax() {
        var min = extract(JsonDocNames.MINIMUM);
        var max = extract(JsonDocNames.MAXIMUM);

        if (min.isEmpty()) {
            min = extract(JsonDocNames.EXCLUSIVE_MINIMUM);
            if (min.isPresent()) min = Optional.of("<" + min.get());
        }
        else if (max.isPresent() && min.get().equals(max.get())) { // Exact value
            min = Optional.of("[" + min.get() + "]");
            max = Optional.empty();
        }
        else min = Optional.of("[" + min.get());

        if (max.isEmpty()) {
            max = extract(JsonDocNames.EXCLUSIVE_MAXIMUM);
            if (max.isPresent()) max = Optional.of(max.get() + ">");
            else  if (min.isPresent()) max = Optional.of(" ...]");
        }
        else max = Optional.of(max.get() + "]");

        if (min.isPresent()) min = Optional.of(min.get() + ", ");
        else if (max.isPresent()) min = Optional.of("<0, ");
        else return;

        mergeType(min.orElse("") + max.orElse(""));
    }

    private void minMaxLen() {
        var min = extract(JsonDocNames.MIN_LENGTH);
        var max = extract(JsonDocNames.MAX_LENGTH);
        var exact = false;

        if (min.isPresent() && max.isPresent() && min.get().equals(max.get())) { // Exact value
            min = Optional.of("[" + min.get() + "]");
            max = Optional.empty();
            exact = true;
        }
        else if (min.isPresent()) min = Optional.of("[" + min.get());
        if (max.isPresent()) max = Optional.of(max.get() + "]");

        if (min.isPresent() && !exact) min = Optional.of(min.get() + "..");
        else if (max.isPresent()) min = Optional.of("<0..");
        else if (min.isEmpty()) return;

        mergeType(min.orElse("") + max.orElse(""));
    }

    String cardinality() {
        var min = getOpt(JsonDocNames.MIN_ITEMS);
        var max = getOpt(JsonDocNames.MAX_ITEMS);
        final var req = getOpt(JsonDocNames.REQUIRED);

        if (min.isPresent() && max.isPresent() && min.get().equals(max.get())) { // Exact value
            min = Optional.of("[" + min.get() + "]");
            max = Optional.empty();
        }
        else {
            if (min.isPresent()) {
                min = Optional.of("[" + min.get() + ", ");
                if (max.isEmpty()) max = Optional.of(" ...");
            }
            if (max.isPresent()) {
                max = Optional.of(max.get() + "]");
                if (req.isPresent()) min = Optional.of("[1, ");
                else min = Optional.of("[0, ");
            }
        }

        if (min.isPresent()) return min.orElse("") + max.orElse("");
        else if (req.isPresent()) return JsonDocNames.REQUIRED;
        else return "";
    }

    private void minMaxItems() {
        final var cardinality = cardinality();
        mergeType(cardinality);
        // remove these
        extract(JsonDocNames.MIN_ITEMS);
        extract(JsonDocNames.MAX_ITEMS);
        extract(JsonDocNames.REQUIRED);
    }

    private void uniqueItems() {
        final var unique = extract(JsonDocNames.UNIQUE_ITEMS);
        if (unique.isPresent()) mergeType(JsonDocNames.UNIQUE_ITEMS);
    }

    private void format() {
        final var format = extract(JsonDocNames.FORMAT);
        format.ifPresent(this::mergeType);
    }

    private void pattern() {
        final var pattern = extract(JsonDocNames.PATTERN);
        pattern.ifPresent(s -> mergeType(JsonDocNames.PATTERN + "=" + s));
    }

    private void enums() {
        final var pattern = extract(JsonDocNames.ENUM); //FIXME
        pattern.ifPresent(s -> mergeType("{" + s + "}"));
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