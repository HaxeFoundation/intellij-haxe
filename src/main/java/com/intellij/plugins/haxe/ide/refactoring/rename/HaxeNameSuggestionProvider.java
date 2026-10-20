package com.intellij.plugins.haxe.ide.refactoring.rename;

import com.intellij.plugins.haxe.ide.refactoring.HaxeRefactoringUtil;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeClassReferenceModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.plugins.haxe.model.HaxePropertyFamily;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.util.HaxeNameKind;
import com.intellij.plugins.haxe.util.HaxeNameSuggesterUtil;
import com.intellij.plugins.haxe.util.HaxeSuggestedNames;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.SuggestedNameInfo;
import com.intellij.refactoring.rename.NameSuggestionProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Rename suggestions for a Haxe declaration, in this order:
///
/// 1. For a parameter, the name the method's contract gives it: the
///    parameter at the same position in the overridden or implemented
///    method, or `value` for a property setter.
/// 2. The current name recased to the kind's convention: a class `myThing`
///    is offered `MyThing`, a constant `maxCount` is offered `MAX_COUNT`.
/// 3. For a local, field or parameter, names from its initializer and its
///    declared type. The name chosen from these is remembered for the next
///    similar value.
///
/// The names avoid those already in use around the declaration, except the
/// declaration's own name, which a rename may keep.
public class HaxeNameSuggestionProvider implements NameSuggestionProvider {
  private static final String SETTER_VALUE_NAME = "value";

  @Override
  public @Nullable SuggestedNameInfo getSuggestedNames(@NotNull PsiElement element,
                                                       @Nullable PsiElement nameSuggestionContext,
                                                       @NotNull Set<String> result) {
    HaxeNamedComponent declaration = declarationOf(element);
    if (declaration == null || declaration.getName() == null) return null;
    HaxeNameKind kind = kindOf(declaration);
    Set<String> used = usedNamesExcept(declaration);

    Set<String> names = new LinkedHashSet<>();
    if (declaration instanceof HaxeParameter parameter) names.addAll(namesFromMethodContract(parameter, used));
    names.addAll(recasedCurrentName(declaration, kind, used));
    HaxeSuggestedNames suggested = namesFromValue(declaration, kind, used);
    names.addAll(suggested.names());
    names.remove(declaration.getName());
    if (names.isEmpty()) return null;
    result.addAll(names);
    return suggested.isEmpty() ? SuggestedNameInfo.NULL_INFO : suggested.asInfo();
  }

  /**
   * The declaring component. A rename started on a declaration hands over
   * its name element; one started on a reference hands over the component
   * the reference resolves to.
   */
  @Nullable
  private static HaxeNamedComponent declarationOf(@NotNull PsiElement element) {
    if (element instanceof HaxeComponentName name && name.getParent() instanceof HaxeNamedComponent component) return component;
    return element instanceof HaxeNamedComponent component ? component : null;
  }

  /** Names from the initializer and declared type of a local, field or parameter; other declarations get none. */
  @NotNull
  private static HaxeSuggestedNames namesFromValue(@NotNull HaxeNamedComponent declaration,
                                                    @NotNull HaxeNameKind kind,
                                                    @NotNull Set<String> used) {
    HaxeVarInit initializer;
    HaxeTypeTag typeTag;
    if (declaration instanceof HaxePsiField field) {
      initializer = field.getVarInit();
      typeTag = field.getTypeTag();
    } else if (declaration instanceof HaxeParameter parameter) {
      initializer = parameter.getVarInit();
      typeTag = parameter.getTypeTag();
    } else {
      return HaxeSuggestedNames.NONE;
    }
    HaxeExpression initialValue = initializer == null ? null : initializer.getExpression();
    ResultHolder type = typeTag == null ? null : HaxeTypeResolver.getTypeFromTypeTag(typeTag, declaration);
    if (initialValue == null && type == null) return HaxeSuggestedNames.NONE;
    return HaxeNameSuggesterUtil.suggest(initialValue, type, kind, null, used);
  }

