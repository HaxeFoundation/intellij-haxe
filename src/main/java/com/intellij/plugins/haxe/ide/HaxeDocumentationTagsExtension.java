package com.intellij.plugins.haxe.ide;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.parser.PostProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HaxeDocumentationTagsExtension implements Parser.ParserExtension {

    public static Extension create() {
        return new HaxeDocumentationTagsExtension();
    }

    @Override
    public void extend(Parser.Builder parserBuilder) {
        parserBuilder.postProcessor(new HaxeDocumentationTagsProcessor());
    }

}

class HaxeDocumentationTagsProcessor implements PostProcessor {

    @Override
    public Node process(Node node) {
        HaxeDocumentationTagsVisitor tagsVisitor = new HaxeDocumentationTagsVisitor();
        node.accept(tagsVisitor);
        return node;
    }
}

class HaxeDocumentationTagsVisitor extends AbstractVisitor {

    public static Pattern docTagPattern = Pattern.compile("(@\\w+)(.*)");
    public static Pattern parameterContentPattern = Pattern.compile("(\\S+)\\s+(.*)");

    public static final String TAG_SINCE = "@since";
    public static final String TAG_SEE = "@see";
    public static final String TAG_PARAM = "@param";
    public static final String TAG_RETURN = "@return";
    public static final String TAG_EVENT = "@event";
    public static final String TAG_THROWS = "@throws";

    private static final List<String> TAGS = List.of(
            TAG_SINCE,
            TAG_SEE,
            TAG_PARAM,
            TAG_RETURN,
            TAG_EVENT,
            TAG_THROWS
    );



    @Override
    public void visit(Text text) {
        super.visit(text);

        String literal = text.getLiteral();
        Node next = text.getNext();

        if (literal.trim().startsWith("@")) {
            Matcher matcher = docTagPattern.matcher(literal);
            if (matcher.find()) {
                String tag = matcher.group(1);
                String content = matcher.group(2);
                switch (tag.toLowerCase()) {
                    case TAG_THROWS -> processThrowsTag(text, tag, content);
                    case TAG_SINCE -> processSinceTag(text, tag, content);
                    case TAG_PARAM, TAG_RETURN -> processParamsTag(text, tag, content);
                    case TAG_SEE -> processSeeTag(text, tag, content);
                    case TAG_EVENT -> processEventTag(text, tag, content);
                    default -> highlightTag(text, tag, content);
                }
            }
        }
    }

    private void processSinceTag(Text text, String tag, String content) {
        // @since = "Available since $1"
        Emphasis emphasis = createEmphasis("Available since " + content);
        text.insertBefore(emphasis);
        // remove text element
        text.unlink();
    }

    private void processSeeTag(Text text, String tag, String content) {
        BulletList list = new BulletList();
        text.insertBefore(new Text("see also:"));
        text.insertBefore(list);
        lookForAndAddSeeTagsToList(list, text);
    }

    private void processParamsTag(Text text, String tag, String content) {
        TableBlock tableBlock = new TableBlock();
        text.insertBefore(tableBlock);

        // TODO  bundle ?
        createTableHeader(tableBlock, "Argument", "Description");
        lookForAndAddParameterTagsToTable(text, tableBlock);

        text.unlink();

    }

    private void processEventTag(Text text, String tag, String content) {
        TableBlock tableBlock = new TableBlock();
        text.insertBefore(tableBlock);

        // TODO  bundle ?
        createTableHeader(tableBlock, "Event", "Description");
        lookForAndAddEventTagsToTable(text, tableBlock, TAG_EVENT);

        text.unlink();
    }
    private void processThrowsTag(Text text, String tag, String content) {
        TableBlock tableBlock = new TableBlock();
        text.insertBefore(tableBlock);

        // TODO  bundle ?
        createTableHeader(tableBlock, "Exception", "Description");
        lookForAndAddEventTagsToTable(text, tableBlock, TAG_THROWS);

        text.unlink();
    }

