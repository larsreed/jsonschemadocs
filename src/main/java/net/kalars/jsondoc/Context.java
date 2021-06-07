package net.kalars.jsondoc;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** The runtime context settings (variable defs etc). */
@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "SameParameterValue"})
class Context {

    static final String EMBED_ROWS = "embedUpToRows";
    static final String EXCLUDE_COLUMNS = "excludeColumns";
    static final String FILES = "files";
    static final String MODE = "mode";
    static final String SAMPLE_COLUMNS = "sampleColumns";
    static final String SCHEMA_MODE = "SCHEMA";
    static final String SKIP_TABLES = "skipTables";
    static final String STRICT = "strict";
    static final String VARIANT = "variant";

    private final Map<String, String> map = new LinkedHashMap<>();

    Context(final String mode) { this.map.put(MODE, mode); }

    Context add(final String key, final String value) {
        this.map.put(key, value);
        return this;
    }

    Context clone(final String mode) {
        final var clone = new Context(mode);
        clone.map.putAll(this.map);
        clone.map.put(MODE, mode);
        return clone;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isSchemaMode() {return SCHEMA_MODE.equalsIgnoreCase(this.map.get(MODE)); }
    Optional<String> value(final String key) { return Optional.ofNullable(this.map.get(key)); }
    boolean contains(final String key) { return map.containsKey(key); }
    @Override public String toString() { return "Context{"  + map + '}'; }

    /** Does the given key exist in the context, and does it contain the given value toMatch?
     *  The value is read as comma-separated, and case-insensitive. */
    Optional<Boolean> anyMatch(final String key, final String toMatch) {
        final var hit = this.map.get(key);
        if (hit==null || hit.isEmpty()) return Optional.empty();
        final var candidates = toMatch.split(", *");
        final var keys = hit.split(", *");
        for (final var match: candidates) {
            if (Arrays.stream(keys).anyMatch(k -> k.equalsIgnoreCase(match))) return Optional.of(true);
        }
        return Optional.of(false);
    }

    boolean isExcluded(final String column) {
        final var excluded = this.map.get(EXCLUDE_COLUMNS);
        if (excluded==null) return false; // no columns excluded
        return Arrays.stream(excluded.split(", *"))
                .anyMatch(excl -> excl.equalsIgnoreCase(column) ||
                                  excl.equalsIgnoreCase(JsonDocNames.XDOC_PREFIX +
                                          column.replaceAll(" ", "_")));
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