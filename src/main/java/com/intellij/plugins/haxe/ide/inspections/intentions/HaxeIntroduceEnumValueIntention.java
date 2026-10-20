package com.intellij.plugins.haxe.ide.inspections.intentions;

import com.intellij.codeInsight.CodeInsightUtilCore;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeEnumValueDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Creates the enum value an unresolved reference names, after the enum's last value. */
public class HaxeIntroduceEnumValueIntention extends HaxeIntroduceMemberIntentionBase {

  public HaxeIntroduceEnumValueIntention(@NotNull HaxeReferenceExpression expression, @NotNull HaxeClass targetClass) {
    super(expression, targetClass);
  }

  @Override
  public @IntentionName @NotNull String getText() {
    return HaxeBundle.message("haxe.intention.create.enum.value", expressionText, scopeName());
  }

  @Override
  protected PsiFile perform(@NotNull Project project, PsiElement element, @NotNull Editor editor, boolean preview) {
    if (element instanceof HaxeReferenceExpression expression) {
      PsiFile containingFile = expression.getContainingFile();
      InsertInfo insertInfo = findInsertInfo(expression, preview);
      HaxeEnumValueDeclaration declaration = (HaxeEnumValueDeclaration)generateDeclaration(project).copy();
      PsiElement anchor = insertInfo.element();
      PsiUtilCore.ensureValid(anchor);
      if (insertInfo.isAfter()) {
        declaration = (HaxeEnumValueDeclaration)anchor.getParent().addAfter(declaration, anchor);
        anchor.getParent().addBefore(createNewLine(project), declaration);
      }
      else {
        declaration = (HaxeEnumValueDeclaration)anchor.getParent().addBefore(declaration, anchor);
        anchor.getParent().addAfter(createNewLine(project), declaration);
      }
      CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(containingFile);
      CodeStyleManager.getInstance(project)
        .reformatNewlyAddedElement(declaration.getParent().getNode(), declaration.getNode());
      return declaration.getContainingFile();
    }
    return element.getContainingFile();
  }

  private HaxeEnumValueDeclaration generateDeclaration(@NotNull Project project) {
    return HaxeElementGenerator.createEnumValueDeclaration(project, expressionText);
  }

  /** After the last value, whatever the use site. */
  @Override
  protected InsertInfo insertPointAmong(@NotNull PsiElement expression, @NotNull List<? extends PsiElement> members) {
    return new InsertInfo(members.getLast(), true);
  }
}
