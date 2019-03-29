package net.florianschoppmann.issuetracking.asana;

import net.florianschoppmann.issuetracking.util.LazyString;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

final class MarkdownBuilder {
    private final Logger log = LoggerFactory.getLogger(getClass());

    enum ListType {
        UNORDERED,
        ORDERED
    }

    private static class ListState {
        private int nextItemNumber = 0;
        private final ListType listType;

        private ListState(ListType listType) {
            this.listType = listType;
        }
    }

    private final ReferenceFactory referenceFactory;
    private final LazyString.Builder lazyStringBuilder;
    private ListState listState;
    private final Deque<ListState> listStateStack = new ArrayDeque<>();
    private @Nullable String currentLink;
    private boolean inTrivialLink = false;
    private @Nullable String lastText;

    private MarkdownBuilder(ReferenceFactory referenceFactory, int size) {
        this.referenceFactory = Objects.requireNonNull(referenceFactory);
        lazyStringBuilder = new LazyString.Builder(size);
    }

    private void enterOrExitTrivalElement(String element) {
        switch (element) {
            case "strong": lazyStringBuilder.append("**"); break;
            case "em": lazyStringBuilder.append('*'); break;
            case "u": lazyStringBuilder.append('_'); break;
            case "s": lazyStringBuilder.append("~~"); break;
            case "code": lazyStringBuilder.append('`'); break;
        }
    }

    private void noTrivialLink() {
        // An <a> element is trivial if its text is equal to the href attribute
        lazyStringBuilder.append('[');
        if (lastText != null) {
            lazyStringBuilder.append(lastText).append(' ');
            lastText = null;
        }
        inTrivialLink = false;
    }

    private void enterListItem() {
        ++listState.nextItemNumber;
        lazyStringBuilder
            .append('\n')
            .append(
                listStateStack.stream()
                    .skip(1)
                    .map(
                        state -> state.listType == ListType.UNORDERED
                            ? "  "
                            : "   "
                    )
                    .collect(Collectors.joining())
            );
        if (listState.listType == ListType.UNORDERED) {
            lazyStringBuilder.append(listStateStack.size() % 2 == 0 ? "* " : "- ");
        } else {
            lazyStringBuilder.append(listState.nextItemNumber).append(". ");
        }
    }

    private boolean shouldEnterElement(String element, Map<String, String> attributes) {
        if (inTrivialLink) {
            noTrivialLink();
        }

        switch (element) {
            case "ol":
                listState = new ListState(ListType.ORDERED);
                listStateStack.push(listState);
                break;
            case "ul":
                listState = new ListState(ListType.UNORDERED);
                listStateStack.push(listState);
                break;
            case "li":
                enterListItem();
                break;
            case "a":
                @Nullable String href = attributes.get("href");
                @Nullable String type = attributes.get("data-asana-type");
                if (type != null) {
                    lazyStringBuilder.append(
                        referenceFactory.createReference(href, type, attributes.get("data-asana-gid")));
                    return false;
                } else if (href != null) {
                    currentLink = href;
                    inTrivialLink = true;
                }
                break;
            default: enterOrExitTrivalElement(element);
        }
        return true;
    }

    private void exitElement(String element) {
        switch (element) {
            case "ol": case "ul":
                listStateStack.pop();
                if (listStateStack.isEmpty()) {
                    listState = null;
                    lazyStringBuilder.append("\n\n");
                } else {
                    listState = listStateStack.peekFirst();
                }
                break;
            case "a":
                if (currentLink != null) {
                    if (inTrivialLink) {
                        lazyStringBuilder.append(currentLink);
                        inTrivialLink = false;
                    } else {
                        lazyStringBuilder.append("](").append(currentLink).append(')');
                    }
                    currentLink = null;
                }
                break;
            default: enterOrExitTrivalElement(element);
        }
    }

    private void visitText(String text) {
        if (currentLink != null && inTrivialLink) {
            assert lastText == null : "Impossible to have two text nodes immediately after each other.";
            if (currentLink.equals(text)) {
                lastText = text;
                return;
            } else {
                noTrivialLink();
            }
        }
        lazyStringBuilder.append(text);
    }

    private LazyString toLazyString() {
        return lazyStringBuilder.build();
    }

    private static Document parse(String xml) throws SAXException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = factory.newDocumentBuilder();
            return dBuilder.parse(new InputSource(new StringReader(xml)));
        } catch (ParserConfigurationException | IOException exception) {
            throw new IllegalStateException("Encountered a bug.", exception);
        }
    }

    private static Map<String, String> attributesMap(Node node) {
        NamedNodeMap namedNodeMap = node.getAttributes();
        SortedMap<String, String> stringMap = new TreeMap<>();
        for (int i = 0, size = namedNodeMap.getLength(); i < size; ++i) {
            Attr attribute = (Attr) namedNodeMap.item(i);
            stringMap.put(attribute.getName(), attribute.getValue());
        }
        return Collections.unmodifiableSortedMap(stringMap);
    }

    private LazyString htmlToMarkdownInternal(String html, ReferenceFactory referenceFactory) {
        Objects.requireNonNull(html);
        Objects.requireNonNull(referenceFactory);

        try {
            Node parent = parse(html).getDocumentElement();
            Node node = parent.getFirstChild();
            while (node != null) {
                switch (node.getNodeType()) {
                    case Node.ELEMENT_NODE:
                        boolean shouldEnter = shouldEnterElement(node.getNodeName(), attributesMap(node));
                        if (shouldEnter) {
                            parent = node;
                            node = parent.getFirstChild();
                            continue;
                        } else {
                            break;
                        }
                    case Node.TEXT_NODE:
                        visitText(node.getTextContent());
                        break;
                }

                node = node.getNextSibling();
                while (node == null && parent.getNodeType() == Node.ELEMENT_NODE) {
                    exitElement(parent.getNodeName());
                    node = parent.getNextSibling();
                    parent = parent.getParentNode();
                }
            }
            return toLazyString();
        } catch (SAXException exception) {
            log.warn("Could not parse HTML by received from Asana.", exception);
            return LazyString.of("[parsing error]");
        }
    }

    static LazyString htmlToMarkdown(String html, ReferenceFactory referenceFactory) {
        Objects.requireNonNull(referenceFactory);
        var markdownBuilder = new MarkdownBuilder(referenceFactory, html.length());
        return markdownBuilder.htmlToMarkdownInternal(html, referenceFactory);
    }
}
