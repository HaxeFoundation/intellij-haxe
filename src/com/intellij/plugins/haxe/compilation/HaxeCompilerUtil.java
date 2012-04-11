package com.intellij.plugins.haxe.compilation;

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.module.Module;
import com.intellij.util.PathUtil;

import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeCompilerUtil {
  public static void fillContext(Module module, CompileContext context, List<String> errors) {
    for (String error : errors) {
      addErrorToContext(module, error, context);
    }
  }

  private static void addErrorToContext(Module module, String error, CompileContext context) {
    final HaxeCompilerError compilerError = HaxeCompilerError.create(PathUtil.getParentPath(module.getModuleFilePath()), error);
    if (compilerError == null) {
      context.addMessage(CompilerMessageCategory.ERROR, error, null, -1, -1);
      return;
    }

    context.addMessage(CompilerMessageCategory.ERROR, compilerError.getErrorMessage(), compilerError.getUrl(), compilerError.getLine(), -1);
  }
}
