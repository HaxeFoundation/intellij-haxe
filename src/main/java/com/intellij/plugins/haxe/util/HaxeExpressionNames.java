package com.intellij.plugins.haxe.util;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.ide.refactoring.HaxeNamesValidator;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.text.NameUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Names taken from an expression: from what it is, and from where it sits.
 * The results are raw lowerCamel names, best first; casing and tails are
 * applied later.
 */
public final class HaxeExpressionNames {
  /** A call named {@code getUserName} or {@code toString} is about its remainder: {@code userName}, {@code string}. */
  private static final Set<String> PROPERTY_VERBS = Set.of("get", "is", "find", "create", "as", "to");
  private static final String REGEX_VALUE_NAME = "regex";
  private static final String FUNCTION_VALUE_NAME = "func";
  /** A string literal of up to this many identifier-like words names its value ({@code "Der Kommisar"} gives {@code derKommisar}). */
  private static final int MAX_LITERAL_WORDS = 5;

  private HaxeExpressionNames() {
  }

  /**
   * Names from what the expression is: the property a getter call returns
   * ({@code user.getName()} gives {@code name} and {@code userName}), the
   * called method for any other call, the referenced name, the singular of
   * an indexed array, the class of a {@code new}, the words of a short
   * string literal. Parentheses, type checks and casts are looked through
   * to their operand.
   */
  @NotNull
  public static List<String> ofExpression(@Nullable PsiElement expression) {
    return switch (expression) {
      case null -> List.of();
      case HaxeParenthesizedExpression parenthesized -> ofExpression(insideOf(parenthesized));
      case HaxeTypeCheckExpr typeCheck -> ofExpression(typeCheck.getExpression());
      case HaxeSafeCastExpression cast -> ofCast(cast);
      case HaxeUnsafeCastExpression cast -> ofExpression(cast.getExpression());
      case HaxeCallExpression call -> ofCall(call);
      case HaxeArrayAccessExpression access -> ofArrayAccess(access);
      case HaxeNewExpression construction -> ofNew(construction);
      case HaxeStringLiteralExpression literal -> ofStringLiteral(literal);
      case HaxeRegularExpression ignored -> List.of(REGEX_VALUE_NAME);
      case HaxeFunctionLiteral ignored -> List.of(FUNCTION_VALUE_NAME);
      case HaxeReferenceExpression reference -> listOfNullable(referenceName(reference));
      default -> List.of();
    };
  }

  /**
   * Names from where the expression sits: the parameter it is passed as
   * ({@code setColor(0xff0000)} gives {@code color}), or the variable it is
   * assigned to.
   */
  @NotNull
  public static List<String> ofPlace(@Nullable PsiElement expression) {
    if (expression == null) return List.of();
    PsiElement parent = expression.getParent();
    if (parent instanceof HaxeCallExpressionList arguments && arguments.getParent() instanceof HaxeCallExpression call) {
      int index = arguments.getExpressionList().indexOf(expression);
      return listOfNullable(parameterName(call.getExpression(), index));
    }
    if (parent instanceof HaxeNewExpression construction) {
      int index = construction.getExpressionList().indexOf(expression);
      return listOfNullable(parameterName(construction, index));
    }
    if (parent instanceof HaxeAssignExpression assignment && expression == assignment.getRightExpression()) {
      return assignment.getLeftExpression() instanceof HaxeReferenceExpression target ? listOfNullable(referenceName(target)) : List.of();
    }
    return List.of();
  }

  @Nullable
  private static PsiElement insideOf(@NotNull HaxeParenthesizedExpression parenthesized) {
    HaxeExpression expression = parenthesized.getExpression();
    return expression != null ? expression : parenthesized.getTypeCheckExpr();
  }

  /** The operand's names, then the type cast to. */
  @NotNull
  private static List<String> ofCast(@NotNull HaxeSafeCastExpression cast) {
    List<String> names = new ArrayList<>(ofExpression(cast.getExpression()));
    HaxeTypeOrAnonymous target = cast.getTypeOrAnonymous();
    HaxeType targetType = target != null ? target.getType() : null;
    if (targetType != null) names.add(StringUtil.decapitalize(targetType.getReferenceExpression().getText()));
    return names;
  }

