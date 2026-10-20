package com.intellij.plugins.haxe.lang.parser;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.lexer.HaxeDocTokenTypes;
import com.intellij.psi.TokenType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The markdown line structure of a doc comment, shared by the fence injector
 * and the inline-code annotator: content lines (past the managed indent and
 * any leading asterisk) split into prose and fenced code blocks.
 */
public final class HaxeDocMarkdown {

  // a fence marker line: three or more backticks plus an optional info string
  // (```haxe, or multi-word ```haxe linenos - the FIRST word becomes the tag);
  // backticks in the info string disqualify the marker, per CommonMark
  private static final Pattern FENCE_MARKER = Pattern.compile("`{3,}([^`]*)");

  /** One content line: absolute start offset, its text, and the newlines separating it from the previous content line. */
  public record DocLine(int startOffset, @NotNull String text, int newlinesBefore) {
  }

  /** A fenced code block: the lower-cased info tag ("" when untagged) and its content lines. */
  public record Fence(@NotNull String tag, @NotNull List<DocLine> lines) {
  }

  public record Scan(@NotNull List<DocLine> proseLines, @NotNull List<Fence> fences) {
  }

  private HaxeDocMarkdown() {
  }

  @NotNull
  public static Scan scan(@NotNull HaxePsiDocCommentImpl docComment) {
    List<DocLine> proseLines = new ArrayList<>();
    List<Fence> fences = new ArrayList<>();
    List<DocLine> fenceLines = new ArrayList<>();
    String fenceTag = null;
    for (DocLine line : contentLines(docComment)) {
      Matcher marker = FENCE_MARKER.matcher(line.text().strip());
      if (marker.matches()) {
        String info = marker.group(1).strip();
        if (fenceTag != null) {
          // a CLOSING fence is bare backticks (CommonMark); a marker carrying
          // an info string inside an open fence is content
          if (!info.isEmpty()) {
            fenceLines.add(line);
            continue;
          }
          fences.add(new Fence(fenceTag, List.copyOf(fenceLines)));
          fenceLines.clear();
          fenceTag = null;
        }
        else {
          // the tag is the info string's first whitespace-separated word
          fenceTag = info.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        }
        continue;
      }
      if (fenceTag != null) {
        fenceLines.add(line);
      }
      else {
        proseLines.add(line);
      }
    }
    // markdown runs an unclosed fence to the end of the block
    if (fenceTag != null) {
      fences.add(new Fence(fenceTag, List.copyOf(fenceLines)));
    }
    return new Scan(proseLines, fences);
  }

  @NotNull
  private static List<DocLine> contentLines(@NotNull HaxePsiDocCommentImpl docComment) {
    List<DocLine> lines = new ArrayList<>();
    int lineStart = -1;
    int newlinesBefore = 0;
    int pendingNewlines = 0;
    StringBuilder lineText = new StringBuilder();
    for (ASTNode child = docComment.getNode().getFirstChildNode(); child != null; child = child.getTreeNext()) {
      var type = child.getElementType();
      if (type == TokenType.WHITE_SPACE) {
        long newlines = child.getText().chars().filter(c -> c == '\n').count();
        if (newlines > 0) {
          if (lineStart >= 0) {
            lines.add(new DocLine(lineStart, lineText.toString(), newlinesBefore));
            lineStart = -1;
            lineText.setLength(0);
          }
          pendingNewlines += (int)newlines;
        }
        continue;
      }
      if (type == HaxeDocTokenTypes.DOC_START || type == HaxeDocTokenTypes.DOC_LEADING_ASTERISK) continue;
      if (type == HaxeDocTokenTypes.DOC_END) break;
      if (lineStart < 0) {
        lineStart = child.getStartOffset();
        newlinesBefore = pendingNewlines;
        pendingNewlines = 0;
      }
      lineText.append(child.getText());
    }
    if (lineStart >= 0) {
      lines.add(new DocLine(lineStart, lineText.toString(), newlinesBefore));
    }
    return lines;
  }
}
