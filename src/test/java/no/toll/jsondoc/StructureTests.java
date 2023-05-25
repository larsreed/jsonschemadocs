package no.toll.jsondoc;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("OptionalGetWithoutIsPresent")
class StructureTests {

    private Context ctx(final String mode) {
        return new Context(mode)
                .add("test", "true")
//                .add("sampleColumns", "eksempel")
//                .add(Context.SKIP_TABLES,"metadata,data")
//                .add("variant", "tags")
//                .add(Context.EXCLUDED_COLUMNS, "x-ICS2_XML")
                ;
    }

    private String runHtml(final String data) {
        final var context = ctx("HTML");
        return new HtmlPrinter(new JsonDocParser(context).parseString(data), context).create();
    }

    private String runSchema(final String data) {
        final var context = ctx("SCHEMA");
        final var root = new JsonDocParser(context).parseString(data);
        return new SchemaPrinter(root).create();
    }

    @Test
    void attributesOnTopNode_areShown() {
        final var data = """
                {
                  "description": "Top!",
                  "title": "X"
                }""";
        assertTrue(runHtml(data).contains("Top!"));
    }

    @Test
    void required_areHiddenForHtmlAndShownForSchema() {
        final var data = """
                {
                  "title": "X",
                  "required": [
                    "A"
                  ]
                }""";
        assertFalse(runHtml(data).contains(no.toll.jsondoc.JsonDocNames.REQUIRED));
        assertTrue(runSchema(data).contains(no.toll.jsondoc.JsonDocNames.REQUIRED));
    }

    @Test
    void required_marksAttribute() {
        final var data = """
                {
                  "properties": {
                    "title": "X",
                    "B": true
                  },
                  "required": [
                    "A",
                    "title"
                  ]
                }""";
        final var root = new JsonDocParser(ctx("HTML")).parseString(data);
        assertFalse(root.isRequired(), "root");
        for (final var c : root.children) {
            if (c.name.equals("title")) assertTrue(c.isRequired(), c.name);
            else assertFalse(c.isRequired(), c.name);
        }
    }

    @Test
    void properties_areRemovedForHtmlAndShownForSchema() {
        final var data = """
                {
                  "title": "X",
                  "properties": {
                    "A": "b"
                  }
                }""";
        assertFalse(runHtml(data).contains(no.toll.jsondoc.JsonDocNames.PROPERTIES));
        assertTrue(runSchema(data).contains(no.toll.jsondoc.JsonDocNames.PROPERTIES));
    }

    @Test
    void properties_areMovedToParent() {
        final var data = """
                {
                  "top": "",
                  "properties": {
                    "prop": ""
                  }
                }""";
        assertTrue(runHtml(data).matches("(?s).*<td>top(</?td>)+</tr>(\\s*\\R*\\s*)*<tr><td>prop</td>.*"));
    }

    @Test
    void type_canContainArray() {
        final var data = """
                {
                  "properties": {
                    "A": "b",
                    "foo": {
                      "type": [
                        "string",
                        "null"
                      ]
                    }
                  }
                }""";
        assertTrue(runHtml(data).contains("string, null"));
    }

    @Test
    void alwaysColumns_areThere() {
        final var data = """
                {
                  "A": "b"
                }""";
        final var res = runHtml(data).toUpperCase();
        for (final var c: no.toll.jsondoc.JsonDocNames.ALWAYS_COLUMNS)
            assertTrue(res.contains("<TH>" + c.toUpperCase() + "</TH>"), c);
    }

