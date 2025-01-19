package com.intellij.plugins.haxe.model.type;

import com.intellij.plugins.haxe.lang.psi.HaxeMethodDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HaxeMacroUtil {

  public static boolean isMacroMethod(HaxeNamedComponent method) {
    if(method instanceof  HaxeMethodDeclaration methodDeclaration) {
      // todo make a better solution to check if macro
      return methodDeclaration.getMethodModifierList().stream().anyMatch(modifier -> modifier.getText().trim().equals("macro"));
      }
    return false;
  }

  @NotNull
  public static ResultHolder resolveMacroTypesForFunction(@NotNull ResultHolder type) {
    if(type.isFunctionType()) {
      SpecificFunctionReference functionType = type.getFunctionType();
      if (functionType != null) {
        List<HaxeArgument> argumentList = functionType.getArguments().stream()
          .map(HaxeMacroUtil::resolveMacroTypeForArgument)
          .toList();

        ResultHolder returnType = functionType.getReturnType();
        if (returnType.isClassType()) {
          ResultHolder resolvedReturnType = resolveMacroType(returnType);
          if (resolvedReturnType!= null) {
            returnType = resolvedReturnType;
          }
        }
        return new SpecificFunctionReference(argumentList, returnType, (HaxeMethodModel)null, functionType.context).createHolder();
      }
    }
    return type;
  }

  private static HaxeArgument resolveMacroTypeForArgument(HaxeArgument argument) {
    ResultHolder type = argument.getType();
    if (type.isClassType()) {
      ResultHolder holder = resolveMacroType(type);
      return argument.withType(holder);
    }
    // TODO function type support
    return argument;
  }

  public static ResultHolder resolveMacroType(ResultHolder returntype) {
    SpecificHaxeClassReference type = returntype.getClassType();
    SpecificTypeReference reference = resolveMacroType(type);
    return reference == null ? returntype : reference.createHolder();
  }
  public static SpecificTypeReference resolveMacroType(SpecificHaxeClassReference classReference) {

    if (classReference == null  || classReference.getHaxeClass() == null) return classReference;
    String qualifiedName = classReference.getHaxeClass().getQualifiedName();
    return  switch (qualifiedName) {
      case HaxeMacroTypeUtil.EXPR  -> SpecificTypeReference.getDynamic(classReference.getElementContext());
      case HaxeMacroTypeUtil.EXPR_OF -> classReference.getSpecifics()[0].getClassType();
      default -> classReference;
    };
  }
  public static boolean isMacroType(ResultHolder returnType) {
    return isMacroType(returnType.getClassType());
  }
  public static boolean isMacroType(SpecificHaxeClassReference classReference) {
    if (classReference == null  || classReference.getHaxeClass() == null) return false;
    String qualifiedName = classReference.getHaxeClass().getQualifiedName();
    return  switch (qualifiedName) {
      case HaxeMacroTypeUtil.EXPR, HaxeMacroTypeUtil.EXPR_OF -> true;
      default -> false;
    };
  }

}