  @NotNull
  private static List<String> ofCall(@NotNull HaxeCallExpression call) {
    if (!(call.getExpression() instanceof HaxeReferenceExpression callee)) return List.of();
    String method = referenceName(callee);
    if (method == null) return List.of();
    List<String> words = NameUtilCore.nameToWordList(method);
    boolean namesAProperty = words.size() > 1 && PROPERTY_VERBS.contains(StringUtil.toLowerCase(words.getFirst()));
    if (!namesAProperty) return List.of(method);

    String property = StringUtil.decapitalize(method.substring(words.getFirst().length()));
    String receiver = receiverVariableName(callee);
    return receiver == null ? List.of(property) : List.of(property, receiver + StringUtil.capitalize(property));
  }

  /** The singular of the array's names: {@code items[i]} gives {@code item}. A name without a distinct singular gives nothing. */
  @NotNull
  private static List<String> ofArrayAccess(@NotNull HaxeArrayAccessExpression access) {
    List<HaxeExpression> operands = access.getExpressionList();
    if (operands.isEmpty()) return List.of();
    List<String> singulars = new ArrayList<>();
    for (String plural : ofExpression(operands.getFirst())) {
      String singular = StringUtil.unpluralize(plural);
      if (singular != null && !singular.equals(plural)) singulars.add(singular);
    }
    return singulars;
  }

  @NotNull
  private static List<String> ofNew(@NotNull HaxeNewExpression construction) {
    HaxeType type = construction.getType();
    return type == null ? List.of() : List.of(StringUtil.decapitalize(type.getReferenceExpression().getText()));
  }

  /**
   * {@code "userName"} gives {@code userName}; {@code "Der Kommisar"} gives
   * {@code derKommisar}; text with anything but words a Haxe name can hold
   * (punctuation, interpolation, letters beyond ASCII) gives nothing.
   */
  @NotNull
  private static List<String> ofStringLiteral(@NotNull HaxeStringLiteralExpression literal) {
    String value = StringUtil.unquoteString(literal.getText());
    // whitespace-separated words
    String[] words = value.trim().split("\\s+");
    if (words.length == 0 || words.length > MAX_LITERAL_WORDS) return List.of();
    StringBuilder name = new StringBuilder();
    for (String word : words) {
      if (!HaxeNamesValidator.isIdentifier(word)) return List.of();
      name.append(name.isEmpty() ? StringUtil.decapitalize(word) : StringUtil.capitalize(word));
    }
    return List.of(name.toString());
  }

  /** The name of the parameter at {@code index} of the function the callee resolves to. */
  @Nullable
  private static String parameterName(@Nullable PsiElement callee, int index) {
    if (index < 0 || !(callee instanceof HaxeReference reference)) return null;
    if (!(reference.resolve() instanceof HaxeMethod method)) return null;
    List<HaxeParameterModel> parameters = method.getModel().getParameters();
    return index < parameters.size() ? parameters.get(index).getName() : null;
  }

  /** The receiver's name when the receiver is a variable ({@code user} in {@code user.getName()}), not a type. */
  @Nullable
  private static String receiverVariableName(@NotNull HaxeReferenceExpression callee) {
    HaxeReferenceExpression receiver = PsiTreeUtil.getChildOfType(callee, HaxeReferenceExpression.class);
    if (receiver == null) return null;
    PsiElement target = receiver.resolve();
    boolean isVariable = target instanceof HaxePsiField || target instanceof HaxeParameter;
    return isVariable ? referenceName(receiver) : null;
  }

  /** The name a possibly qualified reference ends in: {@code name} for {@code user.name}. */
  @Nullable
  private static String referenceName(@NotNull HaxeReferenceExpression reference) {
    HaxeIdentifier identifier = reference.getIdentifier();
    String name = identifier != null ? identifier.getText() : null;
    return name == null || name.isEmpty() ? null : name;
  }

  @NotNull
  private static List<String> listOfNullable(@Nullable String name) {
    return name == null ? List.of() : List.of(name);
  }
}
