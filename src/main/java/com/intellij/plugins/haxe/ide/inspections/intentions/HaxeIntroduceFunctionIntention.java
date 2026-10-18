package com.intellij.plugins.haxe.ide.inspections.intentions;

import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeBlockStatement;
import com.intellij.plugins.haxe.lang.psi.HaxeCallExpression;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import static com.intellij.plugins.haxe.ide.inspections.intentions.HaxeIntroduceUtil.findInsertBeforeElementFunction;

/** Creates a local function for an unresolved call inside a block, its parameters derived from the call's arguments. */
public class HaxeIntroduceFunctionIntention extends HaxeUnresolvedSymbolIntentionBase<HaxeCallExpression> {

  private final String methodName;

  public HaxeIntroduceFunctionIntention(HaxeCallExpression callExpression) {
    super(callExpression);
    PsiElement[] children = callExpression.getExpression().getChildren();
    methodName = children[children.length - 1].getText();
  }

  @Override
  public @IntentionName @NotNull String getText() {
    return HaxeBundle.message("haxe.intention.create.local.function", methodName);
  }

  @Override
  protected PsiFile perform(@NotNull Project project, @NotNull PsiElement element, @NotNull Editor editor, boolean preview) {
    HaxeBlockStatement block = PsiTreeUtil.getParentOfType(element, HaxeBlockStatement.class);
    if (block != null) {
      PsiElement anchor = findInsertBeforeElementFunction(element, block);
      PsiElement methodDeclaration = generateDeclaration(project).copy();
      anchor.getParent().addBefore(methodDeclaration, anchor);
      anchor.getParent().addBefore(createNewLine(project), anchor);
      CodeStyleManager.getInstance(project).reformat(methodDeclaration);
    }
    return element.getContainingFile();
  }

  private PsiElement generateDeclaration(@NotNull Project project) {
    HaxeCallExpression call = myPsiElementPointer.getElement();
    String returnType = callReturnTypeText(call);
    String function = """
      function %s (%s):%s {
        %s
      }
      """
      .formatted(methodName, parameterListFromArguments(callArguments(call), true), returnType, defaultReturnStatement(returnType));
    return HaxeElementGenerator.createMethodDeclaration(project, function);
  }
}
