package com.intellij.plugins.haxe.model.evaluator;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeBaseMemberModel;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeEnumValueConstructorModel;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * The type a position in a switch pattern matches: the switch's matched type
 * ({@link HaxeSwitchSubjectTypeCache}) walked down the pattern - an array
 * pattern's element matches the element type, an object pattern's field the
 * member's type, an enum constructor's argument the constructor's parameter
 * type. Null as soon as a step cannot be typed, and for a position under a
 * {@code value => pattern} extractor, whose matched value is the extractor
 * expression's.
 */
public final class HaxePatternMatchedType {
  private sealed interface Step {
  }

  private record Element(int index) implements Step {
  }

  private record Field(String name) implements Step {
  }

  private record ConstructorArgument(HaxeEnumArgumentExtractor extractor, int index) implements Step {
  }

  private HaxePatternMatchedType() {
  }

  /** The type matched at the element's position, or null. */
  @Nullable
  public static ResultHolder at(@NotNull PsiElement patternElement) {
    HaxeSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(patternElement, HaxeSwitchStatement.class);
    List<Step> steps = switchStatement == null ? null : stepsOf(patternElement);
    return steps == null ? null : typeAlong(switchStatement, steps);
  }

  /** The class of the type matched at the element's position, or null. */
  @Nullable
  public static HaxeClass classAt(@NotNull PsiElement patternElement) {
    ResultHolder matched = at(patternElement);
    SpecificHaxeClassReference classType = matched == null ? null : resolvedClassType(matched);
    return classType == null ? null : classType.getHaxeClass();
  }

  /** The member an object pattern's field names at the element's position, or null when the position is not a field. */
  @Nullable
  public static HaxeBaseMemberModel memberAt(@NotNull PsiElement patternElement) {
    HaxeSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(patternElement, HaxeSwitchStatement.class);
    List<Step> steps = switchStatement == null ? null : stepsOf(patternElement);
    if (steps == null || steps.isEmpty() || !(steps.getLast() instanceof Field field)) return null;
    ResultHolder owner = typeAlong(switchStatement, steps.subList(0, steps.size() - 1));
    SpecificHaxeClassReference ownerType = owner == null ? null : resolvedClassType(owner);
    HaxeClassModel ownerModel = ownerType == null ? null : ownerType.getHaxeClassModel();
    return ownerModel == null ? null : ownerModel.getMember(field.name(), ownerType.getGenericResolver());
  }

  @Nullable
  private static ResultHolder typeAlong(@NotNull HaxeSwitchStatement switchStatement, @NotNull List<Step> steps) {
    ResultHolder type = HaxeSwitchSubjectTypeCache.subjectType(switchStatement);
    for (Step step : steps) {
      if (type == null || type.isUnknown()) return null;
      type = step(type, step);
    }
    return type == null || type.isUnknown() ? null : type;
  }

  /** The steps from the case pattern down to the element, outermost first; null for an unsupported position. */
  @Nullable
  private static List<Step> stepsOf(@NotNull PsiElement patternElement) {
    Deque<Step> steps = new ArrayDeque<>();
    PsiElement child = patternElement;
    for (PsiElement parent = child.getParent(); parent != null; child = parent, parent = parent.getParent()) {
      switch (parent) {
        case HaxeSwitchCaseExpr ignored -> {
          return new ArrayList<>(steps);
        }
        case HaxeSwitchCase ignored -> {
          return null;
        }
        case HaxeExtractorMatchExpression ignored -> {
          return null;
        }
        case HaxeEnumObjectLiteralElement element -> steps.push(new Field(element.getName()));
        case HaxeEnumExtractArrayLiteral array -> steps.push(new Element(indexAmong(array, child)));
        case HaxeSwitchCaseExprArray array -> steps.push(new Element(indexAmong(array, child)));
        case HaxeEnumExtractorArgumentList arguments when arguments.getParent() instanceof HaxeEnumArgumentExtractor extractor ->
          steps.push(new ConstructorArgument(extractor, indexAmong(arguments, child)));
        default -> {
        }
      }
    }
    return null;
  }

  private static int indexAmong(@NotNull PsiElement parent, @NotNull PsiElement child) {
    return Arrays.asList(parent.getChildren()).indexOf(child);
  }

  @Nullable
  private static ResultHolder step(@NotNull ResultHolder type, @NotNull Step step) {
    SpecificHaxeClassReference classType = resolvedClassType(type);
    if (classType == null) return null;
    return switch (step) {
      case Element ignored -> classType.getIterableElementType(null);
      case Field field -> memberType(classType, field.name());
      case ConstructorArgument argument -> constructorArgumentType(classType, argument);
    };
  }

  @Nullable
  private static SpecificHaxeClassReference resolvedClassType(@NotNull ResultHolder type) {
    SpecificHaxeClassReference classType = type.getClassType();
    if (classType == null) return null;
    return classType.fullyResolveTypeDefAndUnwrapNullTypeReference() instanceof SpecificHaxeClassReference resolved ? resolved : null;
  }

  @Nullable
  private static ResultHolder memberType(@NotNull SpecificHaxeClassReference classType, @NotNull String name) {
    HaxeClassModel model = classType.getHaxeClassModel();
    HaxeBaseMemberModel member = model == null ? null : model.getMember(name, classType.getGenericResolver());
    return member == null ? null : member.getResultType(classType.getGenericResolver());
  }

  @Nullable
  private static ResultHolder constructorArgumentType(@NotNull SpecificHaxeClassReference enumType, @NotNull ConstructorArgument argument) {
    HaxeClassModel model = enumType.getHaxeClassModel();
    if (model == null || argument.index() < 0) return null;
    String constructorName = argument.extractor().getEnumValueReference().getReferenceExpression().getIdentifier().getText();
    HaxeBaseMemberModel member = model.getMember(constructorName, enumType.getGenericResolver());
    return member instanceof HaxeEnumValueConstructorModel constructor
           ? constructor.getParameterType(argument.index(), enumType.getGenericResolver())
           : null;
  }
}