    private void highlightTag(Text text, String tag, String content) {
        text.insertBefore(new HardLineBreak());

        var item = new StrongEmphasis();
        item.appendChild(new Text(tag));
        text.insertBefore(item);
        text.insertBefore(new Text(content));
        // remove text element
        text.unlink();
    }



    private void lookForAndAddParameterTagsToTable(Text first, TableBlock tableBlock) {

        Node next = first;
        while (next instanceof Text text) {
            Matcher matcher = docTagPattern.matcher(text.getLiteral());
            if (matcher.find()) {
                String tag = matcher.group(1);
                String content = matcher.group(2);
                if (TAG_RETURN.equalsIgnoreCase(tag)) {
                    next = findNextTagTextNode(text, TAG_PARAM);
                    TableRow row = findAndCreateReturnRow(text, content);
                    tableBlock.appendChild(row);
                    text.unlink();
                } else if (TAG_PARAM.equalsIgnoreCase(tag)) {
                    next = findNextTagTextNode(text, TAG_PARAM);
                    if(next == null) next = findNextTagTextNode(text, TAG_RETURN);
                    TableRow row = findAndCreateParameterRoEventRow(text, content);
                    tableBlock.appendChild(row);
                    text.unlink();
                } else {
                    return;
                }
            }
        }
    }

    private void lookForAndAddEventTagsToTable(Text first, TableBlock tableBlock, final String tagToFind) {
        Node next = first;
        while (next instanceof Text text) {
            Matcher matcher = docTagPattern.matcher(text.getLiteral());
            if (matcher.find()) {
                String tag = matcher.group(1);
                String content = matcher.group(2);
                if (tagToFind.equalsIgnoreCase(tag)) {
                    next = findNextTagTextNode(text, tagToFind);
                    TableRow row = findAndCreateParameterRoEventRow(text, content);
                    tableBlock.appendChild(row);
                    text.unlink();
                } else {
                    return;
                }
            }
        }
    }

    private  void lookForAndAddSeeTagsToList(BulletList list, Node node) {
        Node next = node;
        while (next instanceof Text text) {
            Matcher matcher = docTagPattern.matcher(text.getLiteral());
            if (matcher.find()) {
                String tag = matcher.group(1);
                String content = matcher.group(2);
                boolean blank = content.isBlank();
                if (TAG_SEE.equalsIgnoreCase(tag)) {
                    next = findNextTagTextNode(text, TAG_SEE);
                    ListItem item = findAndCreateListItem(text, content, blank);
                    list.appendChild(item);
                    text.unlink();
                } else {
                    return;
                }
            }
        }
    }



    private @NotNull TableRow findAndCreateReturnRow(Text text, String content) {
        TableRow parameterRow = new TableRow();

        TableCell argumentCell = new TableCell();
        TableCell descriptionCell = new TableCell();

        parameterRow.appendChild(argumentCell);
        parameterRow.appendChild(descriptionCell);

        Code paramNameText = new Code("return");
        argumentCell.appendChild(paramNameText);

        if (!content.isBlank()) {
            descriptionCell.appendChild(new Text(content));
        }
        // add any trailing content from the line with tag
        Node nextContentNode = text.getNext();
        while (nextContentNode != null
               && !(nextContentNode instanceof HardLineBreak)
               && !(nextContentNode instanceof Block)
        ) {
            // when adding node to listItem, it gets unlinked so to  have an extra reference here
            if(nextLiteralContainsTag(nextContentNode)) break;
            Node contentNode = nextContentNode;
            nextContentNode = nextContentNode.getNext();
            descriptionCell.appendChild(contentNode);
        }

        return parameterRow;
    }

