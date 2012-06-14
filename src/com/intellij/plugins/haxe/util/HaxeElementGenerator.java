package com.intellij.plugins.haxe.util;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeExpressionCodeFragmentImpl;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeElementGenerator {
  public static PsiElement createStatementFromText(Project myProject, String text) {
    final PsiFile dummyFile = createDummyFile(myProject, HaxeCodeGenerateUtil.wrapStatement(text).getFirst());
    final HaxeClass haxeClass = PsiTreeUtil.getChildOfType(dummyFile, HaxeClass.class);
    assert haxeClass != null;
    final HaxeFunctionDeclarationWithAttributes mainMethod =
      (HaxeFunctionDeclarationWithAttributes)haxeClass.getMethods().iterator().next();
    final HaxeBlockStatement statement = mainMethod.getBlockStatement();
    assert statement != null;
    return statement.getChildren()[0];
  }

  public static HaxeVarDeclarationPart createVarDeclarationPart(Project myProject, String text) {
    final PsiFile dummyFile = createDummyFile(myProject, HaxeCodeGenerateUtil.wrapFunction(text).getFirst());
    final HaxeClass haxeClass = PsiTreeUtil.getChildOfType(dummyFile, HaxeClass.class);
    assert haxeClass != null;
    return (HaxeVarDeclarationPart)haxeClass.getFields().iterator().next();
  }

  public static List<HaxeNamedComponent> createFunctionsFromText(Project myProject, String text) {
    final PsiFile dummyFile = createDummyFile(myProject, HaxeCodeGenerateUtil.wrapFunction(text).getFirst());
    final HaxeClass haxeClass = PsiTreeUtil.getChildOfType(dummyFile, HaxeClass.class);
    assert haxeClass != null;
    return haxeClass.getMethods();
  }

  @Nullable
  public static HaxeIdentifier createIdentifierFromText(Project myProject, String name) {
    final HaxeImportStatement importStatement = createImportStatementFromPath(myProject, name);
    if (importStatement == null) {
      return null;
    }
    return PsiTreeUtil.findChildOfType(importStatement.getExpression(), HaxeIdentifier.class);
  }

  @Nullable
  public static HaxeImportStatement createImportStatementFromPath(Project myProject, String path) {
    final PsiFile dummyFile = createDummyFile(myProject, "import " + path + ";");
    return PsiTreeUtil.getChildOfType(dummyFile, HaxeImportStatement.class);
  }

  @Nullable
  public static HaxePackageStatement createPackageStatementFromPath(Project myProject, String path) {
    final PsiFile dummyFile = createDummyFile(myProject, "package " + path + ";");
    return PsiTreeUtil.getChildOfType(dummyFile, HaxePackageStatement.class);
  }


  public static PsiFile createDummyFile(Project myProject, String text) {
    final PsiFileFactory factory = PsiFileFactory.getInstance(myProject);
    final String name = "dummy." + HaxeFileType.HAXE_FILE_TYPE.getDefaultExtension();
    final LightVirtualFile virtualFile = new LightVirtualFile(name, HaxeFileType.HAXE_FILE_TYPE, text);
    final PsiFile psiFile = ((PsiFileFactoryImpl)factory).trySetupPsiForFile(virtualFile, HaxeLanguage.INSTANCE, false, true);
    assert psiFile != null;
    return psiFile;
  }

  public static PsiFile createExpressionCodeFragment(Project myProject, String text, PsiElement context, boolean resolveScope) {
    final String name = "dummy." + HaxeFileType.HAXE_FILE_TYPE.getDefaultExtension();
    HaxeExpressionCodeFragmentImpl codeFragment = new HaxeExpressionCodeFragmentImpl(myProject, name, text, true);
    codeFragment.setContext(context);
    return codeFragment;
  }
}
