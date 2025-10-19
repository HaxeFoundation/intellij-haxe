package com.intellij.plugins.haxe.ide.refactoring.move;

import com.intellij.codeInsight.editorActions.moveUpDown.LineMover;
import com.intellij.codeInsight.editorActions.moveUpDown.LineRange;
import com.intellij.codeInsight.editorActions.moveUpDown.StatementUpDownMover;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.util.Pair;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.parser.HaxeLazyWithOwner;
import com.intellij.plugins.haxe.lang.parser.HaxePsiDocCommentImpl;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement;
import com.intellij.psi.impl.source.tree.TreeUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.intellij.plugins.haxe.util.UsefulPsiTreeUtil.*;

public class HaxeMoveDeclarationHandler extends LineMover {


    @Override
    public void beforeMove(@NotNull Editor editor, @NotNull MoveInfo info, boolean down) {
        super.beforeMove(editor, info, down);
    }

    @Override
    public void afterMove(@NotNull Editor editor, @NotNull PsiFile file, @NotNull MoveInfo info, boolean down) {
        super.afterMove(editor, file, info, down);
    }


    @Override
    public boolean checkAvailable(@NotNull Editor editor, @NotNull PsiFile file, @NotNull StatementUpDownMover.MoveInfo info, boolean down) {
        boolean available = super.checkAvailable(editor, file, info, down);
        if (!available) return false;

        // TODO might want to add support for selection
        if (editor.getSelectionModel().hasSelection()) return false;

        if (file instanceof HaxeFile haxeFile) {

            TreeUtil.ensureParsed(file.getNode());

            // TODO honor minimum lines between declarations
            int lines = getMinimumLinesToKeep(info.toMove, editor, haxeFile);

            LineRange rangeIncludingDocsAndMeta = findRangeForComponent(info.toMove, editor, haxeFile);
            if (rangeIncludingDocsAndMeta == null) {
                return false;
            }

            if (down) {
                LineRange lineAfterComponent = getLineAfter(rangeIncludingDocsAndMeta);
                LineRange nextComponent = findRangeForComponent(lineAfterComponent, editor, haxeFile);
                info.toMove = rangeIncludingDocsAndMeta;
                info.toMove2 = Objects.requireNonNullElse(nextComponent, lineAfterComponent);
            } else {
                LineRange lineBeforeComponent = getLineBefore(rangeIncludingDocsAndMeta);
                if(lineBeforeComponent == null) {
                    return info.prohibitMove();
                }
                LineRange previousComponent = findRangeForComponent(lineBeforeComponent, editor, haxeFile);
                info.toMove = rangeIncludingDocsAndMeta;
                info.toMove2 = Objects.requireNonNullElse(previousComponent, lineBeforeComponent);
            }

            if(!validateMove(info)) {
                return info.prohibitMove();
            }

            return true;
        }
        return false;
    }

    private boolean validateMove(@NotNull StatementUpDownMover.MoveInfo info) {
        return  !info.toMove.contains(info.toMove2) && !info.toMove2.contains(info.toMove);
    }

    @Nullable
    private LineRange findRangeForComponent(LineRange range, Editor editor, HaxeFile file) {
        if(!valid(range)) return null;

        LineRange expanded = expandRangeToDefinition(range, editor, file);
        if (expanded == null) return null;

        HaxeNamedComponent possibleComponent = findComponent(range, editor, file);
        if (possibleComponent == null) return null;

        return expandRangeToIncludeMetadataAndDocs(editor, expanded, possibleComponent);
    }

    private boolean valid(@Nullable LineRange range) {
        if(range == null) return false;
        return range.startLine >= 0;
    }

    @Nullable
    private HaxeNamedComponent findComponent(LineRange originalRange, Editor editor, HaxeFile file) {
        LineRange expanded = expandRangeToDefinition(originalRange, editor, file);
        if (expanded == null) return null;

        int lineStartOffset = editor.getDocument().getLineStartOffset(expanded.startLine);
        PsiElement startElement = file.findElementAt(lineStartOffset);

        return findComponentOnLine(startElement, editor.getDocument(), expanded.startLine);
    }


    private int getMinimumLinesToKeep(LineRange originalRange, Editor editor, HaxeFile file) {
        CodeStyleSettings currSettings = CodeStyleSettingsManager.getSettings(file.getProject());
        CommonCodeStyleSettings commonSettings = currSettings.getCommonSettings(HaxeLanguage.INSTANCE);

        HaxeNamedComponent component = findComponent(originalRange, editor, file);
        if (component == null) return 0;
        return switch (component.getComponentType()) {
            case CLASS -> commonSettings.BLANK_LINES_AROUND_CLASS;
            case FUNCTION -> commonSettings.BLANK_LINES_AROUND_METHOD;
            case FIELD -> commonSettings.BLANK_LINES_AROUND_FIELD;
            default -> 0;
        };
    }

