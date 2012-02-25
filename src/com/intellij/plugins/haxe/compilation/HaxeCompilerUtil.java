package com.intellij.plugins.haxe.compilation;

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerMessageCategory;

import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeCompilerUtil {
  public static void fillContext(CompileContext context, List<String> errors) {
    for (String error : errors) {
      addErrorToContext(error, context);
    }
  }

  private static void addErrorToContext(String error, CompileContext context) {
    final HaxeCompilerError compilerError = HaxeCompilerError.create(error);
    if (compilerError == null) {
      context.addMessage(CompilerMessageCategory.ERROR, error, null, -1, -1);
      return;
    }

    context.addMessage(CompilerMessageCategory.ERROR, compilerError.getErrorMessage(), compilerError.getUrl(), compilerError.getLine(), -1);
  }
}
