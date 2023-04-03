/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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
package com.intellij.plugins.haxe.ide.inspections;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.annotator.HaxeAnnotatingVisitor;
import com.intellij.plugins.haxe.lang.psi.HaxeMethodDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeFieldDeclaration;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Usievaład Čorny on 04.04.2016.
 **/
public class HaxeDeprecatedInspection extends LocalInspectionTool {
  @NotNull
  public String getGroupDisplayName() {
    return HaxeBundle.message("inspections.group.name");
  }

  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return HaxeBundle.message("haxe.inspection.deprecated.symbol");
  }

  @Override
  public boolean isEnabledByDefault() {
    return true;
  }

  @NotNull
  @Override
  public String getShortName() {
    return "HaxeDeprecatedSymbol";
  }

  @Nullable
  @Override
  public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull final InspectionManager manager, final boolean isOnTheFly) {
    if (!(file instanceof HaxeFile)) return null;
    final List<ProblemDescriptor> result = new ArrayList<ProblemDescriptor>();
    new HaxeAnnotatingVisitor() {
      @Override
      protected void handleDeprecatedFunctionDeclaration(@NotNull HaxeMethodDeclaration function) {
        PsiIdentifier nameIdentifier = function.getNameIdentifier();

        if (nameIdentifier != null) {
          result.add(manager.createProblemDescriptor(
            nameIdentifier,
            TextRange.from(0, nameIdentifier.getTextLength()),
            getDisplayName(),
            ProblemHighlightType.LIKE_DEPRECATED,
            isOnTheFly
          ));
        }
      }

      @Override
      protected void handleDeprecatedCallExpression(HaxeReferenceExpression referenceExpression) {
        PsiIdentifier identifier = referenceExpression.getIdentifier();
        result.add(manager.createProblemDescriptor(
          identifier,
          TextRange.from(0, identifier.getTextLength()),
          getDisplayName(),
          ProblemHighlightType.LIKE_DEPRECATED,
          isOnTheFly
        ));
      }

      @Override
      protected void handleDeprecatedFieldDeclaration(HaxeFieldDeclaration varDeclaration) {
        PsiIdentifier nameIdentifier = varDeclaration.getNameIdentifier();

        result.add(manager.createProblemDescriptor(
          nameIdentifier,
          TextRange.from(0, nameIdentifier.getTextLength()),
          getDisplayName(),
          ProblemHighlightType.LIKE_DEPRECATED,
          isOnTheFly
        ));
      }
    }.visitFile(file);
    return ArrayUtil.toObjectArray(result, ProblemDescriptor.class);
  }
}