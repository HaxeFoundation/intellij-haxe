package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.codeInsight.intention.CommonIntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeAdditiveExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeStringLiteralExpression;
import com.intellij.plugins.haxe.model.HaxeDocumentModel;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.plugins.haxe.model.fixer.HaxeStringEscapeUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HaxeStringInterpolationUtil {

    private static final String DOUBLE_QUOTE = "\"";
    private static final String SINGE_QUOTE = "'";

    public static @NotNull CommonIntentionAction convertToInterpolationFix(HaxeAdditiveExpression additiveExpression) {
        // TODO bundle
        return new HaxeFixer("Convert to String interpolation") {
            @Override
            public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
                HaxeAdditiveExpression elementInCopy = PsiTreeUtil.findSameElementInCopy(additiveExpression, file);
                HaxeDocumentModel documentModel = new HaxeDocumentModel(editor.getDocument(), file);
                if(elementInCopy != null) {
                    execute(elementInCopy, documentModel);
                    return IntentionPreviewInfo.DIFF;
                }
                return IntentionPreviewInfo.EMPTY;
            }

            @Override
            public void run() {
                HaxeDocumentModel documentModel = HaxeDocumentModel.fromElement(additiveExpression);
                execute(additiveExpression, documentModel);
            }

            private void execute(HaxeAdditiveExpression additiveExpression, HaxeDocumentModel documentModel) {

                HaxeAdditiveExpression topMostAdditiveExpression = additiveExpression;
                while(topMostAdditiveExpression.getParent() instanceof HaxeAdditiveExpression parentAdditiveExpression) {
                    topMostAdditiveExpression = parentAdditiveExpression;
                }
                String mergedText = performInterpolation(topMostAdditiveExpression);
                documentModel.replaceElementText(topMostAdditiveExpression, SINGE_QUOTE + mergedText + SINGE_QUOTE);

            }
        };
    }

    private static String performInterpolation(HaxeExpression left, HaxeExpression right) {
        boolean leftIsString = resultIsString(left);
        boolean rightIsString = resultIsString(right);

        String leftValue = getStringFromPsiElement(left);
        String rightValue = getStringFromPsiElement(right);

        if (leftIsString && rightIsString) {
            return leftValue + rightValue;
        } else if (!leftIsString) {
            return "${" + leftValue + "}" + rightValue;
        } else {
            return leftValue + "${" + rightValue + "}";
        }
    }

    private static String getStringFromPsiElement(HaxeExpression element) {
        if(element instanceof HaxeStringLiteralExpression) return stripAndTranslateIfNecessary(element.getText());
        if(element instanceof  HaxeAdditiveExpression expression ) return performInterpolation(expression);
        return element.getText();
    }

    private static String performInterpolation(HaxeAdditiveExpression additiveExpression) {
        List<HaxeExpression> expressionList = additiveExpression.getExpressionList();
        HaxeExpression left = expressionList.getFirst();
        HaxeExpression right = expressionList.getLast();
        return performInterpolation(left, right);
    }

    public static boolean resultIsString(HaxeExpression element) {
        if (element instanceof HaxeStringLiteralExpression) return true;
        if(element instanceof HaxeAdditiveExpression additiveExpression) {
            List<HaxeExpression> expressionList = additiveExpression.getExpressionList();
            HaxeExpression left = expressionList.getFirst();
            HaxeExpression right = expressionList.getLast();
            return  resultIsString(left) || resultIsString(right);
        }
        return false;
    }
    public static boolean allElementsAreString(HaxeExpression element) {
        if (element instanceof HaxeStringLiteralExpression) return true;
        if(element instanceof HaxeAdditiveExpression additiveExpression) {
            List<HaxeExpression> expressionList = additiveExpression.getExpressionList();
            HaxeExpression left = expressionList.getFirst();
            HaxeExpression right = expressionList.getLast();
            return  allElementsAreString(left) && allElementsAreString(right);
        }
        return false;
    }

    private static String stripAndTranslateIfNecessary(String text) {
        if (text.startsWith(DOUBLE_QUOTE) && text.endsWith(DOUBLE_QUOTE)) {
            // remove quotes from string
            String withoutWrappingQuotes = stripStringWrapping(text);
            // translate to single quote
            return HaxeStringEscapeUtil.TO_SINGLE_QUOTE_TRANSLATOR.translate(withoutWrappingQuotes);
        } else if (text.startsWith(SINGE_QUOTE) && text.endsWith(SINGE_QUOTE)) {
            // remove quotes from string
            return stripStringWrapping(text);
        }

        return text;
    }

    public static @NotNull String stripStringWrapping(String text) {
        return text.substring(1, text.length() - 1);
    }

}
