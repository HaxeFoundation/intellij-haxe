package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.codeInsight.intention.CommonIntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeAdditiveExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeStringLiteralExpression;
import com.intellij.plugins.haxe.model.HaxeDocumentModel;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.plugins.haxe.model.fixer.HaxeStringEscapeUtil;
import com.intellij.plugins.haxe.model.fixer.HaxeSurroundFixer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.intellij.plugins.haxe.ide.annotator.HaxeSemanticAnnotatorInspections.STRING_INTERPOLATION_QUOTE_CHECK;
import static com.intellij.plugins.haxe.ide.annotator.semantics.HaxeStringInterpolationUtil.convertToInterpolationFix;
import static com.intellij.plugins.haxe.ide.annotator.semantics.HaxeStringInterpolationUtil.stripStringWrapping;
import static com.intellij.plugins.haxe.ide.annotator.semantics.HaxeStringTemplateUtils.*;


public class HaxeStringAnnotator implements Annotator, DumbAware {



  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if(!element.isValid()) return;

    if (element instanceof HaxeStringLiteralExpression stringLiteral) {
      check(stringLiteral, holder);
    }
    if(element instanceof HaxeAdditiveExpression additiveExpression) {
        checkInterpolation(additiveExpression, holder);
    }
  }


  public void check(HaxeStringLiteralExpression psi, AnnotationHolder holder) {
    if (!STRING_INTERPOLATION_QUOTE_CHECK.isEnabled(psi)) return;

    if (isSingleQuotesRequired(psi)) {
      holder.newAnnotation(HighlightSeverity.WARNING,
                           HaxeBundle.message(
                             "haxe.semantic.inspection.message.expression.that.contains.string.interpolation.should.be.wrapped.with.single.quotes"))
        .withFix(HaxeSurroundFixer.replaceQuotesWithSingleQuotes(psi))
        .create();
    }
  }


  private static boolean templateMatches(String text, Pattern pattern) {
    // We need an odd number of dollar signs to avoid detecting escaped dollar signs as templates.
    Matcher matcher = pattern.matcher(text);
    while (matcher.find()) {
      if (matcher.groupCount() == 1 && (matcher.end(1) - matcher.start(1)) % 2 != 0) {
        return true;
      }
    }
    return false;
  }

  private void checkInterpolation(HaxeAdditiveExpression additiveExpression, @NotNull AnnotationHolder holder) {
    List<HaxeExpression> expressionList = additiveExpression.getExpressionList();
    boolean containsString = expressionList.stream().anyMatch(HaxeStringLiteralExpression.class::isInstance);
    boolean allStrings = expressionList.stream().allMatch(HaxeStringInterpolationUtil::allElementsAreString);


    if(containsString && !allStrings) {
      // suggest interpolation
      holder.newAnnotation(HighlightSeverity.INFORMATION, HaxeBundle.message("haxe.semantic.convert.to.string.interpolation"))
              .range(additiveExpression)
              .withFix(convertToInterpolationFix(additiveExpression))
              .create();

    }else if(allStrings) {
      holder.newAnnotation(HighlightSeverity.INFORMATION, HaxeBundle.message("haxe.semantic.merge.concatenated.strings"))
              .range(additiveExpression)
              .withFix(mergeStringsFix(additiveExpression))
              .create();
    }
  }

  private @NotNull CommonIntentionAction mergeStringsFix(HaxeAdditiveExpression additiveExpression) {
    return new HaxeFixer(HaxeBundle.message("haxe.semantic.merge.concatenated.strings")) {

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
          if(parentAdditiveExpression.getExpressionList().stream().allMatch(HaxeStringInterpolationUtil::resultIsString)) {
            topMostAdditiveExpression = parentAdditiveExpression;
          }else {
            break;
          }
        }
        boolean onlyDoubleQuotes = onlyDoubleQuoteStrings(topMostAdditiveExpression);
        String merged = performMerge(topMostAdditiveExpression, onlyDoubleQuotes);

        if(onlyDoubleQuotes) {
          documentModel.replaceElementText(topMostAdditiveExpression, DOUBLE_QUOTE + merged+ DOUBLE_QUOTE);
        }else {
          documentModel.replaceElementText(topMostAdditiveExpression, SINGE_QUOTE + merged+ SINGE_QUOTE);
        }
      }
    };
  }



  private String performMerge(HaxeAdditiveExpression additiveExpression, boolean onlyDoubleQuotes) {
    List<HaxeExpression> expressionList = additiveExpression.getExpressionList();
    HaxeExpression left = expressionList.getFirst();
    HaxeExpression right = expressionList.getLast();
    return performMerge(left, right, onlyDoubleQuotes);
  }

  private String performMerge(HaxeExpression left, HaxeExpression right, boolean onlyDoubleQuotes) {

    String leftText = getStringValue(left, onlyDoubleQuotes);
    String rightText = getStringValue(right, onlyDoubleQuotes);

    if(!onlyDoubleQuotes) {
      if (left instanceof HaxeStringLiteralExpression && left.getText().startsWith(DOUBLE_QUOTE)) {
        leftText = HaxeStringEscapeUtil.TO_SINGLE_QUOTE_TRANSLATOR.translate(leftText);
      }
      if (right instanceof HaxeStringLiteralExpression && right.getText().startsWith(DOUBLE_QUOTE)) {
        rightText = HaxeStringEscapeUtil.TO_SINGLE_QUOTE_TRANSLATOR.translate(rightText);
      }
    }

    return leftText + rightText;
  }

  private String getStringValue(HaxeExpression element, boolean onlyDoubleQuotes) {
    if(element instanceof HaxeAdditiveExpression additiveExpression) {
     return performMerge(additiveExpression, onlyDoubleQuotes);
    }else if (element instanceof HaxeStringLiteralExpression){
      return stripStringWrapping(element.getText());
    }else {
      return element.getText();
    }
  }


  private static boolean onlyDoubleQuoteStrings(HaxeExpression element) {
    if (element instanceof HaxeStringLiteralExpression literalExpression) {
      return literalExpression.getText().startsWith(DOUBLE_QUOTE);
    }
    if(element instanceof HaxeAdditiveExpression additiveExpression) {
      List<HaxeExpression> expressionList = additiveExpression.getExpressionList();
      HaxeExpression left = expressionList.getFirst();
      HaxeExpression right = expressionList.getLast();
      return onlyDoubleQuoteStrings(left) || onlyDoubleQuoteStrings(right);
    }
    return false;
  }


}
