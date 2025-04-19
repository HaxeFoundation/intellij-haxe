package com.intellij.plugins.haxe.ide.inspections.intentions;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeBlockStatement;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class HaxeIntroduceVariableIntention
  extends HaxeUnresolvedSymbolIntentionBase<HaxeReferenceExpression>
  implements HighPriorityAction {

  private final String expressionText;

  public HaxeIntroduceVariableIntention(HaxeReferenceExpression expression) {
    super(expression);
    @NotNull PsiElement[] children = expression.getChildren();
    expressionText = children[children.length-1].getText();
  }


  @Override
  public @IntentionName @NotNull String getText() {
    return "Create local variable '" + expressionText + "'";
  }


  @Override
  protected PsiFile perform(@NotNull Project project, @NotNull PsiElement element, @NotNull Editor editor, boolean preview) {
    HaxeBlockStatement block = PsiTreeUtil.getParentOfType(element, HaxeBlockStatement.class);
    if (block != null) {
      PsiElement insertBeforeElement = findInsertBeforeElement(element, block);
      PsiElement variableDeclaration = generateDeclaration(project).copy();
      block.addBefore(variableDeclaration, insertBeforeElement);
      block.addBefore(createNewLine(project), insertBeforeElement);
    }
    return element.getContainingFile();
  }


  private PsiElement generateDeclaration(@NotNull Project project) {
    return HaxeElementGenerator.createStatementFromText(project, "var " + expressionText + ":" + guessElementTypeText() + ";");
  }



  private static @NotNull PsiElement findInsertBeforeElement(@NotNull PsiElement startElement, HaxeBlockStatement block) {
    PsiElement insertBeforeElement = startElement;
    PsiElement parent = startElement.getParent();

    while (parent != null && parent != block) {
      insertBeforeElement = parent;
      parent = parent.getParent();
    }
    return insertBeforeElement;
  }
}
