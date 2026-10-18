package com.intellij.plugins.haxe.ide.inspections.intentions;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeMethodDeclaration;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.plugins.haxe.ide.inspections.intentions.HaxeIntroduceUtil.findInsertAfterElementForMethod;
import static com.intellij.plugins.haxe.ide.inspections.intentions.HaxeIntroduceUtil.findTypesRequiringImportsForMethodAndAddToFile;

/**
 * Creates a method on a target class for an unresolved symbol: the insert
 * point, the declaration text and the imports its types need are shared; the
 * subclass supplies the signature parts.
 */
abstract class HaxeIntroduceMethodIntentionBase<T extends PsiElement>
  extends HaxeUnresolvedSymbolIntentionBase<T>
  implements HighPriorityAction {

  protected final @NotNull SmartPsiElementPointer<HaxeClass> myPsiTargetPointer;
  protected final String methodName;

  protected HaxeIntroduceMethodIntentionBase(@NotNull T element, @NotNull String methodName, @NotNull HaxeClass targetClass) {
    super(element);
    this.methodName = methodName;
    myPsiTargetPointer = createPointer(targetClass);
  }

  /** The return type's text; {@code Dynamic} leaves the declaration untyped. */
  protected abstract String returnTypeText();

  /** The parameter list as the declaration writes it. */
  protected abstract String parameterListText();

  /** The argument types known at the use site, for the import check; empty when nothing is known. */
  protected abstract List<ResultHolder> knownParameterTypes();

  @Override
  public @IntentionName @NotNull String getText() {
    return HaxeBundle.message("haxe.intention.create.method", methodName);
  }

  @Override
  protected String getPreviewName() {
    HaxeClass aClass = myPsiTargetPointer.getElement();
    return aClass == null ? null : aClass.getQualifiedName();
  }

  @Override
  protected PsiElement getTargetPsi() {
    return findInsertAfterElementForMethod(myPsiElementPointer.getElement(), myPsiTargetPointer.getElement(), false);
  }

  @Override
  protected PsiFile perform(@NotNull Project project, @NotNull PsiElement element, @NotNull Editor editor, boolean preview) {
    PsiElement anchor = findInsertAfterElementForMethod(element, myPsiTargetPointer.getElement(), preview);

    PsiElement methodDeclaration = generateDeclaration(project).copy();
    methodDeclaration = anchor.getParent().addAfter(methodDeclaration, anchor);
    anchor.getParent().addBefore(createNewLine(project), methodDeclaration);
    methodDeclaration = CodeStyleManager.getInstance(project).reformat(methodDeclaration);

    if (!preview && methodDeclaration instanceof HaxeMethodDeclaration declaration) {
      HaxeMethodModel newModel = declaration.getModel();
      List<HaxeParameterModel> parameters = newModel.getParameters();
      ResultHolder returnType = newModel.getReturnType(null);
      ResultHolder knownReturnType = guessElementType(myPsiElementPointer.getElement());
      if (knownReturnType.isDynamic() || knownReturnType.isUnknown()) knownReturnType = null;
      findTypesRequiringImportsForMethodAndAddToFile(parameters, knownParameterTypes(), returnType, knownReturnType, anchor.getContainingFile());
    }
    return anchor.getContainingFile();
  }

  private PsiElement generateDeclaration(@NotNull Project project) {
    String returnType = returnTypeText();
    String typeTag = returnType.equals("Dynamic") ? "" : ":" + returnType;
    String optionalStaticKeyword = needsToBeStatic() ? "static" : "";
    String privateKeyword = needsToBePublic() ? "public" : "private";
    String function = """
      %s %s function %s (%s)%s {
        %s
      }
      """
      .formatted(privateKeyword, optionalStaticKeyword, methodName, parameterListText(), typeTag, defaultReturnStatement(returnType));
    return HaxeElementGenerator.createMethodDeclaration(project, function);
  }
}