    private @NotNull TableRow findAndCreateParameterRoEventRow(Text text, String content) {
        TableRow parameterRow = new TableRow();
        Matcher matcher = parameterContentPattern.matcher(content.trim());
        if (matcher.find()) {
            String parameterName = matcher.group(1);
            String parameterDescription = matcher.group(2).trim();

            TableCell argumentCell = new TableCell();
            TableCell descriptionCell = new TableCell();

            parameterRow.appendChild(argumentCell);
            parameterRow.appendChild(descriptionCell);

            Code paramNameText = new Code(parameterName);
            argumentCell.appendChild(paramNameText);

            if (!parameterDescription.isBlank()) {
                descriptionCell.appendChild(new Text(parameterDescription));
            }
            // add any trailing content from the line with tag
            Node nextContentNode = text.getNext();
            while (nextContentNode != null
                   && !(nextContentNode instanceof HardLineBreak)
                   && !(nextContentNode instanceof Block)
            ) {
                // when adding node to listItem, it gets unlinked so to  have an extra reference here
                if(nextLiteralContainsTag(nextContentNode)) break;
                Node contentNode = nextContentNode;
                nextContentNode = nextContentNode.getNext();
                descriptionCell.appendChild(contentNode);
            }

        }
        return parameterRow;
    }

    private static boolean nextLiteralContainsTag(Node nextContentNode) {
        // ignore soft linebreaks (needed when multiple tags of different types are joined together)
        if(nextContentNode instanceof SoftLineBreak) {
            nextContentNode = nextContentNode.getNext();
        }
        if(nextContentNode instanceof  Text nextText) {
            String literal = nextText.getLiteral();
            Matcher matcher1 = docTagPattern.matcher(literal);
            if(matcher1.find()) {
                String group = matcher1.group(1);
                return TAGS.contains(group);
            }
        }
        return false;
    }

    private @NotNull ListItem findAndCreateListItem(Text text, String content, boolean blank) {
        ListItem listItem = new ListItem();
        // add rest of text from text node with tag
        if (!blank) {
            listItem.appendChild(new Text(content));
        }
        // add any trailing content from the line with tag
        Node nextContentNode = text.getNext();
        while (nextContentNode != null
               && !(nextContentNode instanceof HardLineBreak)
               && !(nextContentNode instanceof Block)
        ) {
            // when adding node to listItem, it gets unlinked so to  have an extra reference here
            if(nextLiteralContainsTag(nextContentNode)) break;
            Node contentNode = nextContentNode;
            nextContentNode = nextContentNode.getNext();
            listItem.appendChild(contentNode);
        }
        return listItem;
    }


    private static void createTableHeader(TableBlock tableBlock, String... columns) {

        TableHead tableHead = new TableHead();
        TableRow tableHeader = new TableRow();
        for (String column : columns) {
            TableCell cell = new TableCell();
            cell.setHeader(true);
            cell.appendChild(new Text(column));
            tableHeader.appendChild(cell);
        }

        tableHead.appendChild(tableHeader);
        tableBlock.appendChild(tableHead);
    }

    private static Node findNextTagTextNode(Text text, String paragraphFirstTag) {
        Node nextContentNode = text.getNext();
        while (nextContentNode != null) {
            if (nextContentNode instanceof Text nextText) {
                if (paragraphFirstTag != null) {
                    if (nextText.getLiteral().startsWith(paragraphFirstTag)) {
                        return nextText;
                    }
                } else {
                    if (nextText.getLiteral().startsWith("@")) {
                        return nextText;
                    }
                }
            }

            if (nextContentNode instanceof Paragraph paragraph) {
                if (paragraphFirstTag != null) {
                    if (paragraph.getFirstChild() instanceof Text ptext) {
                        if (ptext.getLiteral().startsWith(paragraphFirstTag)) {
                            return ptext;
                        }
                    }
                }
                return null;
            }

            Node next = nextContentNode.getNext();
            if (paragraphFirstTag != null) {
                if (next == null) {
                    Node parent = nextContentNode.getParent();
                    if (parent != null && parent.getNext() instanceof Paragraph paragraph) {

                        if (paragraph.getFirstChild() instanceof Text ptext) {
                            if (ptext.getLiteral().startsWith(paragraphFirstTag)) {
                                return ptext;
                            }
                        }
                    }
                }
            }
            nextContentNode = next;

        }
        return null;
    }

    private static @NotNull Emphasis createEmphasis(String content) {
        Emphasis emphasis = new Emphasis();
        emphasis.appendChild(new Text(content));
        return emphasis;
    }
}