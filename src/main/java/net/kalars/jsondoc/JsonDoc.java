package net.kalars.jsondoc;

import java.util.regex.Pattern;

/** The CLI main class. */
@SuppressWarnings("FeatureEnvy")
public final class JsonDoc {
    private static final Pattern pattern = Pattern.compile("^-D([^=]+)=([^=]+)$");

    public static void main(final String[] args) {
        if (args.length == 1 && "help".equalsIgnoreCase(args[0])) help("Help", 0);
        else if (args.length < 2) help("At least two arguments required", 2);

        final var action = args[0].toUpperCase();
        final var inputFile = args[1];

        // Context with defaults
        final var context = new Context(action);
        context.add(Context.EMBED_ROWS, "1");

        for (int i = 2; i < args.length; i++) {
            final var arg = args[i];
            final var match = pattern.matcher(arg);
            if (match.matches()) context.add(match.group(1), match.group(2));
            else help("Illegible argument " + arg, 1);
        }

        runWith(args[0], inputFile, context);
    }

    private static void runWith(final String outType, final String inputfile, final Context context) {
        switch (outType.toUpperCase()) {
            case "HTML" -> {
                final var visitor3 = new JsonDocHtmlVisitor(context);
                new JsonSchemaParser().parseFile(inputfile).visit(visitor3);
                System.out.println(visitor3);
            }
            case "WIKI" -> {
                final var visitor2 = new JsonDocWikiVisitor(context);
                new JsonSchemaParser().parseFile(inputfile).visit(visitor2);
                System.out.println(visitor2);
            }
            case "GRAPH" -> {
                final var visitor4 = new JsonDocDotVisitor(context);
                new JsonSchemaParser().parseFile(inputfile).visit(visitor4);
                System.out.println(visitor4);
            }
            case "SCHEMA" -> {
                final var visitor1 = new JsonSchemaPrintVisitor(context);
                new JsonGenParser().parseFile(inputfile).visit(visitor1);
                System.out.println(visitor1);
            }
            default -> help("Unknown type " + outType, 1);
        }
    }

    private static void help(final String message, final int err) {
        System.err.println(message);
        System.out.println("JSON SCHEMA DOCUMENTATION TOOL -- Lars Reed, 2021");
        System.out.println("Usage: java -jar jsondoc.jar TYPE INPUTFILE [DEFINITIONS] > resultfile");
        System.out.println();
        System.out.println("TYPE: one of SCHEMA, HTML, GRAPH, WIKI");
        System.out.println("    SCHEMA: output a clean schema file, without additional attributes");
        System.out.println("    HTML:   output HTML-formatted documentation");
        System.out.println("    DOT:    output a script to create a graph using graphviz/dot");
        System.out.println("    WIKI:   output in Confluence wiki format (not working yet)");
        System.out.println("INPUTFILE: name of extended JSON Schema file");
        System.out.println("DEFINTIONS: follows the pattern -Dname=value, and precedes the TYPE");
        System.out.println("    e.g. -D" + Context.VARIANT + "=variant could define a context for \""
                + JsonDocNames.XIF_PREFIX + Context.VARIANT + "\": \"variant\"");
        System.out.println("    -D" + Context.EXCLUDED_COLUMNS + "=\"col1,col2,...\" to exclude named columns");
        System.out.println("    -D" + Context.EMBED_ROWS + "=\"n\" defines embedding in HTML tables");
        System.out.println("Output is written to stdout and must be redirected.");
        System.exit(err);
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