/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
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
package com.intellij.plugins.haxe.ide.annotator;

import com.google.common.base.Strings;
import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxePackageStatement;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeVisitor;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.util.PsiUtil;
import org.apache.commons.lang.ArrayUtils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class HaxeSemanticAnnotator implements Annotator {
  static private Key<List<String>> KEY_HAXE_ERROR = new Key<List<String>>("KEY_HAXE_ERROR");
  static private Key<Long> KEY_ANALYZED_METHOD_TIME = new Key<Long>("HAXE_ANALYZED_METHOD");

  @Override
  public void annotate(PsiElement element, AnnotationHolder holder) {
    PsiFile file = element.getContainingFile();
    // Analyze upon file changes
    Long analyzedFileTime = file.getUserData(KEY_ANALYZED_METHOD_TIME);
    if (analyzedFileTime == null || analyzedFileTime != file.getModificationStamp()) {
      analyze(file);
      file.putUserData(KEY_ANALYZED_METHOD_TIME, file.getModificationStamp());
    }

    List<String> errorMessages = element.getUserData(KEY_HAXE_ERROR);
    if (errorMessages != null) {
      for (String message : errorMessages) {
        holder.createErrorAnnotation(element, message);
      }
    }
  }

  private static void clearErrors(PsiElement element) {
    element.putUserData(KEY_HAXE_ERROR, null);
  }

  private static void addError(PsiElement element, String message) {
    List<String> data = element.getUserData(KEY_HAXE_ERROR);
    if (data == null) {
      element.putUserData(KEY_HAXE_ERROR, new LinkedList<String>());
    }
    element.getUserData(KEY_HAXE_ERROR).add(message);
  }

  static void analyze(PsiElement element) {
    clearErrors(element);

    if (element instanceof PsiJavaToken) {
      if (((PsiJavaToken)element).getTokenType() == HaxeTokenTypes.LITINT) {
        //element.putUserData(KEY_HAXE_ERROR, "litint!");
      }
    }
    else if (element instanceof HaxePackageStatement) {
      HaxeReferenceExpression expression = ((HaxePackageStatement)element).getReferenceExpression();
      String packageName = expression.getText();
      PsiFile file = element.getContainingFile();
      String expectedPath = packageName.replace('.', '/');
      String actualPath = getDirectoryPath(file.getParent());

      for (String s : packageName.split("\\.")) {
        if (!s.substring(0, 1).toLowerCase().equals(s.substring(0, 1))) {
          addError(element, "Package name '" + s + "' must start with a lower case character");
        }
      }

      if (!actualPath.endsWith(expectedPath)) {
        addError(element, "Invalid package name! '" + expectedPath + "' not contained in '" + actualPath + "'");
      }
    }

    for (ASTNode node : element.getNode().getChildren(null)) {
      analyze(node.getPsi());
    }
  }

  static private String getDirectoryPath(PsiDirectory dir) {
    String out = "";
    while (dir != null) {
      if (out.length() != 0) out = "/" + out;
      out = dir.getName() + out;
      dir = dir.getParent();
    }
    return out;
  }
}
