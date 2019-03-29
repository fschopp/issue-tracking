package net.florianschoppmann.issuetracking.util;

import static net.florianschoppmann.issuetracking.util.StringNode.node;
import static net.florianschoppmann.issuetracking.util.StringNode.nodeOfNodes;
import static net.florianschoppmann.issuetracking.util.StringNode.rootOfNodes;
import static net.florianschoppmann.issuetracking.util.StringNode.rootOfStrings;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class StringNodeTest {
    @DataProvider
    public Object[][] dataForToStringAsTree() {
        // <body> tags are added later!
        return new Object[][] {
            {rootOfStrings("foo", "bar"), "foo,bar", "foo,bar"},
            {rootOfNodes(nodeOfNodes("foo", node("a"), node("b")), node("bar")), "foo(a,b),bar", "foo.a,foo.b,bar"},
            {rootOfNodes(nodeOfNodes("foo", nodeOfNodes("bar", node("a"), nodeOfNodes("baz", node("b")))), node("c")),
                "foo(bar(a,baz(b))),c", "foo.bar.a,foo.bar.baz.b,c"}
        };
    }

    @Test(dataProvider = "dataForToStringAsTree")
    public void toStringAsTree(StringNode stringNode, String expectedTreeString, String expectedFlattenedString) {
        Assert.assertEquals(stringNode.toStringAsTree("(", ",", ")"), expectedTreeString);
        Assert.assertEquals(stringNode.toStringFlattened(".", ","), expectedFlattenedString);
    }
}
