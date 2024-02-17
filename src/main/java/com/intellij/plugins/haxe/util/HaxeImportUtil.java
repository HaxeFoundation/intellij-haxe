/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2017 Ilya Malanin
 * Copyright 2019 Eric Bishton
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
import com.intellij.psi.PsiPackage;
import com.intellij.util.SmartList;
import com.intellij.util.containers.MultiMap;
import lombok.CustomLog;

import org.jetbrains.annotations.NotNull;

import java.util.*;

@CustomLog
public class HaxeImportUtil {
  //static {log.setLevel(LogLevel.DEBUG);}

  public static List<HaxeImportStatement> findUnusedImports(PsiFile file) {
    final Collection<PsiElement> externalReferences = getExternalReferences(file);

    List<HaxeImportStatement> allImportStatements = ((HaxeFile)file).getImportStatements();

    boolean hasWildcards = false;
    for (HaxeImportStatement allImportStatement : allImportStatements) {
      if (allImportStatement.getModel().hasWildcard()) {
        hasWildcards = true;
        break;
      }
    }

    List<HaxeImportStatement> usefulStatements = new ArrayList<>();
    Set<HaxeImportStatement> uniqueValues = new HashSet<>();

    for (HaxeImportStatement statement : allImportStatements) {
      for (PsiElement referencedElement : externalReferences) {
        if (isStatementExposesReference(statement, referencedElement)) {
            if (uniqueValues.add(statement)) {
              usefulStatements.add(statement);
            }
          break;
        }
      }
    }

    if (hasWildcards) {
      removeUnusedWildcards(externalReferences, usefulStatements);
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

  private static void removeUnusedWildcards(Collection<PsiElement> classesInFile, List<HaxeImportStatement> usefulStatements) {
    final MultiMap<PsiElement, HaxeImportModel> referenceMap = new MultiMap<>();

    usefulStatements.forEach(statement -> classesInFile.forEach(referencedElement -> {
      if (referencedElement instanceof HaxeClass ||
          referencedElement instanceof HaxeFieldDeclaration ||
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


  public static Collection<PsiElement> getExternalReferences(@NotNull PsiFile file) {
    final Map<PsiElement, PsiElement> result = new HashMap<>();
    final List<String> names = new ArrayList<>();

    file.acceptChildren(new HaxeRecursiveVisitor() {
      @Override
      public void visitElement(PsiElement element) {
        if (element instanceof HaxePackageStatement || element instanceof HaxeImportStatement || element instanceof HaxeUsingStatement) return;
        if (element instanceof  HaxeCallExpression callExpression) {
          element = callExpression.getExpression();
        }
        if (element instanceof HaxeReference reference) {

          PsiElement referencedElement = reference.resolve();
          // makes sure that even if we have a fully qualified a.b.SomeClass added to the list
          // that any reference of just the class name (SomeClass) without package structure is also added as  reference
          String qualifiedName = reference.getQualifiedName();
          if (!names.contains(qualifiedName)) {

            boolean qualified = reference.isQualified();
            if (!(qualified || referencedElement instanceof PsiPackage)){
              result.put(referencedElement, element);
              names.add(qualifiedName);
            }
            if (qualified) {
              if (referencedElement instanceof HaxePsiField) {
                result.put(referencedElement, element);
                names.add(qualifiedName);
              }
              if (referencedElement instanceof HaxeMethod) {
                result.put(referencedElement, element);
                names.add(qualifiedName);
              }
              else if (referencedElement instanceof HaxeClass) {
                result.put(referencedElement, element);
                names.add(qualifiedName);
              }
              else  if (referencedElement instanceof HaxeIdentifier) {
                result.put(referencedElement, element);
                names.add(qualifiedName);
              }
              else  if (referencedElement instanceof HaxeImportAlias) {
                result.put(referencedElement, element);
                names.add(qualifiedName);
              }
            }
          }
        }

        super.visitElement(element);
      }

      private boolean inSameFile(PsiElement reference) {
        return reference.getContainingFile() != file;
      }

      @Override
      public void visitImportStatement(@NotNull HaxeImportStatement o) {
      }
    });

    if (log.isDebugEnabled()) {
      result.values().forEach(element -> log.debug(((HaxeReference)element).getReferenceNameElement().getText()));
    }
    return result.values();
  }
}
