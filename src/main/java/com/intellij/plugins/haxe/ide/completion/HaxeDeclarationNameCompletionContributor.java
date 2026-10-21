package com.intellij.plugins.haxe.ide.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

/// Suggests a name for a declaration while it is being typed. A Haxe name
/// comes before its type, so nothing about the value is known yet; the
/// suggestions come from the surrounding code instead: identifiers it
/// already uses without a declaration, and for a constructor parameter the
/// class's fields. Here `total` is offered at the caret:
///
/// ```haxe
/// var |
/// total = 1;
/// ```
public class HaxeDeclarationNameCompletionContributor extends CompletionContributor {
  /** A body longer than this is not searched for undeclared names; the walk is per keystroke. */
  private static final int MAX_SEARCHED_LENGTH = 50_000;
  private static final double NAME_PRIORITY = 100;

  public HaxeDeclarationNameCompletionContributor() {
    extend(CompletionType.BASIC,
           PlatformPatterns.psiElement(HaxeTokenTypes.ID)
             .withParent(HaxeIdentifier.class)
             .withSuperParent(2, HaxeComponentName.class),
           new CompletionProvider<>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters,
                                           @NotNull ProcessingContext context,
                                           @NotNull CompletionResultSet result) {
               PsiElement declaration = parameters.getPosition().getParent().getParent().getParent();
               for (String name : namesFor(declaration)) {
                 result.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create(name), NAME_PRIORITY));
               }
             }
           });
  }

  @NotNull
  private static Set<String> namesFor(@NotNull PsiElement declaration) {
    return switch (declaration) {
      case HaxeLocalVarDeclaration local -> undeclaredNamesIn(enclosingFunction(local));
      case HaxeParameter parameter -> namesForParameter(parameter);
      case HaxeFieldDeclaration field -> undeclaredNamesIn(field.getParent());
      default -> Set.of();
    };
  }

  /** For a constructor the class's fields first, then the names the function uses without declaring them. */
  @NotNull
  private static Set<String> namesForParameter(@NotNull HaxeParameter parameter) {
    PsiElement function = parameter.getParent() instanceof HaxeParameterList list ? list.getParent() : null;
    Set<String> names = new LinkedHashSet<>();
    if (function instanceof HaxeMethod method && method.isConstructor() && method.getContainingClass() instanceof HaxeClass owner) {
      for (HaxeNamedComponent field : owner.getHaxeFieldsSelf(null)) {
        if (field.getName() != null) names.add(field.getName());
      }
    }
    names.addAll(undeclaredNamesIn(function));
    return names;
  }

  @Nullable
  private static PsiElement enclosingFunction(@NotNull PsiElement element) {
    return PsiTreeUtil.getParentOfType(element, HaxeMethod.class, HaxeFunctionLiteral.class);
  }

  /** The unqualified and {@code this}-qualified references in {@code scope} that resolve to nothing. */
  @NotNull
  private static Set<String> undeclaredNamesIn(@Nullable PsiElement scope) {
    Set<String> names = new LinkedHashSet<>();
    if (scope == null || scope.getTextLength() > MAX_SEARCHED_LENGTH) return names;
    for (HaxeReferenceExpression reference : PsiTreeUtil.findChildrenOfType(scope, HaxeReferenceExpression.class)) {
      if (!isPlainName(reference) || reference.resolve() != null) continue;
      HaxeIdentifier identifier = reference.getIdentifier();
      String name = identifier == null ? null : identifier.getText();
      if (name != null && !name.contains(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED)) names.add(name);
    }
    return names;
  }

  /**
   * Whether the reference is a bare name ({@code total}) or a member of
   * {@code this} ({@code this.total}). A member of another object
   * ({@code other.total}) and a qualifier ({@code other}) are not.
   */
  private static boolean isPlainName(@NotNull HaxeReferenceExpression reference) {
    PsiElement qualifier = reference.getFirstChild();
    boolean plain = qualifier instanceof HaxeIdentifier || qualifier instanceof HaxeThisExpression;
    boolean isQualifierItself = reference.getParent() instanceof HaxeReferenceExpression parent && parent.getFirstChild() == reference;
    return plain && !isQualifierItself;
  }
}