    @Test
    void cardinality_none() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "title": "x"
                    }
                  }
                }""";
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("Type"))
                .map(n -> n.values)
                .map(NodeValues::toString);
        assertFalse(vals.isPresent());
    }

    @Test
    void cardinality_required() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "title": "x"
                    }
                  },
                  "required": [
                    "foo"
                  ]
                }""";
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertTrue(vals.contains(no.toll.jsondoc.JsonDocNames.REQUIRED));
    }

    @Test
    void cardinality_minOnly() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "title": "x",
                      "minItems": 11
                    }
                  }
                }""";
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("[11, ...]", vals);
    }

    @Test
    void cardinality_maxOnly() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "title": "x",
                      "maxItems": 12
                    }
                  }
                }""";
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("[0, 12]", vals);
    }

    @Test
    void cardinality_arrayMinOnly() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "type": "array",
                      "minLength": 0,
                      "items": {
                        "type": "object",
                        "properties": {
                          "id": {
                            "type": "string"
                          }
                        }
                      }
                    }
                  }
                }""";
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertTrue(vals.contains("[0...]"));
    }

    @Test
    void cardinality_minMax() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "title": "x",
                      "minItems": 30,
                      "maxItems": 20
                    }
                  }
                }""";
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("[30, 20]", vals);
    }

    @Test
    void cardinality_minMaxEqual() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "title": "x",
                      "minItems": 3,
                      "maxItems": 3
                    }
                  }
                }""";
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("[3]", vals);
    }

    @Test
    void length_minOnly() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "title": "x",
                      "minLength": 11
                    }
                  }
                }""";
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("[11...]", vals);
    }

    @Test
    void length_maxOnly() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "title": "x",
                      "maxLength": 12
                    }
                  }
                }""";
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("[0..12]", vals);
    }

    @Test
    void length_minMax() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "title": "x",
                      "minLength": 30,
                      "maxLength": 20
                    }
                  }
                }""";
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("[30..20]", vals);
    }

    @Test
    void length_minMaxEqual() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "title": "x",
                      "minLength": 3,
                      "maxLength": 3
                    }
                  }
                }""";
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("[3]", vals);
    }

    @Test
    void minmax_minOnly() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "title": "x",
                      "minimum": 11
                    }
                  }
                }""";
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("[11, ...]", vals);
    }

    @Test
    void minmax_maxOnly() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "title": "x",
                      "maximum": 12
                    }
                  }
                }""";
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("[0, 12]", vals);
    }

    @Test
    void minmax_minMax() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "title": "x",
                      "minimum": 20,
                      "maximum": 30
                    }
                  }
                }""";
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("[20, 30]", vals);
    }

    @Test
    void minmax_minMaxEqual() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "title": "x",
                      "minimum": 3,
                      "maximum": 3
                    }
                  }
                }""";
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("[3]", vals);
    }


    @Test
    void minmaxExcl_minOnly() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "title": "x",
                      "exclusiveMinimum": 11
                    }
                  }
                }""";
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("<11, ...]", vals);
    }

    @Test
    void minmaxExcl_maxOnly() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "title": "x",
                      "exclusiveMaximum": 12
                    }
                  }
                }""";
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("[0, 12>", vals);
    }

    @Test
    void minmaxExcl_minMax() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "title": "x",
                      "exclusiveMinimum": 20,
                      "exclusiveMaximum": 30
                    }
                  }
                }""";
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertEquals("<20, 30>", vals);
    }

    @Test
    void type_miscConversions() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "format": "date-time",
                      "type": "string",
                      "uniqueItems": true,
                      "readOnly": true,
                      "writeOnly": true,
                      "deprecated": true,
                      "pattern": "^parrot$",
                      "propertyNames": "^bird$",
                      "const": 42,
                      "multipleOf": 13,
                      "default": "q1",
                      "additionalProperties": false,
                      "enum": [
                        "1",
                        "2",
                        "3"
                      ]
                    }
                  }
                }""";
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertTrue(vals.contains("date-time"), "format");
        assertTrue(vals.contains("string"), "type");
        assertTrue(vals.contains("1, 2, 3"), "enum");
        assertTrue(vals.contains(no.toll.jsondoc.JsonDocNames.UNIQUE_ITEMS), "unique");
        assertTrue(vals.contains(no.toll.jsondoc.JsonDocNames.READ_ONLY), "RO");
        assertTrue(vals.contains(no.toll.jsondoc.JsonDocNames.WRITE_ONLY), "WO");
        assertTrue(vals.contains(no.toll.jsondoc.JsonDocNames.DEPRECATED.toUpperCase()), "deprecated");
        assertTrue(vals.matches("(?s).*" + no.toll.jsondoc.JsonDocNames.PATTERN + ".*=.*parrot.*"), "pattern");
        assertTrue(vals.contains("==42"), "const");
        assertTrue(vals.contains(no.toll.jsondoc.JsonDocNames.MULTIPLE_OF + " 13"), "multiple");
        assertTrue(vals.contains(no.toll.jsondoc.JsonDocNames.DEFAULT + "=q1"), "default");
        assertTrue(vals.contains(no.toll.jsondoc.JsonDocNames.ADDITIONAL_DISP), vals);
        assertTrue(vals.contains(no.toll.jsondoc.JsonDocNames.PROPERTY_NAMES_DISP), vals);
    }

    @Test
    void type_ignoreBooleanSwitches() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "format": "x",
                      "uniqueItems": false,
                      "readOnly": false,
                      "writeOnly": false,
                      "deprecated": false
                    }
                  }
                }""";
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild("type"))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertFalse(vals.contains(no.toll.jsondoc.JsonDocNames.UNIQUE_ITEMS), "unique");
        assertFalse(vals.contains(no.toll.jsondoc.JsonDocNames.READ_ONLY), "RO");
        assertFalse(vals.contains(no.toll.jsondoc.JsonDocNames.WRITE_ONLY), "WO");
        assertFalse(vals.contains(no.toll.jsondoc.JsonDocNames.DEPRECATED.toUpperCase()), "deprecated");
    }

    @Test
    void extid_handlesSpecialChars() {
        final var node = new Node("(rød)", NodeType.Object, null, ctx("HTML"));
        assertEquals("r_d_", node.extId());
    }

    @Test
    void description_convertsId() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "$id": "bar"
                    }
                  }
                }""";
        final var vals = new JsonDocParser(ctx("HTML")).parseString(data).getChild("foo")
                .flatMap(n -> n.getChild(no.toll.jsondoc.JsonDocNames.DESCRIPTION))
                .map(n -> n.values)
                .map(NodeValues::toString)
                .get();
        assertTrue(vals.contains("id="), "format");
        assertTrue(vals.contains("bar"), vals);
        assertFalse(vals.contains(no.toll.jsondoc.JsonDocNames.ID), vals);
    }

    @Test
    void specialNames_notConfused() {
        final var data = """
                {
                  "foo": {
                    "type": "object",
                    "properties": {
                      "type": {
                        "type": "string",
                        "minLength": 1
                      },
                      "required": {
                        "type": "string",
                        "minLength": 1
                      }
                    },
                    "description": {
                      "type": "string",
                      "minLength": 1
                    },
                    "field": {
                      "type": "string",
                      "minLength": 1
                    },
                    "required": [
                      "type",
                      "field"
                    ]
                  }
                }""";
        final var data2 = """
                {
                  "foo": {
                    "type": "object",
                    "properties": {
                      "type2": {
                        "type": "string",
                        "minLength": 1
                      },
                      "required2": {
                        "type": "string",
                        "minLength": 1
                      },
                      "description2": {
                        "type": "string",
                        "minLength": 1
                      },
                      "field2": {
                        "type": "string",
                        "minLength": 1
                      }
                    },
                    "required": [
                      "type2",
                      "field2"
                    ]
                  }
                }""";
        final var res = new DebugPrinter(new JsonDocParser(ctx("HTML")).parseString(data)).create()
                .replaceAll("2", "");
        final var res2 = new DebugPrinter(new JsonDocParser(ctx("HTML")).parseString(data2)).create()
                .replaceAll("2", "");
        assertEquals(res2, res);
    }

    @Test
    void defref_convertsText() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "$ref": "#/$defs/foobar"
                    },
                    "$defs": {
                      "foobar": {
                        "aKey": "aValue"
                      }
                    }
                  }
                }""";
        final var ctx = ctx("HTML");
        final var vals = new HtmlPrinter(new JsonDocParser(ctx).parseString(data), ctx).create();
        assertTrue(vals.contains("<td>($defs)</td>"), "$defs in parens");
        assertTrue(vals.contains("<td>$ref</td>"), "still uses $ref");
        assertTrue(vals.contains("href=\"#$defs__foobar\""), "contains hyperlink");
    }

    @Test @Ignore
    void embedding_correctColumns() {
        final var data = """
                {
                  "properties": {
                    "x": "y",
                    "fooList": {
                      "description": "foo",
                      "type": "array",
                      "x-sample": [        "oh!"
                      ],
                      "items": {
                        "type": "string"
                      }
                    }
                  }
                }""";
        final var res = runHtml(data);
        // FIXME fail();
    }
}