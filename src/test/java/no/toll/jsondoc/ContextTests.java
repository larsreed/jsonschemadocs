package no.toll.jsondoc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class ContextTests {

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void match_multipleKeysAndValues() {
        final var context = new Context("HTML")
                .add("x", "a, b, c,d");
        assertFalse(context.anyMatch("y", "a").isPresent());
        assertTrue(context.anyMatch("x", "a").get());
        assertTrue(context.anyMatch("x", "c").get());
        assertFalse(context.anyMatch("x", "g").get());
    }

    @Test
    void context_test() {
        final var data = """
{
  "properties": {
    "foo": {
      "description": "1",
      "bar": {
        "description": "2",
        "ting": "tang"
      }
    },
    "baz": {
      "description": "3",
      "a": "b"
    }
  }
}""";
        final var context0 = new Context("HTML").add(Context.EMBED_ROWS, "0");
        final var root0 = new JsonDocParser(context0).parseString(data);
        final var res0 = new HtmlPrinter(root0, context0).create();
        final var context1 = new Context("HTML").add(Context.EMBED_ROWS, "1");
        final var root1 = new JsonDocParser(context1).parseString(data);
        final var res1 = new HtmlPrinter(root1, context1).create();
        assertFalse(res0.contains("<td>foo</td><td>1<br/><table>"),res0);
        assertTrue(res1.contains("<td>foo</td><td>1<br/><table>"),res1);
        assertTrue(res0.matches("(?s).*<td>bar</td><td>2<br/><a href=..foo__bar.>bar.gt;</a></td>.*"),res0);
        assertTrue(res1.matches("(?s).*<td>bar</td><td>2<br/><a href=..foo__bar.>bar.gt;</a></td>.*"),res1);
    }
}