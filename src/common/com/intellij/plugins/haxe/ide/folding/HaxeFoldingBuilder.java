/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Ilya Malanin
 * Copyright 2018 Eric Bishton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.ide.folding;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.ide.HaxeCommenter;
import com.intellij.plugins.haxe.util.HaxeStringUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.*;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import static com.intellij.psi.TokenType.WHITE_SPACE;

public class HaxeFoldingBuilder implements FoldingBuilder {

  private static final String PLACEHOLDER_TEXT = "...";

  private static final String WS = "[ \\t]"; // Better unicode space detection: "[\\s\\p{Z}]";
  private static final RegionDefinition FD_STYLE = new RegionDefinition("^\\{"+WS+"*region"+WS+"+(.*)$",
                                                                        "^}"+WS+"?end"+WS+"*region("+WS+"*(.*))?$",
                                                                        (matcher)->matcher.group(1));
  private static final RegionDefinition PLUGIN_STYLE = new RegionDefinition("^region"+WS+"*(.*)?$",
                                                                            "^end"+WS+"*region"+WS+"*(.*)?$", (matcher)->matcher.group(1));
  private static final RegionDefinition COMMENT_REGIONS[] = { FD_STYLE, PLUGIN_STYLE };

  private static final RegionDefinition CC_REGION = new RegionDefinition("#", "#", null);


  private static class RegionDefinition {
    final Pattern begin;
    final Pattern end;
    final Function<Matcher,String> substitutor;


    public RegionDefinition(String begin, String end, Function<Matcher, String> titleExtractor) {
      this.begin = Pattern.compile(begin);
      this.end = Pattern.compile(end);
      this.substitutor = titleExtractor;
    }

    public RegionDefinition(String begin, Function<Matcher, String> titleExtractor) {
      this.begin = Pattern.compile(begin);
      this.end = this.begin;
      this.substitutor = titleExtractor;
    }

    public Matcher startMatcher(String s) {
      return begin.matcher(s);
    }

    public Matcher endMatcher(String s) {
      return end.matcher(s);
    }

    public String getPlaceholder(Matcher m) {
      if (null != substitutor) {
        return substitutor.apply(m);
      }
      return PLACEHOLDER_TEXT;
    }
  }

  private static class RegionMarker {
    public enum Type {
      START,
      END,
      CC,
      EMPTY
    }

    public static final RegionMarker EMPTY = new RegionMarker(null, null, null, Type.EMPTY);

    public final RegionDefinition region;
    public final ASTNode node;
    public final Type type;
    public final Matcher matcher;

    public RegionMarker(RegionDefinition region, ASTNode node, Matcher matcher, Type type) {
      this.region = region;
      this.node = node;
      this.type = type;
      this.matcher = matcher;
    }
  }



  @NotNull
  @Override
  public FoldingDescriptor[] buildFoldRegions(@NotNull ASTNode node, @NotNull Document document) {
    List<FoldingDescriptor> descriptorList = new ArrayList<>();
    List<RegionMarker> regionMarkers = new ArrayList<>();
    List<RegionMarker> ccMarkers = new ArrayList<>();

    buildFolding(node, descriptorList, regionMarkers, ccMarkers);

    buildMarkerRegions(descriptorList, regionMarkers);

    buildCCMarkerRegions(descriptorList, ccMarkers, document);

    FoldingDescriptor[] descriptors = new FoldingDescriptor[descriptorList.size()];
    return descriptorList.toArray(descriptors);
  }

  private static void buildFolding(@NotNull ASTNode node, List<FoldingDescriptor> descriptors,
                                   List<RegionMarker> regionMarkers,
                                   List<RegionMarker> ccMarkers) {
    final IElementType elementType = node.getElementType();

    FoldingDescriptor descriptor = null;
    if (isImportOrUsingStatement(elementType) && isFirstImportStatement(node)) {
      descriptor = buildImportsFolding(node);
    } else if (isCodeBlock(elementType)) {
      descriptor = buildCodeBlockFolding(node);
    } else if (isBodyBlock(elementType)) {
      descriptor = buildBodyBlockFolding(node);
    } else if (isComment(elementType, node)) {
       RegionMarker matched = matchRegion(node);
       if (null != matched) {
         regionMarkers.add(matched);
       }
    } else if (isCompilerConditional(elementType)) {
      RegionMarker matched = matchCCRegion(node);
      if (null != matched) {
        ccMarkers.add(matched);
      }
    }

    if (descriptor != null) {
      descriptors.add(descriptor);
    }

    for (ASTNode child : node.getChildren(null)) {
      buildFolding(child, descriptors, regionMarkers, ccMarkers);
    }
  }

  private static RegionMarker peekUpStack(List<RegionMarker> list, int n) {
    int current = 0;
    for (RegionMarker marker : list) {
      if (current == n) {
        return marker;
      }
      current++;
    }
    return null;
  }

