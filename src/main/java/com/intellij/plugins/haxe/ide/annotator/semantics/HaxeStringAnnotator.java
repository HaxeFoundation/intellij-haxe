package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.project.DumbAware;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeStringLiteralExpression;
import com.intellij.plugins.haxe.model.fixer.HaxeSurroundFixer;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.intellij.plugins.haxe.ide.annotator.HaxeSemanticAnnotatorInspections.STRING_INTERPOLATION_QUOTE_CHECK;


public class HaxeStringAnnotator implements Annotator, DumbAware {

  // These patterns are designed to find likely string templates; they are not intended to be exhaustive.
  private static final Pattern shortTemplate = Pattern.compile("(\\$+)\\w+");
  private static final Pattern longTemplate = Pattern.compile("(\\$+)\\{.*}");


  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (element instanceof HaxeStringLiteralExpression stringLiteral) {
      check(stringLiteral, holder);
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

  private static boolean isSingleQuotesRequired(HaxeStringLiteralExpression psi) {
    String text = psi.getText();
    return text.startsWith("\"") && (templateMatches(text, shortTemplate) || templateMatches(text, longTemplate));
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
}
