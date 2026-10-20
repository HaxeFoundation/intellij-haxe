package com.intellij.plugins.haxe.ide.inspections.intentions;

import com.intellij.codeInsight.CodeInsightUtilCore;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeFieldDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeTypeTag;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Creates the field an unresolved reference names, after the last field declared before the use site. */
public class HaxeIntroduceFieldIntention extends HaxeIntroduceMemberIntentionBase {

  public HaxeIntroduceFieldIntention(@NotNull HaxeReferenceExpression expression, @NotNull HaxeClass targetClass) {
    super(expression, targetClass);
  }

  @Override
  public @IntentionName @NotNull String getText() {
    return HaxeBundle.message("haxe.intention.create.field", expressionText, scopeName());
  }

  @Override
  protected PsiFile perform(@NotNull Project project, PsiElement element, @NotNull Editor editor, boolean preview) {
    if (element instanceof HaxeReferenceExpression expression) {
      PsiFile containingFile = expression.getContainingFile();
      InsertInfo insertInfo = findInsertInfo(expression, preview);
      HaxeFieldDeclaration variableDeclaration = (HaxeFieldDeclaration)generateDeclaration(project).copy();
      PsiElement anchor = insertInfo.element();
      PsiUtilCore.ensureValid(anchor);
      if (insertInfo.isAfter()) {
        variableDeclaration = (HaxeFieldDeclaration)anchor.getParent().addAfter(variableDeclaration, anchor);
      }
      else {
        variableDeclaration = (HaxeFieldDeclaration)anchor.getParent().addBefore(variableDeclaration, anchor);
      }
      CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(containingFile);
      CodeStyleManager.getInstance(project)
        .reformatNewlyAddedElement(variableDeclaration.getParent().getNode(), variableDeclaration.getNode());
      if (!preview) {
        addImportsFor(variableDeclaration, anchor.getContainingFile());
      }
      return variableDeclaration.getContainingFile();
    }
    return element.getContainingFile();
  }

  /** Adds imports for the types in the guessed field type that the generated declaration cannot resolve. */
  private void addImportsFor(@NotNull HaxeFieldDeclaration variableDeclaration, @NotNull PsiFile containingFile) {
    HaxeTypeTag typeTag = variableDeclaration.getTypeTag();
    if (typeTag == null) return;
    ResultHolder guessedType = guessElementType(myPsiElementPointer.getElement());
    ResultHolder newElementType = HaxeTypeResolver.getTypeFromTypeTag(typeTag, containingFile);
    HaxeIntroduceUtil.findTypesRequiringImportsForFieldAndAddToFile(newElementType, guessedType, containingFile);
  }

  private HaxeFieldDeclaration generateDeclaration(@NotNull Project project) {
    String privateKeyword = needsToBePublic() ? "public" : "private";
    String text = privateKeyword + (needsToBeStatic() ? " static" : "") + " var " + expressionText + ":" + guessElementTypeText() + ";";
    return HaxeElementGenerator.createVarDeclaration(project, text);
  }

  /** After the last member declared before the use site; before the first when none is. */
  @Override
  protected InsertInfo insertPointAmong(@NotNull PsiElement expression, @NotNull List<? extends PsiElement> members) {
    PsiElement element = members.getFirst();
    boolean isAfter = false;
    for (PsiElement next : members) {
      if (next.getTextRange().getEndOffset() < expression.getTextOffset()) {
        element = next;
        isAfter = true;
      }
      else {
        break;
      }
    }
    return new InsertInfo(element, isAfter);
  }
}
