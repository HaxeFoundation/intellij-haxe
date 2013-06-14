/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
