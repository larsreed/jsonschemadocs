package net.kalars.jsondoc;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Map-based implementation of a Context. */
@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "SameParameterValue"})
class Context {

    static final String MODE = "mode";
    static final String EMBED_ROWS = "embedUpToRows";
    static final String VARIANT = "variant";
    static final String EXCLUDED_COLUMNS = "excludeColumns";
    static final String SKIP_TABLES = "skipTables";

    private final Map<String, String> map = new LinkedHashMap<>();

    Context(final String mode) { this.map.put(MODE, mode); }

    @SuppressWarnings("UnusedReturnValue")
    Context add(final String key, final String value) {
        this.map.put(key, value);
        return this;
    }

    Optional<String> value(final String key) { return Optional.ofNullable(this.map.get(key)); }

    boolean matches(final String key, final boolean notMatched, final String... candidates) {
        final var hit = this.map.get(key);
        if (hit==null) return notMatched; // it is considered a match if no values at all are given for this key
        return Arrays.stream(candidates).anyMatch(hit::equalsIgnoreCase);
    }

    boolean anyMatch(final String key, final String toMatch) {
        final var hit = this.map.get(key);
        return Arrays.stream((hit==null? "" : hit).split(", *")).anyMatch(k -> k.equalsIgnoreCase(toMatch));
    }

    boolean isExcluded(final String column) {
        final var excluded = this.map.get(EXCLUDED_COLUMNS);
        if (excluded==null) return false; // no columns excluded
        return Arrays.stream(excluded.split(", *"))
                .anyMatch(excl -> excl.equalsIgnoreCase(column) ||
                        excl.equalsIgnoreCase(JsonDocNames.XDOC_PREFIX+column.replaceAll(" ", "_")));
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