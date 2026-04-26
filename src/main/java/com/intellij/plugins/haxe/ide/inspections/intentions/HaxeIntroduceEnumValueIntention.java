package com.intellij.plugins.haxe.ide.inspections.intentions;

import com.intellij.codeInsight.CodeInsightUtilCore;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiParserFacade;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class HaxeIntroduceEnumValueIntention extends HaxeUnresolvedSymbolIntentionBase<HaxeReferenceExpression> {

  protected final @NotNull SmartPsiElementPointer<HaxeClass> myPsiTargetPointer;

  private final String myModuleName;
  private final String myClassName;

  private final String expressionText;

  public HaxeIntroduceEnumValueIntention(@NotNull HaxeReferenceExpression expression,  @NotNull HaxeClass targetClass) {
    super(expression);
    @NotNull PsiElement[] children = expression.getChildren();
    expressionText = children[children.length-1].getText();
    myPsiTargetPointer = createPointer(targetClass);

    myModuleName = expression.getContainingFile().getName();
    myClassName = targetClass.getName();

  }


  @Override
  public @IntentionName @NotNull String getText() {
    String scope = myClassName != null ? myClassName : myModuleName;
    return "Create enum value '" + expressionText + "' in " + scope;
  }

  @Override
  protected String getPreviewName() {
    HaxeClass aClass = myPsiTargetPointer.getElement();
    return  aClass == null ? null : aClass.getQualifiedName();
  }


  protected PsiFile perform(@NotNull Project project, PsiElement element, @NotNull Editor editor, boolean preview) {
    if (element instanceof  HaxeReferenceExpression expression) {
      PsiFile containingFile = expression.getContainingFile();
      InsertInfo insertInfo = findInsertInfo(expression, preview);

      HaxeEnumValueDeclaration declaration = (HaxeEnumValueDeclaration)generateDeclaration(project).copy();

      PsiElement anchor = insertInfo.element();
      PsiUtilCore.ensureValid(anchor);

      if (insertInfo.isAfter()) {
        declaration = (HaxeEnumValueDeclaration) anchor.getParent().addAfter(declaration, anchor);
        anchor.getParent().addBefore(createNewLine(project), declaration);
      } else {
        declaration = (HaxeEnumValueDeclaration) anchor.getParent().addBefore(declaration, anchor);
        anchor.getParent().addAfter(createNewLine(project), declaration);
      }

      CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(containingFile);
      CodeStyleManager.getInstance(project)
        .reformatNewlyAddedElement(declaration.getParent().getNode(), declaration.getNode());

      return declaration.getContainingFile();
    }
    return element.getContainingFile();
  }

  protected PsiElement createNewLine(@NotNull Project project) {
    return PsiParserFacade.getInstance(project).createWhiteSpaceFromText("\n").copy();
  }

  private HaxeEnumValueDeclaration generateDeclaration(@NotNull Project project) {
    return HaxeElementGenerator.createEnumValueDeclaration(project, expressionText);
  }




  private @NotNull InsertInfo findInsertInfo(@NotNull HaxeReferenceExpression expression, boolean readOnly) {
    HaxeModule myModule = PsiTreeUtil.getStubOrPsiParentOfType(expression, HaxeModule.class);
    HaxeClass myClass = myPsiTargetPointer.getElement();
    if (myClass != null) {
      if (readOnly) myClass = copyFileAndReturnClonedPsiElement(myClass);
      return findInsertClass(expression, myClass);
    } else if (myModule != null) {
      List<? extends PsiElement> list = myModule.getModuleFieldDeclarationList();
      if (!list.isEmpty()) {
        return findLastFieldBeforeExpression(expression, list);
      }
      PsiElement sibling = myModule.getPrevSibling();
      if (sibling != null) return new InsertInfo (sibling, false);
    }
    return new InsertInfo (expression.getContainingFile().getFirstChild(), true);
  }

  private static @NotNull InsertInfo findInsertClass(PsiElement expression, HaxeClass myClass) {
    List<? extends PsiElement>  list = Arrays.stream(myClass.getFields()).toList();
    if (!list.isEmpty()) {
      return findLastFieldBeforeExpression(expression, list);
    }else {
      PsiElement brace = myClass.getLBrace();
      return brace != null ? new InsertInfo(brace, true) : new InsertInfo(myClass, false);
    }
  }

  private static InsertInfo findLastFieldBeforeExpression(PsiElement expression, List<? extends PsiElement> list) {
    if (list.isEmpty()) {
      return new InsertInfo(null, true);
    } else {
      PsiElement last = list.getLast();
      return new InsertInfo(last, true);
    }
  }
}
