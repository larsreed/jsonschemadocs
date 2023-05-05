package no.toll.jsondoc;

import java.util.Arrays;

@SuppressWarnings("unused")
public final class Logger  {
    private static final boolean DEBUG = true;
    private static final boolean ERROR = true;
    private static final boolean WARN = true;

    static void debug(final Object... msg) {
        if (!DEBUG) return;
        System.err.print("DEBUG: ");
        Arrays.stream(msg).forEach(o-> System.err.print(o.toString() + " "));
        System.err.println();
    }

    static void error(final Object... msg) {
        if (!ERROR) return;
        System.err.print("ERROR: ");
        Arrays.stream(msg).forEach(o-> System.err.print(o.toString() + " "));
        System.err.println();
    }

    static void warn(final Object... msg) {
        if (!WARN) return;
        System.err.print("WARN: ");
        Arrays.stream(msg).forEach(o-> System.err.print(o.toString() + " "));
        System.err.println();
    }
}

/** Wrapping of exceptions that have been reported */
class HandledException extends RuntimeException { HandledException(final Throwable t) { super(t); } }

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