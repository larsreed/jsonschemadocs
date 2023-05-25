package no.toll.jsondoc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownTests {

    private Context ctx() {
        return new Context("MARKDOWN")
                .add("test", "true")
//                .add("sampleColumns", "eksempel")
//                .add(Context.SKIP_TABLES,"metadata,data")
//                .add("variant", "tags")
//                .add(Context.EXCLUDED_COLUMNS, "x-ICS2_XML")
                ;
    }

    private String runMarkdown(final String data) {
        final var context = ctx();
        final var root = new JsonDocParser(context).parseString(data);
        return new MarkdownPrinter(root).create();
    }


    @Test
    void markdown_lineBreaks() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "x-bar": "ab\\nc"
                    }
                  }
                }""";
        final var res = runMarkdown(data);
        assertTrue(res.contains("ab<br />c"));
    }

    @Test
    void markdown_simpleLink() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "x-bar": "linkTo(http://github.com)"
                    }
                  }
                }""";
        final var res = runMarkdown(data);
        assertTrue(res.matches("(?s).*\\[http://github.com].*"), res);
    }

    @Test
    void markdown_linkWithText() {
        final var data = """
                {
                  "properties": {
                    "foo": {
                      "x-bar": "linkTo(http://github.com,  target)"
                    }
                  }
                }""";
        final var res = runMarkdown(data);
        assertTrue(res.matches("(?s).*\\[target]\\(http://github.com\\).*"), res);
    }

    @Test
    void markdown_linkToTable() {
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
        final var res = runMarkdown(data);
        assertTrue(res.matches("(?s).*\\[bar>]\\(#foo__bar\\).*"), res);
    }
}