  private static void buildMarkerRegions(List<FoldingDescriptor> descriptors, List<RegionMarker> regionMarkers) {
    // The list should be a set of balanced nested markers.  If they are unbalanced, we simply don't
    // create the descriptor.

    LinkedList<RegionMarker> startStack = new LinkedList<>();

    for (RegionMarker current : regionMarkers) {
      if (current.type == RegionMarker.Type.START) {
        startStack.push(current);
      } else {
        // TODO: Use a 'diff' algorithm to determine the best sequence?? See Google's DiffUtils package.

        // current matches with whatever is on top of the stack.
        if (startStack.isEmpty()) {
          // Unbalanced END.  // TODO: Mark it as an error in the code??  Can we do that here?
          continue;
        }
        // TODO: Maybe check if the end has a title, and if it does, use that to match to the start as well ??
        RegionMarker start = startStack.peek();
        if (start.region != current.region) {
          // Unbalanced start/end types.

          // Until we can get a better algorithm, we'll use a simple heuristic to see if maybe it's an unbalanced start.
          RegionMarker nextStart = peekUpStack(startStack, 1);
          if (null != nextStart && nextStart.region == current.region) {
            startStack.pop(); // Unbalanced start, so pop one of those
            start = startStack.peek();
          } else {
            continue;
          }
        }

        startStack.pop();
        descriptors.add(buildRegionFolding(start, current));
      }
    }
  }

  private static int lineNumber(ASTNode node, Document document) {
    return document.getLineNumber(node.getStartOffset());
  }

  private static void buildCCRegion(List<FoldingDescriptor> descriptors, RegionMarker start, RegionMarker end, Document document) {
    if (lineNumber(start.node, document) < lineNumber(end.node, document)) {
      descriptors.add(buildCCFolding(start, end));
    }
  }

  private static void buildCCMarkerRegions(List<FoldingDescriptor> descriptors, List<RegionMarker> regionMarkers, Document document) {
    LinkedList<RegionMarker> interruptedStack = new LinkedList<>();

    RegionMarker inProcess = null;

    for (RegionMarker marker : regionMarkers) {
      IElementType type = marker.node.getElementType();

      if (PPEND == type) {
        if (null != inProcess) {
          buildCCRegion(descriptors, inProcess, marker, document);
        }
        if (!interruptedStack.isEmpty()) {
          inProcess = interruptedStack.pop();
        }
      } else {
        // No matter where we start (e.g. #else instead of #if), we just go to the next marker...
        if (null == inProcess) {
          inProcess = marker;
        }
        else {
          // #if introduces a new level.  Other kinds do not.
          if (PPIF == type) {
            interruptedStack.push(inProcess);
            inProcess = marker;
          }
          else {
            buildCCRegion(descriptors, inProcess, marker, document);
            inProcess = marker;
          }
        }
      }
    }
  }

  private static boolean isCodeBlock(IElementType elementType) {
    return elementType == BLOCK_STATEMENT;
  }

  private static boolean isBodyBlock(IElementType elementType) {
    return BODY_TYPES.contains(elementType);
  }

  private static boolean isComment(IElementType elementType, ASTNode node) {
    return ONLY_COMMENTS.contains(elementType);
  }

  private static boolean isCompilerConditional(IElementType elementType) {
    return ONLY_CC_DIRECTIVES.contains(elementType);
  }

  private static String stripComment(IElementType elementType, String text) {
    if (elementType == MSL_COMMENT) {
      return strip(text, HaxeCommenter.SINGLE_LINE_PREFIX, null);
    }
    if (elementType == MML_COMMENT) {
      text = HaxeStringUtil.terminateAt(text, '\n');
      return strip(text.trim(), HaxeCommenter.BLOCK_COMMENT_PREFIX, HaxeCommenter.BLOCK_COMMENT_SUFFIX);
    }
    if (elementType == DOC_COMMENT) {
      text = HaxeStringUtil.terminateAt(text, '\n');
      return strip(text, HaxeCommenter.DOC_COMMENT_PREFIX, HaxeCommenter.DOC_COMMENT_SUFFIX);
    }
    return text.trim();
  }

  private static String strip(@NotNull String text, @Nullable String prefix, @Nullable String suffix) {
    String stripped = text.trim();
    if (null != prefix) stripped = HaxeStringUtil.stripPrefix(stripped, prefix);
    if (null != suffix) stripped = HaxeStringUtil.stripSuffix(stripped, suffix);
    return stripped.trim();
  }

  private static RegionMarker matchRegion(ASTNode node) {
    if (null == node) {
      return null;
    }

    String text = stripComment(node.getElementType(), node.getText());
    if (null == text || text.isEmpty()) {
      return null;
    }

    // Figure out if it matches any of the region patterns.
    for (RegionDefinition region : COMMENT_REGIONS) {
      Matcher start = region.startMatcher(text);
      if (start.matches()) {
        return new RegionMarker(region, node, start, RegionMarker.Type.START);
      }
      Matcher end = region.endMatcher(text);
      if (end.matches()) {
        return new RegionMarker(region, node, end, RegionMarker.Type.END);
      }
    }
    return null;
  }

