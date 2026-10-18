package com.intellij.plugins.haxe.ide.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.fakes.HaxeSyntheticDeclarations;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.plugins.haxe.ide.completion.HaxeCommonCompletionPattern.identifierInNewExpression;
import static com.intellij.plugins.haxe.ide.completion.HaxeCompletionUtil.isInReferenceChain;
import static com.intellij.plugins.haxe.ide.lookup.HaxeMemberLookupElement.createSynteticMember;

/** Completes the identifiers the compiler accepts without a declaration: {@code trace} and the untyped target-syntax calls. */
public class HaxeSyntheticMemberCompletionContributor extends CompletionContributor {

  private static final PsiElementPattern.Capture<PsiElement> ELEMENT_CAPTURE = psiElement()
    .inside(HaxeIdentifier.class)
    .andNot(psiElement().inside(HaxeType.class))
    // current = ID token
    // parent 1: HaxeIdentifier
    // parent 2: HaxeReference
    .and(psiElement().withSuperParent(2, HaxeReferenceExpression.class));

  public HaxeSyntheticMemberCompletionContributor() {
    extend(CompletionType.BASIC, ELEMENT_CAPTURE, new CompletionProvider<>() {
      @Override
      protected void addCompletions(@NotNull CompletionParameters parameters,
                                    @NotNull ProcessingContext context,
                                    @NotNull CompletionResultSet result) {
        boolean newExpression = identifierInNewExpression.accepts(parameters.getOriginalPosition());
        boolean inChain = isInReferenceChain(parameters.getPosition());
        if (!newExpression && !inChain) {
          addSyntheticMembers(result, parameters.getOriginalFile().getProject());
        }
      }
    });
  }

  private static void addSyntheticMembers(@NotNull CompletionResultSet result, @NotNull Project project) {
    result.addAllElements(createSynteticMember(HaxeSyntheticDeclarations.traceMethod(project).getComponentName()));
    for (HaxeMethod method : HaxeSyntheticDeclarations.targetSyntaxMethods(project)) {
      result.addAllElements(createSynteticMember(method.getComponentName()));
    }
  }
}
