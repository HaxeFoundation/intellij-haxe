package com.intellij.plugins.haxe.ide.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompletionMode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import static com.intellij.plugins.haxe.ide.completion.HaxeCommonCompletionPattern.inImportOrUsing;
import static com.intellij.plugins.haxe.ide.completion.HaxeCommonCompletionPattern.isSimpleIdentifier;

/**
 * Offers an arrow function and a function literal where the site expects a
 * function: a call or constructor argument, a type-tagged initializer, an
 * assignment or a return. The expected type comes from the static type
 * model here; in the compiler-only mode the compiler contributor adds the
 * same lookups from the server's expected type, so the suggestion appears
 * whatever the completion source.
 */
public class HaxeLambdaCompletionContributor extends CompletionContributor {

  public HaxeLambdaCompletionContributor() {
    CompletionProvider<CompletionParameters> provider = new CompletionProvider<>() {
      @Override
      protected void addCompletions(@NotNull CompletionParameters parameters,
                                    @NotNull ProcessingContext context,
                                    @NotNull CompletionResultSet result) {
        addLambdas(parameters, result);
      }
    };
    extend(CompletionType.BASIC, isSimpleIdentifier.andNot(inImportOrUsing), provider);
    extend(CompletionType.SMART, isSimpleIdentifier.andNot(inImportOrUsing), provider);
  }

  private static void addLambdas(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
    PsiElement position = parameters.getPosition();
    if (HaxeCompilerSettings.getInstance(position.getProject()).getCompletionMode() == HaxeCompletionMode.COMPILER_ONLY) return;
    HaxeReferenceExpression reference = PsiTreeUtil.getParentOfType(position, HaxeReferenceExpression.class);
    // in a chain the left side dictates the members; the expected type adds nothing
    if (reference == null || HaxeCompletionUtil.isInReferenceChain(position)) return;

    ResultHolder expected = HaxeExpectedEnumValueCompletionContributor.declaredExpectedType(reference);
    HaxeLambdaShape shape = HaxeLambdaShape.fromStatic(expected);
    if (shape != null) HaxeLambdaLookups.addTo(result, shape, parameters);
  }
}
