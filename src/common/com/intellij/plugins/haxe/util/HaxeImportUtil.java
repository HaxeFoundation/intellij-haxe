/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2017 Ilya Malanin
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

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeImportModel;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMember;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class HaxeImportUtil {
  public static List<HaxeImportStatement> findUnusedImports(PsiFile file) {
    final List<PsiElement> classesInFile = getExternalReferences(file);

    List<HaxeImportStatement> allImportStatements = ((HaxeFile)file).getImportStatements();

    final boolean hasWildcards = allImportStatements.stream().anyMatch(statement -> statement.getModel().hasWildcard());

    List<HaxeImportStatement> usefulStatements = allImportStatements
      .stream()
      .filter(statement -> classesInFile
        .stream()
        .anyMatch(referencedElement -> isStatementExposesReference(statement, referencedElement)))
      .distinct()
      .collect(Collectors.toList());

    if (hasWildcards) {
      removeUnusedWildcards(classesInFile, usefulStatements);
    }

    usefulStatements.forEach(allImportStatements::remove);

    return allImportStatements;
  }

  public static boolean isStatementExposesReference(HaxeImportStatement statement, PsiElement referencedElement) {
    return exposeReference(statement, referencedElement) != null;
  }

  public static PsiElement exposeReference(HaxeImportStatement statement, PsiElement referencedElement) {
    PsiElement result = null;
    if (referencedElement instanceof HaxeNamedComponent) {
      result = statement.getModel().exposeByName(((HaxeNamedComponent)referencedElement).getName());
    }
    if (result == null && referencedElement instanceof HaxeReference) {
      result = statement.getModel().exposeByName(referencedElement.getText());
    }
    return result;
  }

  private static void removeUnusedWildcards(List<PsiElement> classesInFile, List<HaxeImportStatement> usefulStatements) {
    final MultiMap<PsiElement, HaxeImportModel> referenceMap = new MultiMap<>();

    usefulStatements.forEach(statement -> classesInFile.forEach(referencedElement -> {
      if (referencedElement instanceof HaxeClass ||
          referencedElement instanceof HaxeVarDeclaration ||
          referencedElement instanceof HaxeMethod) {
        HaxeNamedComponent component = (HaxeNamedComponent)referencedElement;
        if (statement.getModel().exposeByName(component.getName()) != null) {
          referenceMap.putValue(referencedElement, statement.getModel());
        }
      }
      else if (referencedElement instanceof HaxeReference) {
        if (statement.getModel().exposeByName(referencedElement.getText()) != null) {
          referenceMap.putValue(referencedElement, statement.getModel());
        }
      }
    }));

    List<HaxeImportModel> uniqueWildcards = new SmartList<>();
    List<HaxeImportModel> notUniqueWildcards = new SmartList<>();
    for (PsiElement key : referenceMap.keySet()) {
      Collection<HaxeImportModel> imports = referenceMap.get(key);
      for (HaxeImportModel statement : imports) {
        if (statement.hasWildcard()) {
          if (imports.size() > 1 && !uniqueWildcards.contains(statement) && !notUniqueWildcards.contains(statement)) {
            notUniqueWildcards.add(statement);
          }
          if (imports.size() == 1 && !uniqueWildcards.contains(statement)) {
            uniqueWildcards.add(statement);
          }
        }
      }
    }

    notUniqueWildcards.forEach(model -> usefulStatements.remove(model.getBasePsi()));
  }


  public static List<PsiElement> getExternalReferences(PsiFile file) {
    final List<PsiElement> result = new ArrayList<>();
    file.acceptChildren(new HaxeRecursiveVisitor() {
      @Override
      public void visitElement(PsiElement element) {
        super.visitElement(element);

        if (element instanceof HaxeReference) {
          PsiElement resolvedElement = ((HaxeReference)element).resolve();
          if (resolvedElement != null) {
            if (resolvedElement instanceof HaxeVarDeclaration || resolvedElement instanceof HaxeMethod) {
              HaxeClass referencedClass = PsiTreeUtil.getParentOfType(element, HaxeClass.class, true);
              if (((PsiMember)resolvedElement).getContainingClass() != referencedClass) {
                result.add(element);
              }
            }
            else if (resolvedElement instanceof HaxeClass) {
              result.add(element);
            }
          }
          else {
            result.add(element);
          }
        }
      }

      @Override
      public void visitImportStatement(@NotNull HaxeImportStatement o) {
      }
    });

    return result;
  }
}
