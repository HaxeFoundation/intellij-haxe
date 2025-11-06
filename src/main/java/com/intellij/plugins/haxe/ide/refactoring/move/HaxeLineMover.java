package com.intellij.plugins.haxe.ide.refactoring.move;

import com.intellij.codeInsight.editorActions.moveUpDown.LineMover;
import com.intellij.codeInsight.editorActions.moveUpDown.LineRange;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.util.Pair;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.parser.HaxeLazyWithOwner;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.plugins.haxe.util.UsefulPsiTreeUtil.isWhitespaceOrComment;

public abstract class HaxeLineMover extends LineMover {

    protected @NotNull LineRange getLineAfter(LineRange range) {
        return new LineRange(range.endLine, range.endLine + 1);
    }

    protected @Nullable LineRange getLineBefore(LineRange range) {
        if(range.startLine == 0) return null;
        return new LineRange(range.startLine - 1, range.startLine);
    }

    protected boolean validateMove(LineRange firstRange, LineRange secondRange) {
        if(firstRange == null || secondRange == null) return false;
        return  !firstRange.contains(secondRange) && !secondRange.contains(firstRange);
    }

    protected boolean validRange(@Nullable LineRange range) {
        if (range == null) return false;
        return range.startLine >= 0;
    }


    @Nullable
    protected PsiElement findExpression(LineRange range, Editor editor, HaxeFile file, boolean start) {
        int lineStartOffset = editor.getDocument().getLineStartOffset(start ? range.startLine : range.endLine);
        if(!start) lineStartOffset--;
        PsiElement elementAt = file.findElementAt(lineStartOffset);

        if(isWhitespaceOrComment(elementAt)) {
            if(start) {
                elementAt = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(elementAt);
            }else {
                elementAt = UsefulPsiTreeUtil.getPrevSiblingSkipWhiteSpacesAndComments(elementAt);
                // getting last psi on line, need to skip "invisible scopes" like switchCase
                if(elementAt instanceof HaxeSwitchCase switchCase) {
                    @NotNull PsiElement[] children = switchCase.getChildren();
                    if(children.length>0) {
                        elementAt = children[children.length - 1];
                    }
                }
            }
        }
        // if token (TODO limit to scope and array)
        if(elementAt instanceof HaxePsiToken token) {
            if(token.getTokenType() == HaxeTokenTypes.OSEMI) {
                elementAt = elementAt.getPrevSibling();
            }else {
                elementAt = elementAt.getParent();
            }
        } else if(elementAt instanceof HaxeSwitchCaseBlock) {
            // HaxeSwitchCaseBlock is an extra psi tree level that we simply skip
            elementAt = elementAt.getFirstChild();
        } else if(elementAt instanceof HaxeBlockStatement) {
            elementAt = elementAt.getParent();
        }
        return elementAt;
    }

    protected boolean parentIsCodeBlock(@NotNull PsiElement expression) {
        PsiElement parent = expression.getParent();
        return switch (parent) {
            case HaxeBlockStatement ignored -> true;
            case HaxeSwitchBlock ignored -> true;
            case null, default -> false;
        };
    }


    @Nullable
    protected HaxeNamedComponent findComponent(LineRange originalRange, Editor editor, HaxeFile file) {
        LineRange expanded = expandRangeToDefinition(originalRange, editor, file);
        if (expanded == null) return null;

        int lineStartOffset = editor.getDocument().getLineStartOffset(expanded.startLine);
        PsiElement startElement = file.findElementAt(lineStartOffset);

        return findComponentOnLine(startElement, editor.getDocument(), expanded.startLine);
    }

    @Nullable
    protected HaxeNamedComponent findComponentOnLine(@Nullable PsiElement element, @NotNull Document document, int expandedLine) {
        if (element == null) return null;
        PsiElement sibling = element;
        while (!(sibling instanceof HaxeNamedComponent component)) {
            if (sibling instanceof HaxeLazyWithOwner lazyWithOwner) {
                if (lazyWithOwner.getOwner() instanceof HaxeNamedComponent namedComponent) {
                    return namedComponent;
                }
            }
            sibling = sibling.getNextSibling() == null ? sibling.getParent() : sibling.getNextSibling();
            int lineNumber = document.getLineNumber(sibling.getTextOffset());
            if (lineNumber != expandedLine) return null;
        }
        return component;
    }



    protected LineRange expandRangeToDefinition(LineRange originalRange, Editor editor, HaxeFile file) {

        Pair<PsiElement, PsiElement> selectionPair = getElementRange(editor, file, originalRange);
        if (selectionPair != null) {
            PsiElement selectionPsiStart = selectionPair.getFirst();
            PsiElement selectionPsiEnd = selectionPair.getSecond();

            // haxe spesific fix for getElementRange, it returns HaxeModule as end element for some module declarations
            // this can in some cases expand the range to include other members
            if(PsiTreeUtil.isAncestor(selectionPsiEnd, selectionPsiStart, true)) {
                @NotNull PsiElement[] children = selectionPsiEnd.getChildren();
                for (PsiElement child : children) {
                    if(PsiTreeUtil.isAncestor(child, selectionPsiStart, false)) {
                        PsiElement commonContext = PsiTreeUtil.findCommonContext(child, selectionPsiStart);
                        if(commonContext != null) {
                            selectionPsiEnd = commonContext;
                        }
                    }
                }
            }

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

    @Nullable
    protected static Pair<PsiElement, PsiElement> getElementRange(@NotNull Editor editor, @NotNull PsiFile file, @NotNull LineRange range) {

        int startOffset = editor.logicalPositionToOffset(new LogicalPosition(range.startLine, 0));
        PsiElement startingElement = firstNonWhiteElement(startOffset, file, true);
        if (startingElement == null) {
            return null;
        } else {
            int endOffset = editor.logicalPositionToOffset(new LogicalPosition(range.endLine, 0)) - 1;
            PsiElement endingElement = firstNonWhiteElement(endOffset, file, false);
            // HACK for haxe switch-case code blocks. They don't have any token to show end of a block
            // and so the last offset in a block will always be expanded to the entire block
            if(endingElement instanceof HaxeSwitchCase && !(startingElement instanceof HaxeSwitchCase)) {
                endingElement =  firstNonWhiteElement(endOffset-1, file, false);
            }
            if(startingElement instanceof HaxeSwitchCaseBlock block) {
                startingElement = block.getFirstChild();
            }
            if (endingElement == null) {
                return null;
            } else if (!PsiTreeUtil.isAncestor(startingElement, endingElement, false) && startingElement.getTextRange().getEndOffset() > endingElement.getTextRange().getStartOffset()) {
                return PsiTreeUtil.isAncestor(endingElement, startingElement, false) ? Pair.create(startingElement, endingElement) : null;
            } else {
                return Pair.create(startingElement, endingElement);
            }
        }
    }

    protected LineRange expandRangeToExpression(LineRange originalRange, Editor editor, HaxeFile file) {

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
        return originalRange;
    }

}
