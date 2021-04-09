package net.kalars.jsondoc;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;


class JsonProps {

    static final List<String> KEYWORDS = Arrays.asList(
            JsonDocNames.DESCRIPTION, JsonDocNames.TYPE,
            JsonDocNames.MINIMUM, JsonDocNames.EXCLUSIVE_MINIMUM,
            JsonDocNames.MAXIMUM, JsonDocNames.EXCLUSIVE_MAXIMUM,
            JsonDocNames.MIN_LENGTH, JsonDocNames.MAX_LENGTH,
            JsonDocNames.MIN_ITEMS, JsonDocNames.MAX_ITEMS, JsonDocNames.UNIQUE_ITEMS,
            JsonDocNames.FORMAT, JsonDocNames.PATTERN);

    private final Map<String, String> props= new LinkedHashMap<>();

    static String unquote(final String s) {
        return s.replaceAll("^\"", "").replaceAll("\"$", "");
    }

    void add(final String key, final String value) { this.props.put(key, unquote(value));  }

    void forEach(final BiConsumer<? super String, ? super String> method) {
        // Redefine the type description before iterating over it
        final var copy = new JsonProps();
        copy.props.putAll(this.props);
        copy.defineType();
        copy.props.forEach(method);
    }

    Map<String, String> copyProps() {return new LinkedHashMap<String, String>(this.props);  }

    private void defineType() {
        format();
        minMax();
        minMaxLen();
        minMaxItems();
        uniqueItems();
        pattern();
    }

    private Optional<String> extract(final String key) {
        final var found = this.props.get(key);
        if (found!=null) {
            this.props.remove(key);
            return Optional.of(found);
        }
        return Optional.empty();
    }

    private void mergeType(final String value) {
        this.props.merge(JsonDocNames.TYPE, value,  (org, add) -> org.contains(add)? org : org + "  " + add);
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

    private void minMaxItems() {
        var min = extract(JsonDocNames.MIN_ITEMS);
        var max = extract(JsonDocNames.MAX_ITEMS);
        final var req = extract(JsonDocNames.REQUIRED);

        if (min.isPresent() && max.isPresent() && min.get().equals(max.get())) { // Exact value
            min = Optional.of("[" + min.get() + "]");
            max = Optional.empty();
        }
        else {
            if (min.isPresent()) min = Optional.of("[" + min.get() + ", ");
            if (max.isPresent()) {
                max = Optional.of(max.get() + "]");
                if (req.isPresent()) min = Optional.of("<1, ");
                else min = Optional.of("<0, ");
            }
        }

        if (min.isPresent()) {
            mergeType(min.orElse("") + max.orElse(""));
        }
        else if (req.isPresent()) mergeType(JsonDocNames.REQUIRED);
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
}