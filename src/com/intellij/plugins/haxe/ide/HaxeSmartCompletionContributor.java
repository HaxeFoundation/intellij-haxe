package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import static com.intellij.patterns.PlatformPatterns.psiElement;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeSmartCompletionContributor extends CompletionContributor {
  public HaxeSmartCompletionContributor() {
    final PsiElementPattern.Capture<PsiElement> idInExpression =
      psiElement().withSuperParent(1, HaxeIdentifier.class).withSuperParent(2, HaxeReference.class);
    extend(CompletionType.SMART,
           idInExpression.and(psiElement().withSuperParent(3, HaxeVarInit.class)),
           new CompletionProvider<CompletionParameters>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet result) {
               tryAddVariantsForEnums(result, parameters.getPosition());
             }
           });
  }

  private static void tryAddVariantsForEnums(CompletionResultSet result, @NotNull PsiElement element) {
    final HaxeVarInit varInit = PsiTreeUtil.getParentOfType(element, HaxeVarInit.class);
    assert varInit != null;
    final HaxeClassResolveResult resolveResult =
      HaxeResolveUtil.tryResolveClassByTypeTag(varInit.getParent(), HaxeGenericSpecialization.EMPTY);
    final HaxeClass haxeClass = resolveResult.getHaxeClass();
    if (haxeClass instanceof HaxeEnumDeclaration) {
      final String className = haxeClass.getName();
      for (HaxeNamedComponent component : HaxeResolveUtil.getNamedSubComponents(haxeClass)) {
        result.addElement(LookupElementBuilder.create(className + "." + component.getName()));
      }
    }
  }
}
