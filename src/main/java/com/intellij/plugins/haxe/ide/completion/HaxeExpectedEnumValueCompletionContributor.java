package com.intellij.plugins.haxe.ide.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.HaxeAbstractEnumUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.intellij.plugins.haxe.ide.completion.HaxeCommonCompletionPattern.inImportOrUsing;
import static com.intellij.plugins.haxe.ide.completion.HaxeCommonCompletionPattern.isSimpleIdentifier;

/**
 * Offers the values of the expected enum at the top of the list when the expected type is
 * declared at the completion site: a type-tagged variable initializer or assignment target,
 * a call or constructor argument, or a parameter default value. The values are offered by
 * bare name; the expected type that triggers the suggestion is also what lets the compiler
 * resolve the unqualified name. Sites whose expected type would need inference (an
 * assignment to an untagged local, say) are deliberately left out so completion never pays
 * for an expression evaluation here.
 */
public class HaxeExpectedEnumValueCompletionContributor extends CompletionContributor {

  private static final double EXPECTED_ENUM_PRIORITY = 1000;

  public HaxeExpectedEnumValueCompletionContributor() {
    CompletionProvider<CompletionParameters> provider = new CompletionProvider<>() {
      @Override
      protected void addCompletions(@NotNull CompletionParameters parameters,
                                    @NotNull ProcessingContext context,
                                    @NotNull CompletionResultSet result) {
        addExpectedEnumValues(parameters, result);
      }
    };
    extend(CompletionType.BASIC, isSimpleIdentifier.andNot(inImportOrUsing), provider);
    extend(CompletionType.SMART, isSimpleIdentifier.andNot(inImportOrUsing), provider);
  }

  private static void addExpectedEnumValues(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
    HaxeClass enumClass = expectedEnumClass(parameters);
    if (enumClass == null) return;

    for (HaxeBaseMemberModel value : enumValues(enumClass)) {
      // bare name: at these sites the expected type is what resolves the value, no qualifier or import needed
      LookupElementBuilder lookupElement = HaxeLookupElementFactory.create(value);
      if (lookupElement == null) continue;
      result.addElement(PrioritizedLookupElement.withPriority(lookupElement, EXPECTED_ENUM_PRIORITY));
    }
  }

  /** The enum whose values the completed expression is expected to yield, or null when the site declares no enum type. */
  @Nullable
  public static HaxeClass expectedEnumClass(@NotNull CompletionParameters parameters) {
    PsiElement position = parameters.getPosition();
    HaxeReferenceExpression reference = PsiTreeUtil.getParentOfType(position, HaxeReferenceExpression.class);
    // in a chain the left side dictates the members; the expected type adds nothing
    if (reference == null || HaxeCompletionUtil.isInReferenceChain(position)) return null;

    ResultHolder expected = declaredExpectedType(reference);
    return expected == null ? null : enumClassOf(expected);
  }

  /**
   * The type the completed expression must conform to, from declarations only: type tags and
   * resolved signatures. Returns null where finding the type would require inference. Shared
   * with the lambda contributor, which offers a function literal at the same sites.
   */
  @Nullable
  static ResultHolder declaredExpectedType(@NotNull HaxeReferenceExpression reference) {
    PsiElement parent = reference.getParent();
    if (parent instanceof HaxeAssignExpression assign) {
      return assignTargetDeclaredType(assign, reference);
    }
    if (parent instanceof HaxeVarInit varInit && varInit.getParent() instanceof HaxeParameter parameter) {
      return taggedType(parameter.getTypeTag(), parameter);
    }
    // covers type-tagged var initializers, call and constructor arguments (through the cached
    // call evaluation), returns, and array/object literals nested in those
    return HaxeResolveChecks.findExpectedType(reference);
  }

  @Nullable
  private static ResultHolder assignTargetDeclaredType(@NotNull HaxeAssignExpression assign,
                                                       @NotNull HaxeReferenceExpression reference) {
    List<HaxeExpression> expressions = assign.getExpressionList();
    if (expressions.size() < 2) return null;
    HaxeExpression left = expressions.getFirst();
    if (left == reference || !(left instanceof HaxeReferenceExpression target)) return null;

    PsiElement resolved = target.resolve();
    if (resolved == null) return null;
    HaxePsiField field = PsiTreeUtil.getParentOfType(resolved, HaxePsiField.class, false);
    if (field != null) {
      return taggedType(field.getTypeTag(), field);
    }
    HaxeParameter parameter = PsiTreeUtil.getParentOfType(resolved, HaxeParameter.class, false);
    return parameter == null ? null : taggedType(parameter.getTypeTag(), parameter);
  }

  /** The type a declaration's tag names; null without a tag (the type would need inference). */
  @Nullable
  private static ResultHolder taggedType(@Nullable HaxeTypeTag tag, @NotNull PsiElement declaration) {
    return tag == null ? null : HaxeTypeResolver.getTypeFromTypeTag(tag, declaration);
  }

  @Nullable
  private static HaxeClass enumClassOf(@NotNull ResultHolder expected) {
    SpecificHaxeClassReference classType = expected.getClassType();
    if (classType == null) return null;
    SpecificTypeReference unwrapped = classType.fullyResolveTypeDefAndUnwrapNullTypeReference();
    if (!(unwrapped instanceof SpecificHaxeClassReference enumReference)) return null;

    HaxeClass haxeClass = enumReference.getHaxeClass();
    return haxeClass != null && haxeClass.isEnum() ? haxeClass : null;
  }

  private static List<HaxeBaseMemberModel> enumValues(@NotNull HaxeClass enumClass) {
    HaxeClassModel model = enumClass.getModel();
    if (model instanceof HaxeEnumModel enumModel) {
      return enumModel.getValues().stream()
        .filter(HaxeBaseMemberModel.class::isInstance)
        .map(HaxeBaseMemberModel.class::cast)
        .toList();
    }
    // abstract enum: the implicit value fields
    return model.getFields().stream()
      .filter(field -> HaxeAbstractEnumUtil.couldBeAbstractEnumField(field.getBasePsi()))
      .map(HaxeBaseMemberModel.class::cast)
      .toList();
  }
}