    private static @NotNull LineRange getLineAfter(LineRange range) {
        return new LineRange(range.endLine, range.endLine + 1);
    }

    private static @Nullable LineRange getLineBefore(LineRange range) {
        if(range.startLine == 0) return null;
        return new LineRange(range.startLine - 1, range.startLine);
    }


    @Nullable
    public static HaxeNamedComponent findComponentOnLine(@Nullable PsiElement element, @NotNull Document document, int expandedLine) {
        if (element == null) return null;
        PsiElement sibling = element;
        while (!(sibling instanceof HaxeNamedComponent component)) {
            if (sibling instanceof HaxeLazyWithOwner lazyWithOwner) {
                if (lazyWithOwner.getOwner() instanceof HaxeNamedComponent namedComponent) {
                    return namedComponent;
                }
            }
            sibling = sibling.getNextSibling() == null ? sibling.getParent() : sibling.getNextSibling();
            if (document.getLineNumber(sibling.getTextOffset()) != expandedLine) return null;
        }
        return component;
    }


    private LineRange expandRangeToIncludeMetadataAndDocs(@NotNull Editor editor, LineRange range, PsiElement component) {
        PsiElement element = component;
        PsiElement iterator = element;
        do {
            iterator = findPreviousSiblingIncludingFromParent(iterator);
            if (isMetadataOrDoc(iterator)) {
                // TODO only include 1x docs
                element = iterator;
            } else if (!isWhitespaceOrCommentButNotDocs(iterator)) {
                break;
            }
        } while (true);

        if (element == component) return range;
        LogicalPosition logicalPositionStart = editor.offsetToLogicalPosition(element.getTextOffset());
        int endLine = editor.getDocument().getLineNumber(component.getTextOffset() + component.getTextLength()) + 1;
        return new LineRange(logicalPositionStart.getLine(), endLine);
    }

    private static PsiElement findPreviousSiblingIncludingFromParent(PsiElement element) {
        PsiElement prevSibling = element.getPrevSibling();
        if (prevSibling != null) return prevSibling;
        // find previous from parent
        PsiElement parent = element.getParent();

        // if we are first child in parent, go one  level deeper
        PsiElement first = parent.getFirstChild();
        if (first == element) {
            return findPreviousSiblingIncludingFromParent(parent);
        }

        @NotNull PsiElement[] children = parent.getChildren();
        PsiElement lastChecked = parent;
        for (PsiElement child : children) {
            if (child != element) {
                lastChecked = child;
            } else {
                return lastChecked;
            }
        }
        return lastChecked;
    }

    private boolean isMetadataOrDoc(PsiElement element) {
        if (element instanceof LazyParseablePsiElement) {
            element = element.getFirstChild();
        }
        return switch (element) {
            //Use HaxePsiDocCommentImpl not HaxePsiDocComment
            case HaxePsiDocCommentImpl e -> true;
            case HaxeMeta e -> true;
            case null, default -> false;
        };
    }

    private LineRange expandRangeToDefinition(LineRange originalRange, Editor editor, HaxeFile file) {

        Pair<PsiElement, PsiElement> selectionPair = getElementRange(editor, file, originalRange);
        if (selectionPair != null) {
            PsiElement selectionPsiStart = selectionPair.getFirst();
            PsiElement selectionPsiEnd = selectionPair.getSecond();

            PsiElement definition = PsiTreeUtil.findCommonParent(selectionPsiStart, selectionPsiEnd);
            Pair<PsiElement, PsiElement> definitionPair = getElementRange(definition, selectionPsiStart, selectionPsiEnd);
            if (definitionPair != null) {

                Document document = editor.getDocument();

                PsiElement definitionPsiStart = definitionPair.getFirst();
                PsiElement definitionPsiEnd = definitionPair.getSecond();

                LogicalPosition startPos = editor.offsetToLogicalPosition(definitionPsiStart.getTextOffset());
                int startLine = Math.min(originalRange.startLine, startPos.line);

                int endLine;
                int endOffset = definitionPsiEnd.getTextRange().getEndOffset();
                if (endOffset == document.getTextLength()) {
                    endLine = document.getLineCount();
                } else {
                    endLine = editor.offsetToLogicalPosition(endOffset).line + 1;
                    endLine = Math.min(endLine, document.getLineCount());
                }
                endLine = Math.max(endLine, originalRange.endLine);

                return new LineRange(startLine, endLine);
            }

        }
        return null;
    }

}