  /**
   * What the method's contract calls the parameter: the name of the
   * parameter at the same position in the overridden or implemented method,
   * and {@code value} for a property setter, since the property's own name
   * would shadow the field the setter assigns.
   */
  @NotNull
  private static List<String> namesFromMethodContract(@NotNull HaxeParameter parameter, @NotNull Set<String> used) {
    if (!(parameter.getParent() instanceof HaxeParameterList parameters) || !(parameters.getParent() instanceof HaxeMethod method)) {
      return List.of();
    }
    HaxeMethodModel model = method.getModel();
    List<String> names = new ArrayList<>();
    int position = parameters.getParameterList().indexOf(parameter);
    for (HaxeMethodModel contract : contractsOf(model)) {
      List<HaxeParameterModel> contractParameters = contract.getParameters();
      if (position < contractParameters.size()) names.add(contractParameters.get(position).getName());
    }
    if (isPropertySetter(method)) names.add(SETTER_VALUE_NAME);
    return HaxeNameSuggesterUtil.suggestFrom(names, HaxeNameKind.VARIABLE, used);
  }

  /** The methods this one overrides or implements, nearest first. */
  @NotNull
  private static List<HaxeMethodModel> contractsOf(@NotNull HaxeMethodModel method) {
    List<HaxeMethodModel> contracts = new ArrayList<>();
    for (HaxeMethodModel parent = method.getParentMethod(null); parent != null; parent = parent.getParentMethod(null)) {
      if (contracts.contains(parent)) break;
      contracts.add(parent);
    }
    HaxeClassModel declaringClass = method.getDeclaringClass();
    if (declaringClass == null) return contracts;
    for (HaxeClassReferenceModel implemented : declaringClass.getImplementingInterfaces()) {
      HaxeClassModel interfaceModel = implemented.getHaxeClassModel();
      if (interfaceModel == null) continue;
      // the model's by-name lookup misses interface methods; the method list has them
      for (HaxeMethodModel declared : interfaceModel.getMethods(null)) {
        if (Objects.equals(declared.getName(), method.getName())) contracts.add(declared);
      }
    }
    return contracts;
  }

  /** Whether the method is a property setter: {@code set_width} bound by a property {@code width}. */
  private static boolean isPropertySetter(@NotNull HaxeMethod method) {
    String name = method.getName();
    return name != null && name.startsWith(HaxePropertyFamily.SETTER_PREFIX) && HaxePropertyFamily.propertyOfAccessor(method) != null;
  }

  /** The current name in each casing the kind uses; the caller drops the unchanged name. */
  @NotNull
  private static List<String> recasedCurrentName(@NotNull HaxeNamedComponent declaration,
                                                 @NotNull HaxeNameKind kind,
                                                 @NotNull Set<String> used) {
    return HaxeNameSuggesterUtil.recased(declaration.getName(), kind, used);
  }

  @NotNull
  private static HaxeNameKind kindOf(@NotNull HaxeNamedComponent declaration) {
    return switch (declaration) {
      case HaxeClass ignored -> HaxeNameKind.TYPE;
      case HaxeEnumValueDeclaration ignored -> HaxeNameKind.ENUM_VALUE;
      case HaxeMethod ignored -> HaxeNameKind.METHOD;
      case HaxeFieldDeclaration field when isConstant(field) -> HaxeNameKind.CONSTANT;
      default -> HaxeNameKind.VARIABLE;
    };
  }

  /** Whether the field holds a constant: a static field that is final or inline. */
  private static boolean isConstant(@NotNull HaxeFieldDeclaration field) {
    if (!field.isStatic()) return false;
    HaxeMutabilityModifier mutability = field.getMutabilityModifier();
    return field.isInline() || mutability != null && mutability.textMatches("final");
  }

  @NotNull
  private static Set<String> usedNamesExcept(@NotNull HaxeNamedComponent declaration) {
    Set<String> used = HaxeRefactoringUtil.collectUsedNames(declaration);
    String ownName = declaration.getName();
    if (ownName != null) used.remove(ownName);
    return used;
  }
}
