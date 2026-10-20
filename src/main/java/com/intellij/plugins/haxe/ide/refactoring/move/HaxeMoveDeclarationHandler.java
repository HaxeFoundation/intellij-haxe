package com.intellij.plugins.haxe.ide.refactoring.move;

import com.intellij.codeInsight.editorActions.moveUpDown.LineRange;
import com.intellij.codeInsight.editorActions.moveUpDown.StatementUpDownMover;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.parser.HaxePsiDocCommentImpl;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.psi.*;
import com.intellij.application.options.CodeStyle;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement;
import com.intellij.psi.impl.source.tree.TreeUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.intellij.plugins.haxe.util.UsefulPsiTreeUtil.*;

public class HaxeMoveDeclarationHandler extends HaxeLineMover {

    @Override
    public boolean checkAvailable(@NotNull Editor editor, @NotNull PsiFile file, @NotNull StatementUpDownMover.MoveInfo info, boolean down) {
        boolean available = super.checkAvailable(editor, file, info, down);
        if (!available) return false;

        // TODO might want to add support for selection
        if (editor.getSelectionModel().hasSelection()) return false;

        if (file instanceof HaxeFile haxeFile) {

            TreeUtil.ensureParsed(file.getNode());

            // TODO honor minimum lines between declarations
            LineRange firstRange = info.toMove;
            LineRange secondRange = info.toMove2;

            int lines = getMinimumLinesToKeep(firstRange, editor, haxeFile);

            LineRange rangeIncludingDocsAndMeta = findRangeForComponent(firstRange, editor, haxeFile);
            if (rangeIncludingDocsAndMeta == null) {
                return false;
            }


            if (down) {
                LineRange lineAfterComponent = getLineAfter(rangeIncludingDocsAndMeta);
                LineRange nextComponent = findRangeForComponent(lineAfterComponent, editor, haxeFile);

                firstRange = rangeIncludingDocsAndMeta;
                if(nextComponent != null && nextComponent.startLine >= lineAfterComponent.startLine) {
                    secondRange = nextComponent;
                }else {
                    // TODO validate not changing scope (one problem is multi-line metadata)
                    secondRange = lineAfterComponent;
                }
            } else {
                LineRange lineBeforeComponent = getLineBefore(rangeIncludingDocsAndMeta);
                if(lineBeforeComponent == null) {
                    return info.prohibitMove();
                }
                LineRange previousComponent = findRangeForComponent(lineBeforeComponent, editor, haxeFile);
                firstRange= rangeIncludingDocsAndMeta;
                secondRange = Objects.requireNonNullElse(previousComponent, lineBeforeComponent);
            }

            if(!moveInSameScope(editor, firstRange, secondRange, haxeFile)){
                return info.prohibitMove();
            }

            if(!validateMove(firstRange, secondRange)) {
                return info.prohibitMove();
            }else {
                info.toMove = firstRange;
                info.toMove2 = secondRange;
            }

            return true;
        }
        return false;
    }

    private boolean moveInSameScope(@NotNull Editor editor,LineRange firstRange , LineRange secondRange,  HaxeFile haxeFile) {
        HaxeNamedComponent source = findComponent(firstRange, editor, haxeFile);
        HaxeNamedComponent target = findComponent(secondRange, editor, haxeFile);
        if(target == null || source == null) return true;
        if (PsiTreeUtil.isAncestor(source, target, true)) return false;
        if (PsiTreeUtil.isAncestor(target, source, true)) return false;
        return true;
    }


    @Nullable
    private LineRange findRangeForComponent(LineRange range, Editor editor, HaxeFile file) {
        if(!validRange(range)) return null;

        // if our line is an expression in a codeblock inside a component we don't want to move the component
        PsiElement expression = findExpression(range, editor, file, true);
        if (expression != null && this.parentIsCodeBlock(expression) ) {
            return null;
        }

        LineRange expanded = expandRangeToDefinition(range, editor, file);
        if (expanded == null) return null;

        HaxeNamedComponent possibleComponent = findComponent(range, editor, file);
        if (possibleComponent == null) return null;

        return expandRangeToIncludeMetadataAndDocs(editor, expanded, possibleComponent);
    }


    private int getMinimumLinesToKeep(LineRange originalRange, Editor editor, HaxeFile file) {
        CodeStyleSettings currSettings = CodeStyle.getSettings(file);
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

    private LineRange expandRangeToIncludeMetadataAndDocs(@NotNull Editor editor, LineRange range, PsiElement component) {
        PsiElement element = component;
        PsiElement iterator = element;
        do {
            iterator = findPreviousSiblingIncludingFromParent(iterator);
            if(iterator == element) break; // prevent eternal loop when on beginning of file
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
        if(parent instanceof PsiFile) return first;
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
        //important: Use HaxePsiDocCommentImpl not HaxePsiDocComment
        if (element instanceof HaxePsiDocCommentImpl) return true;
        // the embedded-meta chameleon wraps its HaxeMeta child
        if (element instanceof LazyParseablePsiElement lazy) {
            element = lazy.getFirstChild();
        }
        return element instanceof HaxeMeta;
    }


}
