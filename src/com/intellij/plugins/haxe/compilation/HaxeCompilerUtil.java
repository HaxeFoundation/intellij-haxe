/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
import com.intellij.openapi.vfs.VfsUtilCore;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeCompilerUtil {
  public static void fillContext(CompileContext context, String errorRoot, String[] errors) {
    for (String error : errors) {
      addErrorToContext(error, context, errorRoot);
    }
  }

  private static void addErrorToContext(String error, CompileContext context, String errorRoot) {
    final HaxeCompilerError compilerError = HaxeCompilerError.create(
      errorRoot,
      error,
      !ApplicationManager.getApplication().isUnitTestMode()
    );
    if (compilerError == null) {
      context.addMessage(CompilerMessageCategory.WARNING, error, null, -1, -1);
      return;
    }

    context.addMessage(
      compilerError.getCategory(),
      compilerError.getErrorMessage(),
      VfsUtilCore.pathToUrl(compilerError.getPath()),
      compilerError.getLine(),
      compilerError.getColumn()
    );
  }
}
