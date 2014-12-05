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
package com.intellij.plugins.haxe.util;

import com.intellij.openapi.util.Condition;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by fedorkorotkov.
 */
public class HaxeImportUtil {
  public static List<HaxeImportStatementRegular> findUnusedImports(PsiFile file) {
    final List<HaxeClass> classesInFile = getReferencedClasses(file);

    List<HaxeImportStatementRegular> filteredUsefulImports = new ArrayList<HaxeImportStatementRegular>();

    List<HaxeImportStatementRegular> allImportStatements = UsefulPsiTreeUtil.getAllImportStatements(file);
    final List<HaxeImportStatementRegular> usefulImportStatements = ContainerUtil.findAll(allImportStatements, new Condition<HaxeImportStatementRegular>() {
      @Override
      public boolean value(HaxeImportStatementRegular statement) {
        final HaxeImportStatementRegular regularImport = statement;
        if(regularImport != null) {
          final HaxeReferenceExpression referenceExpression = regularImport.getReferenceExpression();

          if (null == referenceExpression || referenceExpression.resolve() == null) {
            // don't know for sure
            return true;
          }
        }
        for (HaxeClass haxeClass : classesInFile) {
          if (UsefulPsiTreeUtil.importStatementForClass(statement, haxeClass)) {
            return true;
          }
        }
        return false;
      }
    });

    boolean alreadyAdded = false;

    for (int i = 0; i < usefulImportStatements.size(); i++) {
      for (int j = 0; j < filteredUsefulImports.size(); j++) {
        if (usefulImportStatements.get(i).getReferenceExpression().getText().equals(
          filteredUsefulImports.get(j).getReferenceExpression().getText())) {
          alreadyAdded = true;
          break;
        }
      }

      if (!alreadyAdded) {
        filteredUsefulImports.add(usefulImportStatements.get(i));
      }
    }

    List<HaxeImportStatementRegular> uselessImportStatements = new ArrayList<HaxeImportStatementRegular>(allImportStatements);
    uselessImportStatements.removeAll(filteredUsefulImports);

    return uselessImportStatements;
  }

  public static List<HaxeClass> getReferencedClasses(PsiFile file) {
    final List<HaxeClass> classesInFile = new ArrayList<HaxeClass>();
    file.acceptChildren(new HaxeRecursiveVisitor() {
      @Override
      public void visitElement(PsiElement element) {
        super.visitElement(element);
        if (element instanceof HaxeReference) {
          HaxeClass haxeClass = ((HaxeReference)element).resolveHaxeClass().getHaxeClass();
          if (haxeClass != null) {
            classesInFile.add(haxeClass);
          }
        }
      }

      @Override
      public void visitImportStatementRegular(@NotNull HaxeImportStatementRegular o) {
        // stop
      }

      @Override
      public void visitImportStatementWithInSupport(@NotNull HaxeImportStatementWithInSupport o) {
        // stop
      }

      @Override
      public void visitImportStatementWithWildcard(@NotNull HaxeImportStatementWithWildcard o) {
        // stop
      }
    });
    return classesInFile;
  }

  public static List<HaxeImportStatementWithInSupport> findUnusedInImports(PsiFile file) {
    final List<String> referencesInFile = new ArrayList<String>();
    file.acceptChildren(new HaxeRecursiveVisitor() {
      @Override
      public void visitElement(PsiElement element) {
        super.visitElement(element);
        if (element instanceof HaxeReference) {
          referencesInFile.add(element.getText());
        }
      }

      @Override
      public void visitImportStatementRegular(@NotNull HaxeImportStatementRegular o) {
        // stop
      }

      @Override
      public void visitImportStatementWithInSupport(@NotNull HaxeImportStatementWithInSupport o) {
        // stop
      }

      @Override
      public void visitImportStatementWithWildcard(@NotNull HaxeImportStatementWithWildcard o) {
        // stop
      }
    });

    List<HaxeImportStatementWithInSupport> filteredUsefulImports = new ArrayList<HaxeImportStatementWithInSupport>();

    List<HaxeImportStatementWithInSupport> allImportStatementWithInSupports = UsefulPsiTreeUtil.getAllInImportStatements(file);
    List<HaxeImportStatementWithInSupport> usefulImportStatementWithInSupports =
      ContainerUtil.findAll(allImportStatementWithInSupports, new Condition<HaxeImportStatementWithInSupport>() {
        @Override
        public boolean value(HaxeImportStatementWithInSupport importStatementWithInSupport) {
          return referencesInFile.contains(importStatementWithInSupport.getIdentifier().getText());
        }
      });

    boolean alreadyAdded = false;

    for (int i = 0; i < usefulImportStatementWithInSupports.size(); i++) {
      for (int j = 0; j < filteredUsefulImports.size(); j++) {
        if (usefulImportStatementWithInSupports.get(i).getReferenceExpression().getText().equals(
          filteredUsefulImports.get(j).getReferenceExpression().getText())
          && usefulImportStatementWithInSupports.get(i).getIdentifier().getText().equals(
          filteredUsefulImports.get(j).getIdentifier().getText())) {
          alreadyAdded = true;
          break;
        }
      }

      if (!alreadyAdded) {
        filteredUsefulImports.add(usefulImportStatementWithInSupports.get(i));
      }
    }

    List<HaxeImportStatementWithInSupport> uselessImportStatements = new ArrayList<HaxeImportStatementWithInSupport>(allImportStatementWithInSupports);
    uselessImportStatements.removeAll(filteredUsefulImports);

    return uselessImportStatements;
  }