  /**
   * Gather the CC and its expression (if any) as the placeholder.
   *
   * @param node The Compiler Conditional node.
   * @return a placeholder string for code folding.
   */
  @NotNull
  private static String getCCPlaceholder(ASTNode node) {
    StringBuilder s = new StringBuilder();
    s.append(node.getText());
    ASTNode next = node.getTreeNext();
    while (null != next && next.getElementType() == PPEXPRESSION) {
      s.append(next.getText());
      next = next.getTreeNext();
    }
    if (0 != s.length()) {
      s.append(' ');
    }
    String placeholder = s.toString();
    return placeholder.isEmpty() ? "compiler conditional" : placeholder;
  }

  private static RegionMarker matchCCRegion(ASTNode node) {
    return new RegionMarker(CC_REGION, node, null, RegionMarker.Type.CC);
  }

  private static FoldingDescriptor buildCodeBlockFolding(@NotNull ASTNode node) {
    final ASTNode openBrace = node.getFirstChildNode();
    final ASTNode closeBrace = node.getLastChildNode();

    return buildBlockFolding(node, openBrace, closeBrace);
  }

  private static FoldingDescriptor buildBodyBlockFolding(@NotNull ASTNode node) {
    final ASTNode openBrace = UsefulPsiTreeUtil.getPrevSiblingSkipWhiteSpacesAndComments(node);
    final ASTNode closeBrace = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(node);

    return buildBlockFolding(node, openBrace, closeBrace);
  }

  private static FoldingDescriptor buildBlockFolding(@NotNull ASTNode node, ASTNode openBrace, ASTNode closeBrace) {
    TextRange textRange;
    if (openBrace != null && closeBrace != null && openBrace.getElementType() == PLCURLY && closeBrace.getElementType() == PRCURLY) {
      textRange = new TextRange(openBrace.getTextRange().getEndOffset(), closeBrace.getStartOffset());
    } else {
      textRange = node.getTextRange();
    }

    if (textRange.getLength() > PLACEHOLDER_TEXT.length()) {
      return new FoldingDescriptor(node, textRange);
    }

    return null;
  }

  private static FoldingDescriptor buildRegionFolding(final RegionMarker start, final RegionMarker end) {
    TextRange range = new TextRange(start.node.getStartOffset(), end.node.getTextRange().getEndOffset());

    return new FoldingDescriptor(start.node, range) {
      @Nullable
      @Override
      public String getPlaceholderText() {
        String s = start.region.getPlaceholder(start.matcher);
        return null == s ? PLACEHOLDER_TEXT : s;
      }
    };
  }

  private static FoldingDescriptor buildCCFolding(final RegionMarker start, final RegionMarker end) {
    TextRange range = new TextRange(start.node.getStartOffset(), end.node.getTextRange().getStartOffset());

    final String placeholder = getCCPlaceholder(start.node);

    return new FoldingDescriptor(start.node, range) {
      @Nullable
      @Override
      public String getPlaceholderText() {
        return placeholder;
      }
    };
  }

  private static FoldingDescriptor buildImportsFolding(@NotNull ASTNode node) {
    final ASTNode lastImportNode = findLastImportNode(node);
    if (!node.equals(lastImportNode)) {
      return new FoldingDescriptor(node, buildImportsFoldingTextRange(node, lastImportNode));
    }
    return null;
  }

  private static TextRange buildImportsFoldingTextRange(ASTNode firstNode, ASTNode lastNode) {
    ASTNode nodeStartFrom = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(firstNode.getFirstChildNode());
    if (nodeStartFrom == null) {
      nodeStartFrom = firstNode;
    }
    return new TextRange(nodeStartFrom.getStartOffset(), lastNode.getTextRange().getEndOffset());
  }

  private static boolean isFirstImportStatement(@NotNull ASTNode node) {
    ASTNode prevNode = UsefulPsiTreeUtil.getPrevSiblingSkipWhiteSpacesAndComments(node);
    return prevNode == null || !isImportOrUsingStatement(prevNode.getElementType());
  }

  private static ASTNode findLastImportNode(@NotNull ASTNode node) {
    ASTNode lastImportNode = node;
    ASTNode nextNode = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(node);
    while (nextNode != null && isImportOrUsingStatement(nextNode.getElementType())) {
      lastImportNode = nextNode;
      nextNode = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(nextNode);
    }
    if (lastImportNode.getElementType() == WHITE_SPACE) {
      lastImportNode = lastImportNode.getTreePrev();
    }
    return lastImportNode;
  }

  private static boolean isImportOrUsingStatement(IElementType type) {
    return type == IMPORT_STATEMENT || type == USING_STATEMENT;
  }

  @Nullable
  @Override
  public String getPlaceholderText(@NotNull ASTNode node) {
    return PLACEHOLDER_TEXT;
  }

  @Override
  public boolean isCollapsedByDefault(@NotNull ASTNode node) {
    return false;
  }
}
