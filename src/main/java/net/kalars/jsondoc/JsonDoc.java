package net.kalars.jsondoc;

@SuppressWarnings("FeatureEnvy")
final public class JsonDoc {
    public static void main(final String[] args) {
        if (args.length == 1 && "help".equalsIgnoreCase(args[0])) help("Help");
        else if (args.length < 2) help("Two arguments required");
        else runWith(args[0], args[1], args);
    }

    private static void runWith(final String outType, final String inputfile, final String... args) {
        switch (outType.toUpperCase()) {
            case "HTML" -> {
                int embedUpToRows = 1;
                if (args.length > 2 && args[2].matches("[0-9]+")) embedUpToRows = Integer.parseInt(args[2]);
                final var visitor3 = new JsonDocHtmlVisitor(embedUpToRows);
                new JsonSchemaParser().parseFile(inputfile).visit(visitor3);
                System.out.println(visitor3);
            }
            case "WIKI" -> {
                final var visitor2 = new JsonDocWikiVisitor();
                new JsonSchemaParser().parseFile(inputfile).visit(visitor2);
                System.out.println(visitor2);
            }
            case "GRAPH" -> {
                final var visitor4 = new JsonDocDotVisitor();
                new JsonSchemaParser().parseFile(inputfile).visit(visitor4);
                System.out.println(visitor4);
            }
            case "SCHEMA" -> {
                final var visitor1 = new JsonSchemaPrintVisitor();
                new JsonGenParser().parseFile(inputfile).visit(visitor1);
                System.out.println(visitor1);
            }
            default -> help("Unknown type " + outType);
        }
    }

    private static void help(final String message) {
        System.err.println(message);
        System.out.println("Usage: java -jar jsondoc.jar type inputfile [additional args] > resultfile");
        System.out.println();
        System.out.println("type: one of SCHEMA, HTML, GRAPH, WIKI");
        System.out.println("    SCHEMA: output a clean schema file, without additional attributes");
        System.out.println("    HTML:   output HTML-formatted documentation");
        System.out.println("            this type accepts, but does not require, an additional argument:");
        System.out.println("            number -- for embedding tables, see documentation");
        System.out.println("    DOT:    output a script to create a graph using graphviz/dot");
        System.out.println("    WIKI:   output in Confluence wiki format (not working yet)");
        System.out.println("inputfile: name of extended JSON Schema file");
        System.out.println("Output is written to stdout and must be redirected.");
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