package no.toll.jsondoc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlTests {

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
        final var root = new JsonDocParser(context).parseString(data);
        return new HtmlPrinter(root, context).create();
    }

    @Test
    void html_lineBreaks() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "x-bar": "ab\\nc"
                    }
                  }
                }""";
        final var res = runHtml(data);
        assertTrue(res.matches("(?s).*ab<br/>c.*"));
    }

    @Test
    void html_simpleLink() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "x-bar": "linkTo(http://github.com)"
                    }
                  }
                }""";
        final var res = runHtml(data);
        assertTrue(res.matches("(?s).*<a href=.http://github.com.>http://github.com</a>.*"), res);
    }

    @Test
    void html_linkWithText() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "x-bar": "linkTo(http://github.com,  target)"
                    }
                  }
                }""";
        final var res = runHtml(data);
        assertTrue(res.matches("(?s).*<a href=.http://github.com.>target</a>.*"), res);
    }

    @Test
    void html_langAttr() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "x-bar": "linkTo(http://github.com,  target)"
                    }
                  }
                }""";
        final var res = runHtml(data);
        assertTrue(res.matches("(?s).*lang=." + Context.LANG_EN + ".*"), res);
        final var context2 = ctx("HTML").add(Context.LANG, "no");
        final var res2 =  new HtmlPrinter(new JsonDocParser(context2).parseString(data), context2).create();
        assertTrue(res2.matches("(?s).*lang=.no.*"), res);
    }

    @Test
    void html_linkToTable() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "description": "1",
                      "bar": {
                        "description": "2",
                        "ting": "tang"
                      },
                      "baz": {
                        "description": "3",
                        "ting": "tang"
                      }
                    }
                  }
                }""";
        final var res = runHtml(data);
        assertTrue(res.matches("(?s).*<a href=.#foo__bar.>bar.*<h3 id=.foo__bar.*"), res);
    }

    @Test
    void xhtml_linkToTable() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "description": "1",
                      "bar": {
                        "description": "2",
                        "ting": "tang"
                      },
                      "baz": {
                        "description": "3",
                        "ting": "tang"
                      }
                    }
                  }
                }""";
        final var context = ctx("XHTML");
        final var root = new JsonDocParser(context).parseString(data);
        final var res = new WikiPrinter(root, context).create();
        assertTrue(res.matches("(?s).*ac:link ac:anchor=.foo__bar.*CDATA.bar.*ac:name=.anchor.*foo__bar.*bar.*"),
                res);
    }
}