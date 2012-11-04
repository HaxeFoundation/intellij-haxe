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
  public static boolean fillContext(Module module, CompileContext context, String[] errors) {
    boolean hasErrors = false;
    for (String error : errors) {
      final boolean containsError = addErrorToContext(module, error, context);
      hasErrors = hasErrors || containsError;
    }
    return hasErrors;
  }

  private static boolean addErrorToContext(Module module, String error, CompileContext context) {
    final HaxeCompilerError compilerError = HaxeCompilerError.create(
      PathUtil.getParentPath(module.getModuleFilePath()),
      error,
      !ApplicationManager.getApplication().isUnitTestMode()
    );
    final boolean isError = error.contains("error");
    if (compilerError == null) {
      context.addMessage(isError ? CompilerMessageCategory.ERROR : CompilerMessageCategory.WARNING, error, null, -1, -1);
      return isError;
    }

    context.addMessage(
      isError ? CompilerMessageCategory.ERROR : CompilerMessageCategory.WARNING,
      compilerError.getErrorMessage(),
      VfsUtilCore.pathToUrl(compilerError.getPath()),
      compilerError.getLine(),
      -1
    );
    return isError;
  }
}
