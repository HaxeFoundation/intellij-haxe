package com.intellij.plugins.haxe.ide.lookup.indexed;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.TailType;
import com.intellij.codeInsight.TailTypes;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.util.CompletionStyleUtil;
import com.intellij.codeInsight.completion.util.ParenthesesInsertHandler;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupItem;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;

public class HaxelookupInsertUtil {
   public  static PsiElement insertParentheses(@NotNull InsertionContext context,
                                        @NotNull LookupElement item,
                                        boolean hasParams,
                                        boolean forceClosingParenthesis) {
        Editor editor = context.getEditor();
        char completionChar = context.getCompletionChar();
        PsiFile file = context.getFile();

        TailType tailType = completionChar == '(' ? TailTypes.noneType() :
                completionChar == ':' ? TailTypes.conditionalExpressionColonType() :
                        LookupItem.handleCompletionChar(context.getEditor(), item, completionChar);
        boolean hasTail = tailType != TailTypes.noneType() && tailType != TailTypes.unknownType();
        boolean smart = completionChar == Lookup.COMPLETE_STATEMENT_SELECT_CHAR;

        if (completionChar == '(' || completionChar == '.' || completionChar == ',' || completionChar == ';' || completionChar == ':' || completionChar == ' ') {
            context.setAddCompletionChar(false);
        }

        if (hasTail) {
            hasParams = false;
        }
        boolean needRightParenth = forceClosingParenthesis ||
                !smart && (CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET ||
                        !hasParams && completionChar != '(');

        context.commitDocument();

        CommonCodeStyleSettings styleSettings = CompletionStyleUtil.getCodeStyleSettings(context);
        PsiElement elementAt = file.findElementAt(context.getStartOffset());
        if (elementAt == null || !(elementAt.getParent() instanceof PsiMethodReferenceExpression)) {
            boolean hasParameters = hasParams;
            boolean spaceBetweenParentheses = hasParams && styleSettings.SPACE_WITHIN_METHOD_CALL_PARENTHESES;
            new ParenthesesInsertHandler<>(styleSettings.SPACE_BEFORE_METHOD_CALL_PARENTHESES, spaceBetweenParentheses,
                    needRightParenth, styleSettings.METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE) {
                @Override
                protected boolean placeCaretInsideParentheses(InsertionContext context1, LookupElement item1) {
                    return hasParameters;
                }

                @Override
                protected PsiElement findExistingLeftParenthesis(@NotNull InsertionContext context) {
                    PsiElement token = super.findExistingLeftParenthesis(context);
                    return isPartOfLambda(token) ? null : token;
                }

                private static boolean isPartOfLambda(PsiElement token) {
                    return token != null && token.getParent() instanceof PsiExpressionList &&
                            PsiUtilCore.getElementType(PsiTreeUtil.nextVisibleLeaf(token.getParent())) == JavaTokenType.ARROW;
                }
            }.handleInsert(context, item);
        }
        return elementAt;
    }
}
