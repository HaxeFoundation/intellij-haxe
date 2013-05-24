package com.intellij.plugins.haxe.util;

import com.intellij.openapi.util.Condition;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fedorkorotkov.
 */
public class HaxeImportUtil {
  public static List<HaxeImportStatement> findUnusedImports(PsiFile file) {
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
      public void visitImportStatement(@NotNull HaxeImportStatement o) {
        // stop
      }
    });

    List<HaxeImportStatement> allImportStatements = UsefulPsiTreeUtil.getAllImportStatements(file);
    List<HaxeImportStatement> usefulImportStatements = ContainerUtil.findAll(allImportStatements, new Condition<HaxeImportStatement>() {
      @Override
      public boolean value(HaxeImportStatement statement) {
        HaxeReferenceExpression referenceExpression = statement.getReferenceExpression();
        if (referenceExpression != null && referenceExpression.resolve() == null) {
          // don't know for sure
          return true;
        }
        for (HaxeClass haxeClass : classesInFile) {
          if (UsefulPsiTreeUtil.importStatementForClass(statement, haxeClass)) {
            return true;
          }
        }
        return false;
      }
    });

    List<HaxeImportStatement> uselessImportStatements = new ArrayList<HaxeImportStatement>(allImportStatements);
    uselessImportStatements.removeAll(usefulImportStatements);

    return uselessImportStatements;
  }
}
