package com.intellij.plugins.haxe.compilation;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.util.PathUtil;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeCompilerUtil {
  public static void fillContext(Module module, CompileContext context, String[] errors) {
    for (String error : errors) {
      addErrorToContext(module, error, context);
    }
  }

  private static void addErrorToContext(Module module, String error, CompileContext context) {
    final HaxeCompilerError compilerError = HaxeCompilerError.create(
      PathUtil.getParentPath(module.getModuleFilePath()),
      error,
      !ApplicationManager.getApplication().isUnitTestMode()
    );
    if (compilerError == null) {
      context.addMessage(CompilerMessageCategory.WARNING, error, null, -1, -1);
      return;
    }

    context.addMessage(
      CompilerMessageCategory.WARNING,
      compilerError.getErrorMessage(),
      VfsUtilCore.pathToUrl(compilerError.getPath()),
      compilerError.getLine(),
      -1
    );
  }
}