  public static List<HaxeImportStatementWithWildcard> findUnusedInImportsWithWildcards(PsiFile file) {
    final List<HaxeClass> classesInFile = new ArrayList<HaxeClass>();
    final List<HaxeReference> referenceList = new ArrayList<HaxeReference>();
    file.acceptChildren(new HaxeRecursiveVisitor() {
      @Override
      public void visitElement(PsiElement element) {
        super.visitElement(element);
        if (element instanceof HaxeReference) {
          HaxeReference reference = (HaxeReference)element;
          HaxeClass haxeClass = reference.resolveHaxeClass().getHaxeClass();
          if (haxeClass != null) {
            classesInFile.add(haxeClass);
          }

          referenceList.add(reference);
        }
      }

      @Override
      public void visitImportStatementRegular(@NotNull HaxeImportStatementRegular o) {
        // stop
      }

      @Override
      public void visitImportStatementWithInSupport(@NotNull HaxeImportStatementWithInSupport o) {
        // stop
      }

      @Override
      public void visitImportStatementWithWildcard(@NotNull HaxeImportStatementWithWildcard o) {
        // stop
      }
    });

    List<HaxeClass> alreadyImportedClassList = getAlreadyImportedClasses(file, classesInFile);

    classesInFile.removeAll(alreadyImportedClassList);

    List<HaxeImportStatementWithWildcard> filteredUsefulImports = new ArrayList<HaxeImportStatementWithWildcard>();

    List<HaxeImportStatementWithWildcard> allImportStatementsWithWildcard = UsefulPsiTreeUtil.getAllImportStatementsWithWildcard(file);
    List<HaxeImportStatementWithWildcard> usefulImportStatementWithInSupports =
      ContainerUtil.findAll(allImportStatementsWithWildcard, new Condition<HaxeImportStatementWithWildcard>() {
        @Override
        public boolean value(HaxeImportStatementWithWildcard importStatementWithInSupport) {
          String qName = UsefulPsiTreeUtil.getQNameForImportStatementWithWildcardType(importStatementWithInSupport);
          boolean wildcardForType = UsefulPsiTreeUtil
            .isImportStatementWildcardForType(qName);

          if (wildcardForType) {
            HaxeClass haxeClass = HaxeResolveUtil.findClassByQName(qName, importStatementWithInSupport.getContext());

            if (haxeClass != null) {
              for (HaxeReference reference : referenceList) {
                String referenceText = reference.getText();
                HaxeNamedComponent namedSubComponent = HaxeResolveUtil.findNamedSubComponent(haxeClass, referenceText);

                if (namedSubComponent != null && namedSubComponent.isStatic()) {
                  return true;
                }
              }
            }
          }
          else {
            List<HaxeClass>
              classesForImportStatementWithWildcard = UsefulPsiTreeUtil.getClassesForImportStatementWithWildcard(importStatementWithInSupport);

            return !classesInFile.isEmpty() && !Collections.disjoint(classesInFile, classesForImportStatementWithWildcard);
          }

          return false;
        }
      });

    boolean alreadyAdded = false;

    for (int i = 0; i < usefulImportStatementWithInSupports.size(); i++) {
      for (int j = 0; j < filteredUsefulImports.size(); j++) {
        if (usefulImportStatementWithInSupports.get(i).getReferenceExpression().getText().equals(
          filteredUsefulImports.get(j).getReferenceExpression().getText())) {
          alreadyAdded = true;
          break;
        }
      }

      if (!alreadyAdded) {
        filteredUsefulImports.add(usefulImportStatementWithInSupports.get(i));
      }
    }

    List<HaxeImportStatementWithWildcard> uselessImportStatements = new ArrayList<HaxeImportStatementWithWildcard>(allImportStatementsWithWildcard);
    uselessImportStatements.removeAll(filteredUsefulImports);

    return uselessImportStatements;
  }

  public static List<HaxeClass> getAlreadyImportedClasses(PsiFile file, List<HaxeClass> classesInFile) {
    List<HaxeImportStatementRegular> importStatements = UsefulPsiTreeUtil.getAllImportStatements(file);

    List<HaxeClass> alreadyImportedClassList = new ArrayList<HaxeClass>();

    for (HaxeImportStatementRegular importStatementRegular : importStatements) {
      HaxeReferenceExpression referenceExpression = importStatementRegular.getReferenceExpression();
      if (referenceExpression != null) {
        PsiElement psiElement = referenceExpression.resolve();
        if (psiElement != null) {
          for (HaxeClass haxeClass : classesInFile) {
            if (haxeClass.getContainingFile() == psiElement.getContainingFile()) {
              if (!alreadyImportedClassList.contains(haxeClass)) {
                alreadyImportedClassList.add(haxeClass);
              }
            }
          }
        }
      }
    }
    return alreadyImportedClassList;
  }

  public static List<HaxeClass> getClassesUsedFromImportStatementWithWildcard(PsiFile file, HaxeImportStatementWithWildcard importStatementWithWildcard) {
    List<HaxeClass>
      classesForImportStatementWithWildcard = UsefulPsiTreeUtil.getClassesForImportStatementWithWildcard(importStatementWithWildcard);

    final List<HaxeClass> referencedClasses = getReferencedClasses(file);
    List<HaxeClass> alreadyImportedClasses = getAlreadyImportedClasses(file, referencedClasses);

    referencedClasses.removeAll(alreadyImportedClasses);
    classesForImportStatementWithWildcard.removeAll(alreadyImportedClasses);

    List<HaxeClass> classesUsedClassesFromImportStatementWithWildcard =
      ContainerUtil.findAll(classesForImportStatementWithWildcard, new Condition<HaxeClass>() {
        @Override
        public boolean value(HaxeClass haxeClass) {
          return referencedClasses.contains(haxeClass);
        }
      });

    return classesUsedClassesFromImportStatementWithWildcard;
  }
}
