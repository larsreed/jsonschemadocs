package net.kalars.jsondoc;

import java.util.Arrays;

public class Logger  {
    private static final boolean DEBUG = false;
    private static final boolean ERROR = true;
    static void debug(final Object... msg) {
        if (!DEBUG) return;
        Arrays.stream(msg).forEach(o-> System.err.println(o.toString() + " "));
        System.err.println();
    }

    static void error(final Object... msg) {
        if (!ERROR) return;
        Arrays.stream(msg).forEach(o-> System.err.println(o.toString() + " "));
        System.err.println();
    }

}