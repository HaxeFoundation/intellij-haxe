/*
 * Copyright 20018 Eric Bishton
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
package com.intellij.plugins.haxe.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.DebugUtil;


import java.util.Stack;

public class HaxeDebugPsiUtil {

  public static PsiElement getParents(PsiElement element, int level, boolean stopAtFile) {
    if (element == null) return null;
    if (level < 0) return null;
    if (level == 0) return element;

    PsiElement parent = element;
    while (level-- > 0 && !(stopAtFile && parent instanceof PsiFile)) {
      PsiElement p = parent.getParent();
      if (null == p) { // Gives back the last good parent.
        break;
      }
      parent = p;
    }
    return parent;
  }

  //public static void dumpElementPath(PsiElement element) {
  //  dumpElementPath(createLogger(HaxeDebugUtil.getCallerCanonicalName()), element);
  //}

  //public static void dumpElementPath(HaxeDebugLogger log, PsiElement element) {
  //  if (null == log) {
  //    log = createLogger(HaxeDebugUtil.getCallerCanonicalName());
  //  }
  //  if (log.isDebugEnabled()) {
  //    log.debug(formatElementPath(element, false));
  //  }
  //}

  public static String printElementTree(PsiElement root) {
    if (null == root) return "<NULL>";
    return DebugUtil.psiToString(root, false);
  }

  public static String formatElementPath(PsiElement element, boolean includeDirectories) {
    Stack<PsiElement> elements = new Stack<>();
    while (null != element) {
      if (!includeDirectories && element instanceof PsiFile) {
        break;
      }
      elements.push(element);
      element = element.getParent();
    }

    StringBuilder s = new StringBuilder();
    int indent = 0;
    while (!elements.isEmpty()) {
      for (int j = 0; j < indent; j++) {
        s.append("->");
      }
      indent++;

      element = elements.pop();
      s.append(element.getClass().getName());
      s.append('\n');
    }
    return s.toString();
  }

  //private static HaxeDebugLogger createLogger(String loggerName) {
  //  HaxeDebugLogger log = HaxeDebugLogger.getLogger(loggerName);
  //  log.setLevel(Level.DEBUG);
  //  return log;
  //}
}
