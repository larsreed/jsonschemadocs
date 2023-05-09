package no.toll.jsondoc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleTests {

    private Context ctx() {
        return new Context("SAMPLE")
                .add("test", "true")
//                .add("sampleColumns", "eksempel")
//                .add(Context.SKIP_TABLES,"metadata,data")
//                .add("variant", "tags")
//                .add(Context.EXCLUDED_COLUMNS, "x-ICS2_XML")
                ;
    }

    @Test
    void sample_willCreateIfNotPresent() {
        final var data = """
{
  "properties": {
    "title": "X",
    "foo": {
      "type": "number"
    },
    "bar": {
      "type": "string"
    }
  }
}
                """;
        final var context = ctx().add(Context.SAMPLE_COLUMNS, "x-smp");
        final var res = new SamplePrinter(new JsonDocParser(context).parseString(data), context).toString();
        assertTrue(res.matches("(?s).*foo.: [-.0-9]+.*"), res);
        assertTrue(res.matches("(?s).*bar.:.*"), res);
    }

    @Test
    void sample_willUseNamedColumn() {
        final var data = """
{
  "properties": {
    "title": "X",
    "foo": {
      "type": "number",
      "x-smp": 8048.6
    },
    "bar": {
      "type": "string",
      "x-smp": "smpl"
    }
  }
}""";
        final var context = ctx().add(Context.SAMPLE_COLUMNS, "x-smp");
        final var res = new SamplePrinter(new JsonDocParser(context).parseString(data), context).testString();
        assertFalse(res.contains("x-smp"), res);
        assertTrue(res.contains("foo:8048.6"), res);
        assertTrue(res.contains("bar:smpl"), res);
    }

    @Test
    void sample_canUseExamples() {
        final var data = """
{
  "title": "X",
  "properties": {
    "foo": {
      "type": "number",
      "x-smp": 8048.6,
      "examples": [
        "747",
        "757",
        "777"
      ]
    },
    "bar": {
      "type": "string",
      "examples": [
        "747",
        "757",
        "777"
      ]
    }
  }
}""";
        final var context = ctx().add(Context.SAMPLE_COLUMNS, "x-smp");
        final var rootNode = new JsonDocParser(context).parseString(data);
        final var res = new SamplePrinter(rootNode, context).testString();
        assertFalse(res.contains("x-smp"), res);
        assertTrue(res.contains("foo:8048.6"), res);
        assertTrue(res.contains("bar:747"), res);
    }

    @Test
    void sample_canUseConstants() {
        final var data = """
{
  "title": "X",
  "properties": {
    "foo": {
      "type": "number",
      "const": 42
    },
    "bar": {
      "type": "string",
      "const": "forty-two"
    }
  }
}""";
        final var context = ctx();
        final var rootNode = new JsonDocParser(context).parseString(data);
        final var res = new SamplePrinter(rootNode, context).testString();
        assertTrue(res.contains("foo:42"), res);
        assertTrue(res.contains("bar:forty-two"), res);
    }

    @Test
    void sample_canUseEnum() {
        final var data = """
{
  "title": "X",
  "properties": {
    "foo": {
      "type": "number",
      "enum": [        42,
        43,
        44
      ]
    },
    "bar": {
      "type": "string",
      "enum": [        "alfa",
        "bravo",
        "charlie"
      ]
    }
  }
}""";
        final var context = ctx();
        final var rootNode = new JsonDocParser(context).parseString(data);
        final var res = new SamplePrinter(rootNode, context).testString();
        assertTrue(res.contains("foo:42"), res);
        assertTrue(res.contains("bar:alfa"), res);
    }

    @Test
    void sample_generatesBigNumbers() {
        final var data = """
{
  "title": "X",
  "properties": {
    "foo": {
      "type": "number",
      "minimum": 9999999999999990,
      "maximum": 9999999999999999
    }
  }
}""";
        final var context = ctx();
        final var rootNode = new JsonDocParser(context).parseString(data);
        final var res = new SamplePrinter(rootNode, context).toString();
        assertTrue(res.matches("(?s).*foo.: 999999999999999[0-9].*"), res);
    }

    @Test
    void sample_generatesDecimalNumbers() {
        final var data = """
{
  "title": "X",
  "properties": {
    "foo": {
      "type": "number",
      "minimum": -1.3,
      "maximum": -1.25
    }
  }
}""";
        final var context = ctx();
        final var rootNode = new JsonDocParser(context).parseString(data);
        final var res = new SamplePrinter(rootNode, context).toString();
        assertTrue(res.matches("(?s).*foo.: -1[.][23][0-9].*"), res);
    }

    @Test
    void sample_emitsArrayTypeForSimpleArrayItem() {
        final var data = """
{
  "properties": {
    "codeList": {
      "type": "array",
      "minItems": 1,
      "items": {
        "type": "string",
        "description": "code",
        "examples": [
          "JAVA"
        ]
      }
    }
  }
}
""";

        final var context = ctx();
        final var rootNode = new JsonDocParser(context).parseString(data);
        final var res = new SamplePrinter(rootNode, context).testString();
        assertTrue(res.contains("{codeList:[JAVA]}"), res);
    }

    @Test
    void sample_emitsArrayTypeForObjectArray() {
        final var data = """
{
  "properties": {
    "data": {
      "type": "object",
      "properties": {
        "decision": {
          "type": "string",
          "examples": [
            "1"
          ]
        },
        "triggerList": {
            "minItems": 1,
            "type": "array",
            "items": {
            "type": "object",
            "properties": {
                "trigger": {
                    "type": "string",
                    "examples": [
                      "BANG"
                    ]
                }
            }
          }
        }
      }
    }
  }
}
""";

        final var context = ctx();
        final var rootNode = new JsonDocParser(context).parseString(data);
        final var res = new SamplePrinter(rootNode, context).testString();
        assertTrue(res.contains("triggerList:[{trigger:BANG}]"), res);
    }

    @Test
    void sample_emitsMultipleExamples() {
        final var data = """
{
    "properties": {
        "data": {
            "type": "object",
            "properties": {
                "codeList": {
                    "type": "array",
                    "minItems": 5,
                    "items": {
                        "type": "string",
                        "examples": [ "Java", "Kotlin" ]
                    }
                },
                "decision": {
                    "type": "string",
                    "examples": [ "1" ]
                },
                "triggerList": {
                    "minItems": 3,
                    "type": "array",
                    "items": {
                        "type": "object",
                        "properties": {
                            "trigger": {
                                "type": "string",
                                "examples": [ "BANG", "PANG" ]
                            },
                            "target": {
                                "type": "integer",
                                "examples": [ 1,2,3 ]
                            }
                        }
                    }
                }
            }
        }
    }
}
""";

        final var context = ctx();
        final var rootNode = new JsonDocParser(context).parseString(data);
        final var res = new SamplePrinter(rootNode, context).testString();

        //System.err.println(new SamplePrinter(rootNode, context).toString());
        final var objectSample =
                "triggerList:[{trigger:BANG,target:1,},{trigger:PANG,target:2,},{trigger:BANG,target:3}]";
        final var stringSample = "codeList:[Java,Kotlin,Java,Kotlin,Java]";
        assertTrue(res.contains(objectSample), "object " + res);
        // assertTrue(res.contains(stringSample), "string " + res);
    }

    @Test
    void sample_emitsArrayType() {
        final var data = """
                {
                  "title": "X",
                  "properties": {
                    "foo": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "properties": {
                          "bar": {
                            "type": "integer",
                            "examples": [ 4, 5, 6 ]
                          }
                        }
                      }
                    }
                  }
                }
                                """;
        final var context = ctx();
        final var rootNode = new JsonDocParser(context).parseString(data);
        final var res = new SamplePrinter(rootNode, context).testString();
        assertTrue(res.contains("{foo:[{bar:4}]}"), res);
    }
}