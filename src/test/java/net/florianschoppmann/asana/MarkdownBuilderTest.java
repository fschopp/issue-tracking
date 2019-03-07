package net.florianschoppmann.asana;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class MarkdownBuilderTest {
    @Test
    public void asanaDomToMarkdownNull() {
        Assert.expectThrows(NullPointerException.class,
            () -> MarkdownBuilder.htmlToMarkdown(null, AsanaReference::new)
        );
        Assert.expectThrows(NullPointerException.class,
            () -> MarkdownBuilder.htmlToMarkdown("<body>foo</body>", null)
        );
    }

    private static String stringFromAsanaReference(Object object) {
        return object instanceof AsanaReference
            ? "P-1"
            : String.valueOf(object);
    }

    @DataProvider
    public Object[][] dataForAsanaDomToMarkdown() {
        // <body> tags are added later!
        return new Object[][] {
            {"foo", "foo"},
            {"This is <strong>bold</strong>, <em>italic</em>, <u>unterlined</u>, <s>strikethrough</s>, "
                + "<code>code</code>!", "This is **bold**, *italic*, _unterlined_, ~~strikethrough~~, `code`!"},
            {"&quot;", "\""},
            {"A list:\n\n<ul><li>Foo</li><li>Bar<ul><li>Wow</li></ul></li></ul> Baz",
                "A list:\n\n\n- Foo\n- Bar\n  * Wow\n\n Baz"},
            {"<ol><li>foo<ul><li>bar<ol><li>baz</li></ol></li></ul></li></ol>", "\n1. foo\n   * bar\n     1. baz\n\n"},
            {"<a>foo</a>", "foo"},
            {"<a href=\"http://127.0.0.1\">Write code</a>", "[Write code](http://127.0.0.1)"},
            {"<a href=\"http://127.0.0.1\">http://127.0.0.1</a>", "http://127.0.0.1"},
            {"<a href=\"http://127.0.0.1\">http://127.0.0.1<em>!</em></a>", "[http://127.0.0.1 *!*](http://127.0.0.1)"},
            {"<a data-asana-type=\"task\" data-asana-gid=\"1\">http://127.0.0.1</a>", "P-1"},
            {"<a data-asana-type=\"task\" data-asana-gid=\"1\" href=\"http://127.0.0.1\">http://127.0.0.1</a>", "P-1"}
        };
    }

    @Test(dataProvider = "dataForAsanaDomToMarkdown")
    public void asanaDomToMarkdown(String html, String markdown) {
        Assert.assertEquals(
            MarkdownBuilder.htmlToMarkdown(
                "<body>" + html + "</body>",
                AsanaReference::new
            ).toStringWithContext(MarkdownBuilderTest::stringFromAsanaReference),
            markdown
        );
    }
}
