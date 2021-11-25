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