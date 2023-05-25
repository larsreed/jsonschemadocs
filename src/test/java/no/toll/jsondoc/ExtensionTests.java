package no.toll.jsondoc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtensionTests {

    // TODO test skipTables

    private Context ctx(final String mode) {
        return new Context(mode)
                .add("test", "true")
//                .add("sampleColumns", "eksempel")
//                .add(Context.SKIP_TABLES,"metadata,data")
//                .add("variant", "tags")
//                .add(Context.EXCLUDED_COLUMNS, "x-ICS2_XML")
                ;
    }

    @Test
    void xif_isIgnoredIfNoVariants() {
        final var data = """
            {
              "properties": {
                "title": "X",
                "foo": {
                  "baz": "Z",
                  "description": "Y",
                  "xif-variant": "var1"
                },
                "bar": {
                  "key": "Q"
                }
              }
            }""";
        final var context = ctx("HTML");
        final var res = new HtmlPrinter(new JsonDocParser(context).parseString(data), context).create();
        assertTrue(res.contains("<td>X</td>"), "title");
        assertTrue(res.contains("<td>Y<"), "description");
        assertTrue(res.contains("<td>bar</td>"), "bar");
        assertTrue(res.contains("<td>key</td>"), "key");
        assertTrue(res.contains("<td>baz</td>"), "baz");
        assertFalse(res.contains("xif"), "xif");
    }

    @Test
    void xif_includesOnCorrectVariable() {
        final var data = """
            {
              "properties": {
                "title": "X",
                "foo": {
                  "baz": "Z",
                  "description": "Y",
                  "xif-variant": "var1"
                },
                "bar": {
                  "key": "Q"
                }
              }
            }""";
        final var context = ctx("HTML")
                .add("variant", "var1"); // This is different from previous test
        final var res = new HtmlPrinter(new JsonDocParser(context).parseString(data), context).create();
        assertTrue(res.contains("<td>X</td>"), "title");
        assertTrue(res.contains("<td>Y"), "description");
        assertTrue(res.contains("<td>bar</td>"), "bar");
        assertTrue(res.contains("<td>key</td>"), "key");
        assertTrue(res.contains("<td>baz</td>"), "baz");
        assertFalse(res.contains("xif"), "xif");
    }

    @Test
    void xif_excludesOnOtherVariable() {
        final var data = """
            {
              "properties": {
                "title": "X",
                "foo": {
                  "baz": "Z",
                  "description": "Y",
                  "xif-variant": "var1"
                },
                "bar": {
                  "key": "Q"
                }
              }
            }""";
        final var context = ctx("HTML")
                .add("variant", "var2"); // This is different from previous test
        final var res = new HtmlPrinter(new JsonDocParser(context).parseString(data), context).create();
        assertTrue(res.contains("<td>X</td>"), "title");
        assertFalse(res.contains("<td>Y"), "description");
        assertTrue(res.contains("<td>bar</td>"), "bar");
        assertTrue(res.contains("<td>key</td>"), "key");
        assertFalse(res.contains("<td>baz</td>"), "baz");
        assertFalse(res.contains("xif"), "xif");
    }

    @Test
    void xifnot_isIgnoredIfNoVariants() {
        final var data = """
                {
                  "properties": {
                    "title": "X",
                    "foo": {
                      "baz": "Z",
                      "description": "Y",
                      "xifnot-variant": "var1"
                    },
                    "bar": {
                      "key": "Q"
                    }
                  }
                }""";
        final var context = ctx("HTML");
        final var res = new HtmlPrinter(new JsonDocParser(context).parseString(data), context).create();
        assertTrue(res.contains("<td>X</td>"), "title");
        assertTrue(res.contains("<td>Y<"), "description");
        assertTrue(res.contains("<td>bar</td>"), "bar");
        assertTrue(res.contains("<td>key</td>"), "key");
        assertTrue(res.contains("<td>baz</td>"), "baz");
        assertFalse(res.contains("xif"), "xif");
    }

    @Test
    void xifnot_excludesOnCorrectVariable() {
        final var data = """
                {
                  "properties": {
                    "title": "X",
                    "foo": {
                      "baz": "Z",
                      "description": "Y",
                      "xifnot-variant": "var1"
                    },
                    "bar": {
                      "key": "Q"
                    }
                  }
                }
                """;
        final var context = ctx("HTML")
                .add("variant", "var1"); // This is different from previous test
        final var res = new HtmlPrinter(new JsonDocParser(context).parseString(data), context).create();
        assertTrue(res.contains("<td>X</td>"), "title");
        assertFalse(res.contains("<td>Y"), "description");
        assertTrue(res.contains("<td>bar</td>"), "bar");
        assertTrue(res.contains("<td>key</td>"), "key");
        assertFalse(res.contains("<td>baz</td>"), "baz");
        assertFalse(res.contains("xif"), "xif");
    }

    @Test
    void xifnot_includesOnOtherVariable() {
        final var data = """
                {
                  "properties": {
                    "title": "X",
                    "foo": {
                      "baz": "Z",
                      "description": "Y",
                      "xifnot-variant": "var1"
                    },
                    "bar": {
                      "key": "Q"
                    }
                  }
                }
                """;
        final var context = ctx("HTML")
                .add("variant", "var2"); // This is different from previous test
        final var res = new HtmlPrinter(new JsonDocParser(context).parseString(data), context).create();
        assertTrue(res.contains("<td>X</td>"), "title");
        assertTrue(res.contains("<td>Y"), "description");
        assertTrue(res.contains("<td>bar</td>"), "bar");
        assertTrue(res.contains("<td>key</td>"), "key");
        assertTrue(res.contains("<td>baz</td>"), "baz");
        assertFalse(res.contains("xif"), "xif");
    }

    @Test
    void xdoc_convertsToColumn() {
        final var data = """
                {
                  "title": "X",
                  "properties": {
                    "foo": {
                      "baz": "Z",
                      "x-bar_ba_DOS": "Y"
                    }
                  }
                }""";
        final var context = ctx("HTML");
        final var root = new JsonDocParser(context).parseString(data);
        final var res = new HtmlPrinter(root, context).create();
        assertTrue(res.contains("<td>baz</td>"), "row");
        assertTrue(res.contains("<th>Bar ba DOS</th>"), "row");
        assertFalse(res.contains("x-bar"), "xif");
    }

    @Test
    void excludedColumns_excludes() {
        final var data = """
                {
                  "properties": {
                    "title": "X",
                    "A": 1.0,
                    "B": true
                  }
                }""";
        final var context = ctx("HTML").add(Context.EXCLUDE_COLUMNS, "Q,B,W");
        final var res = new HtmlPrinter(new JsonDocParser(context).parseString(data), context).create();
        assertTrue(res.contains("<td>A</td>"), "A " + res);
        assertFalse(res.contains("<td>B</td>"), "B " + res);
    }

    @Test
    void strict_introducesExtraProperty() {
        final var data = """
                {
                  "properties": {
                    "title": "X",
                    "foo": {
                      "type": "number"
                    }
                  }
                }""";
        final var context = ctx("SCHEMA").add(Context.STRICT, "true");
        final var res = new SchemaPrinter(new JsonDocParser(context).parseString(data)).create();
        assertTrue(res.matches("(?s).*" + JsonDocNames.ADDITIONAL_PROPERTIES + ".: false.*"), res);
    }

    @Test
    void strict_introducesExtraItems() {
        final var data = """
                {
                  "properties": {
                    "title": "X",
                    "foo": {
                      "type": "array",
                      "items": {
                        "type": "string"
                      }
                    }
                  }
                }""";
        final var context = ctx("SCHEMA").add(Context.STRICT, "true");
        final var res = new SchemaPrinter(new JsonDocParser(context).parseString(data)).create();
        assertTrue(res.matches("(?s).*" + JsonDocNames.ADDITIONAL_ITEMS + ".: false.*"), res);
    }
}