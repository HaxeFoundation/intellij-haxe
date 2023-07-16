package com.intellij.plugins.haxe.model.type;

import com.intellij.plugins.haxe.lang.psi.HaxeMethodDeclaration;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeNamedComponent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HaxeMacroUtil {

  public static boolean isMacroMethod(AbstractHaxeNamedComponent method) {
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
        List<SpecificFunctionReference.Argument>
          argumentList = functionType.getArguments().stream().map(HaxeMacroUtil::resolveMacroTypeForArgument).toList();
        ResultHolder returnType = functionType.getReturnType();
        if (returnType.isClassType()) {
          ResultHolder resolvedReturnType = resolveMacroType(returnType);
          if (resolvedReturnType!= null) {
            returnType = resolvedReturnType;
          }
        }
        return new SpecificFunctionReference(argumentList, returnType, null, functionType.context).createHolder();
      }
    }
    return type;
  }

  private static SpecificFunctionReference.Argument resolveMacroTypeForArgument(SpecificFunctionReference.Argument argument) {
    ResultHolder type = argument.getType();
    if (type.isClassType()) {
      ResultHolder holder = resolveMacroType(type);
      return argument.changeType(holder);
    }
    // TODO function type support
    return argument;
  }

  public static ResultHolder resolveMacroType(ResultHolder returnType) {
    SpecificHaxeClassReference classReference = returnType.getClassType();
    if (classReference == null  || classReference.getHaxeClass() == null) return returnType;
    String qualifiedName = classReference.getHaxeClass().getQualifiedName();
    return  switch (qualifiedName) {
      case "haxe.macro.Expr"  -> SpecificTypeReference.getDynamic(returnType.getElementContext()).createHolder();
      case "haxe.macro.Expr.ExprOf" -> classReference.getSpecifics()[0];
      default -> returnType;
    };
  }

}
