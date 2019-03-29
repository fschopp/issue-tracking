package net.florianschoppmann.issuetracking.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class StringNode {
    private static final String EMPTY_STRING = "";
    private final String name;
    private final List<StringNode> children;

    private StringNode(String name, Stream<StringNode> children) {
        this.name = name;
        this.children = children.collect(Collectors.toCollection(ArrayList::new));
        for (StringNode child : this.children) {
            if (child.name.isEmpty()) {
                throw new IllegalArgumentException("Cannot attach root node as child.");
            }
        }
    }

    public static StringNode rootOfStrings(String first, String... other) {
        return new StringNode(EMPTY_STRING, Stream.concat(Stream.of(first), Stream.of(other)).map(StringNode::node));
    }

    public static StringNode rootOfNodes(StringNode first, StringNode... other) {
        return new StringNode(EMPTY_STRING, Stream.concat(Stream.of(first), Stream.of(other)));
    }

    private static String requireNameNotBlank(String name) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("A string node cannot have an empty name.");
        }
        return name;
    }

    public static StringNode node(String name) {
        return new StringNode(requireNameNotBlank(name), Stream.empty());
    }

    public static StringNode nodeOfStrings(String name, String firstChild, String... otherChildren) {
        return new StringNode(requireNameNotBlank(name),
            Stream.concat(Stream.of(firstChild), Stream.of(otherChildren)).map(StringNode::node));
    }

    public static StringNode nodeOfNodes(String name, StringNode firstChild, StringNode... otherChildren) {
        return new StringNode(requireNameNotBlank(name),
            Stream.concat(Stream.of(firstChild), Stream.of(otherChildren)));
    }

    private String emptyStringIfEmptyName(String givenPrefix) {
        return name.isEmpty()
            ? EMPTY_STRING
            : givenPrefix;
    }

    private void toStringAsTreeInternal(StringBuilder stringBuilder, String prefix, String infix, String postfix) {
        stringBuilder.append(name);
        if (!children.isEmpty()) {
            boolean first = true;
            stringBuilder.append(emptyStringIfEmptyName(prefix));
            for (StringNode child : children) {
                if (first) {
                    first = false;
                } else {
                    stringBuilder.append(infix);
                }
                child.toStringAsTreeInternal(stringBuilder, prefix, infix, postfix);
            }
            stringBuilder.append(emptyStringIfEmptyName(postfix));
        }
    }

    public String toStringAsTree(String prefix, String infix, String postfix) {
        StringBuilder stringBuilder = new StringBuilder(name.length());
        toStringAsTreeInternal(stringBuilder, prefix, infix, postfix);
        return stringBuilder.toString();
    }

    private void toStringFlattenedInternal(StringBuilder stringBuilder, StringBuilder currentPrefix,
            String childSeparator, String delimiter) {
        if (children.isEmpty()) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append(delimiter);
            }
            stringBuilder.append(currentPrefix).append(name);
        } else {
            currentPrefix.append(name).append(emptyStringIfEmptyName(childSeparator));
            for (StringNode child : children) {
                child.toStringFlattenedInternal(stringBuilder, currentPrefix, childSeparator, delimiter);
            }
            currentPrefix.setLength(currentPrefix.length()
                - name.length() - emptyStringIfEmptyName(childSeparator).length());
        }
    }

    public String toStringFlattened(String childSeparator, String delimiter) {
        StringBuilder stringBuilder = new StringBuilder(children.size() * children.get(0).name.length());
        toStringFlattenedInternal(stringBuilder, new StringBuilder(EMPTY_STRING), childSeparator, delimiter);
        return stringBuilder.toString();
    }

    @Override
    public String toString() {
        return toStringAsTree("(", ",", ")");
    }
}
