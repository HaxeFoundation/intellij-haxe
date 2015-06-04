/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2015 Elias Ku
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
package com.intellij.plugins.haxe.tests.runner.filters;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaSourceRootType;

import java.util.List;

public class ErrorFilter implements Filter {

  private Module myModule;

  private final String START_TOKEN = "ERR: ";

  public ErrorFilter(Module module) {
    myModule = module;
  }

  @Nullable
  @Override
  public Result applyFilter(String s, int i) {
    if(s.indexOf(START_TOKEN) == -1) {
      return null;
    }
    int classAndLineIndex = START_TOKEN.length();
    int openBraceIndex = s.indexOf("(");
    int closeBraceIndex = s.indexOf(")");

    String classAndLine = s.substring(classAndLineIndex, openBraceIndex);
    Integer lineNumber = Integer.parseInt(classAndLine.substring(classAndLine.indexOf(":") + 1));

    String classAndFunction = s.substring(openBraceIndex + 1, closeBraceIndex).replace(".", "/");
    String functionName = classAndFunction.substring(classAndFunction.lastIndexOf("/") + 1);
    HyperlinkInfo hyperlink = createOpenFileHyperlink(classAndFunction.replace("/" + functionName, ".hx"), lineNumber);
    return new Result(i - s.length() + classAndLineIndex, i - s.length() + openBraceIndex, hyperlink);
  }

  protected HyperlinkInfo createOpenFileHyperlink(String filePath, int line) {
    ModuleRootManager rootManager = ModuleRootManager.getInstance(myModule);
    List<VirtualFile> roots = rootManager.getSourceRoots(JavaSourceRootType.TEST_SOURCE);
    VirtualFile virtualFile = null;
    for (VirtualFile testSourceRoot : roots) {
      String fullPath = testSourceRoot.getPath() + "/" + filePath;
      virtualFile = LocalFileSystem.getInstance().findFileByPath(fullPath);
      if(virtualFile != null) {
        break;
      }
    }
    return virtualFile == null ? null : new OpenFileHyperlinkInfo(myModule.getProject(), virtualFile, line - 1);
  }
}
