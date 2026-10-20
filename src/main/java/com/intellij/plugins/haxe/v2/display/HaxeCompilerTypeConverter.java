package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.display.protocol.JsonTypeRef;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a compiler type ({@code haxe.display.JsonModuleTypes.JsonType}) into
 * the plugin's type model, so a compiler answer can stand in for an
 * evaluator result. Named types resolve through the class indexes, so the
 * result navigates like a statically resolved one. Null when the type cannot
 * be expressed: an unresolved monomorph, a type the indexes do not know, or
 * an anonymous structure.
 * <p>
 * TODO: anonymous structures ({@code TAnonymous}) need a synthetic typedef, as
 * the blueprint path renders them; until then they fall back to the IDE.
 */
final class HaxeCompilerTypeConverter {

  private HaxeCompilerTypeConverter() {
  }

  @Nullable
  static ResultHolder toResultHolder(@Nullable JsonTypeRef type, @NotNull PsiElement context) {
    if (type == null) return null;
    if (type.isNamedType()) return classType(type, context);
    if (type.isFunction()) return functionType(type, context);
    if (type.isDynamic()) return SpecificTypeReference.getDynamic(context).createHolder();
    return null;
  }

  @Nullable
  private static ResultHolder classType(@NotNull JsonTypeRef type, @NotNull PsiElement context) {
    HaxeClass haxeClass = findClass(type, context);
    if (haxeClass == null) return null;
    List<ResultHolder> specifics = new ArrayList<>();
    for (JsonTypeRef param : type.typeParameters()) {
      ResultHolder specific = toResultHolder(param, context);
      if (specific == null) return null;
      specifics.add(specific);
    }
    HaxeClassReference reference = new HaxeClassReference(haxeClass.getModel(), context);
    return SpecificHaxeClassReference.withGenerics(reference, specifics.toArray(ResultHolder[]::new)).createHolder();
  }

  /** The declaration of a named compiler type, through the class indexes: by its qualified name, else by its dot path. */
  @Nullable
  static HaxeClass findClass(@NotNull JsonTypeRef type, @NotNull PsiElement context) {
    String qualifiedName = type.qualifiedName();
    HaxeClass byQualifiedName = HaxeResolveUtil.findClassByQName(qualifiedName, context);
    if (byQualifiedName != null) return byQualifiedName;
    return HaxeResolveUtil.findClassByQName(type.dotPath(), context);
  }

  @Nullable
  private static ResultHolder functionType(@NotNull JsonTypeRef type, @NotNull PsiElement context) {
    List<HaxeArgument> arguments = new ArrayList<>();
    int index = 0;
    for (JsonTypeRef.Argument argument : type.functionArguments()) {
      ResultHolder argumentType = toResultHolder(argument.type(), context);
      if (argumentType == null) return null;
      arguments.add(new HaxeArgument(context, index++, argument.optional(), false, argumentType, argument.name()));
    }
    ResultHolder returnType = toResultHolder(type.returnType(), context);
    if (returnType == null) return null;
    return new SpecificFunctionReference(arguments, returnType, (HaxeMethodModel)null, context).createHolder();
  }
}
