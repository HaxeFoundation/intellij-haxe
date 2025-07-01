package com.intellij.plugins.haxe.ide.inspections.intentions;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.intellij.plugins.haxe.ide.inspections.intentions.HaxeIntroduceUtil.findInsertAfterElementForMethod;
import static com.intellij.plugins.haxe.ide.inspections.intentions.HaxeIntroduceUtil.findTypesRequiringImportsAndAddToFile;

public class HaxeIntroduceMethodForCallableIntention
  extends HaxeUnresolvedSymbolIntentionBase<HaxeReferenceExpression>
  implements HighPriorityAction {

  protected final @NotNull SmartPsiElementPointer<HaxeClass> myPsiTargetPointer;

  private final String methodName;

  public HaxeIntroduceMethodForCallableIntention(@NotNull HaxeReferenceExpression referenceExpression,  @NotNull HaxeClass targetClass) {
    super(referenceExpression);
    methodName = referenceExpression.getText();
    myPsiTargetPointer  = createPointer(targetClass);
  }



  @Override
  public @IntentionName @NotNull String getText() {
    return "Create method '" + methodName + "'";
  }

  @Override
  protected String getPreviewName() {
    HaxeClass aClass = myPsiTargetPointer.getElement();
    return  aClass == null ? null : aClass.getQualifiedName();
  }

  @Override
  protected PsiElement getTargetPsi() {
    return findInsertAfterElementForMethod(myPsiElementPointer.getElement(), myPsiTargetPointer.getElement(),false);
  }

  @Override
  protected PsiFile perform(@NotNull Project project, @NotNull PsiElement element, @NotNull Editor editor, boolean preview) {
    PsiElement anchor = findInsertAfterElementForMethod(element, myPsiTargetPointer.getElement(), preview);


    PsiElement methodDeclaration = generateDeclaration(project).copy();
    methodDeclaration = anchor.getParent().addAfter(methodDeclaration, anchor);
    anchor.getParent().addBefore(createNewLine(project), methodDeclaration);

//    generateMissingImports()

    methodDeclaration = CodeStyleManager.getInstance(project).reformat(methodDeclaration);
    if(!preview) {
      if(methodDeclaration instanceof HaxeMethodDeclaration declaration) {
        HaxeMethodModel newModel = declaration.getModel();
        List<HaxeParameterModel> parameters = newModel.getParameters();
        ResultHolder returnType = newModel.getReturnType(null);

        ResultHolder knownReturnType = guessElementType(myPsiElementPointer.getElement());
        if(knownReturnType.isDynamic() || knownReturnType.isUnknown()) knownReturnType = null;
        findTypesRequiringImportsAndAddToFile(parameters, getKnownParameterTypeList(), returnType, knownReturnType, anchor.getContainingFile());
      }
    }
    return anchor.getContainingFile();
  }




  private PsiElement generateDeclaration(@NotNull Project project) {
    String returnType = guessReturnElementType();
    String typeTag = returnType.equals("Dynamic") ? "" : ":"+ returnType;
    String returnStatement = determineReturnStatement(returnType);
    String optionalStaticKeyword = needsToBeStatic() ? "static" : "";
    String privateKeyword = needsToBePublic() ? "public" : "private";
    String function = """
      %s %s function %s (%s)%s {
        %s
      }
      """
      .formatted(privateKeyword, optionalStaticKeyword, methodName, generateParameterList(), typeTag, returnStatement);

    return HaxeElementGenerator.createMethodDeclaration(project, function);
  }

  private String determineReturnStatement(String type) {
    if (Objects.equals(type, SpecificHaxeClassReference.VOID)) return "";
    if (Objects.equals(type, SpecificHaxeClassReference.INT)) return " return 0;";
    if (Objects.equals(type, SpecificHaxeClassReference.FLOAT)) return " return 0.0;";
    if (Objects.equals(type, SpecificHaxeClassReference.BOOL)) return " return false;";
    return "return null;";
  }

  private String guessReturnElementType() {
    return SpecificHaxeClassReference.DYNAMIC;
  }

  private List<ResultHolder> getKnownParameterTypeList() {
    return List.of();
  }

  private String generateParameterList() {
    return "";
  }
}