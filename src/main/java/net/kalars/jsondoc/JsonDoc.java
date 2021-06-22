package net.kalars.jsondoc;

import java.util.regex.Pattern;

/** The CLI main class. */
public final class JsonDoc {
    private static final Pattern pattern = Pattern.compile("^([^=]+)=([^=]+)$");
    private static final int EDATA = 65;

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
            if (match.matches() && match.groupCount()==2) context.add(match.group(1), match.group(2));
            else help("Illegible argument " + arg, 1);
        }

        runWith(args[0], inputFile, context);
    }

    private static void runWith(final String outType, final String inputfile, final Context context) {
        switch (outType.toUpperCase()) {
            case "HTML" -> {
                final var root = new JsonDocParser(context).parseFile(inputfile);
                final var printer = new HtmlPrinter(root);
                System.out.println(printer);
            }
            case "WIKI" -> {
                final var root = new JsonDocParser(context).parseFile(inputfile);
                final var printer = new WikiPrinter(root);
                System.out.println(printer);
            }
            case "MARKDOWN" -> {
                final var root = new JsonDocParser(context).parseFile(inputfile);
                final var printer = new MarkdownPrinter(root);
                System.out.println(printer);
            }
            case "GRAPH" -> {
                final var root = new JsonDocParser(context).parseFile(inputfile);
                final var printer = new GraphPrinter(root);
                System.out.println(printer);
            }
            case "SCHEMA" -> {
                final var root = new JsonDocParser(context).parseFile(inputfile);
                final var printer = new SchemaPrinter(root);
                System.out.println(printer);
            }
            case "SAMPLE" -> {
                final var root = new JsonDocParser(context).parseFile(inputfile);
                final var printer = new SamplePrinter(root, context);
                System.out.println(printer);
            }
            case "VALIDATE" -> {
                final ValidationResult res = Validator.validate(inputfile, context);
                System.out.println(res);
                if (!res.isOk()) System.exit(EDATA);
            }
            default -> help("Unknown type " + outType, 1);
        }
    }

    private static void help(final String message, final int err) {
        System.err.println(message);
        System.out.println("""
        JSON SCHEMA DOCUMENTATION TOOL -- Lars Reed, 2021
        Usage: java -jar jsondoc.jar TYPE SCHEMAFILE [DEFINITIONS] > resultfile
        
        TYPE:
            SCHEMA:   output a clean schema file, without additional attributes
            HTML:     output HTML-formatted documentation
            MARKDOWN: output Markdown-formatted documentation
            GRAPH:    output a script to create a graph using graphviz/dot
            WIKI:     output in Confluence wiki XHTML format
            SAMPLE:   output sample data -- Note: Experimental!
            VALIDATE: perform validation of datafiles against a schema
        SCHEMAFILE: name of extended JSON Schema file
        DEFINITIONS: follows the pattern name=value, and comes after the inputfile""");
        System.out.println("    " + Context.VARIANT + "=foo could define a context for \""
                + JsonDocNames.XIF_PREFIX + Context.VARIANT + "\": \"foo\"");
        System.out.println("    " + Context.EXCLUDE_COLUMNS + "=col1,col2,... to exclude named columns");
        System.out.println("    " + Context.SKIP_TABLES + "=table1,table2,... to exclude tables with given IDs");
        System.out.println("    " + Context.EMBED_ROWS + "=n defines embedding in HTML tables");
        System.out.println("    " + Context.SAMPLE_COLUMNS + "=col1,... defines columns to use for sample output");
        System.out.println("    " + Context.FILES + "=file1,... required with VALIDATE to name files to validate");
        System.out.println("    " + Context.STRICT + "=true with SCHEMA/VALIDATE to have strict schema/validation");
        System.out.println("""
                Output is written to stdout and should be redirected.""");
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