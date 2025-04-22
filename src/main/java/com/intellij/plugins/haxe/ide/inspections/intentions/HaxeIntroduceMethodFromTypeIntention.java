package com.intellij.plugins.haxe.ide.inspections.intentions;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificFunctionReference;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.plugins.haxe.util.HaxeAddImportHelper;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.intellij.plugins.haxe.ide.inspections.intentions.HaxeIntroduceUtil.collectHaxeClasses;
import static com.intellij.plugins.haxe.ide.inspections.intentions.HaxeIntroduceUtil.findInsertAfterElementForMethod;

public class HaxeIntroduceMethodFromTypeIntention
  extends HaxeUnresolvedSymbolIntentionBase<PsiElement>
  implements HighPriorityAction {

  protected final @NotNull SmartPsiElementPointer<HaxeClass> myPsiTargetPointer;

  private final String methodName;
  private final String returnTypeText;
  private final List<ArgumentData> argumentInfo;

  record ArgumentData(String name, String typeText, boolean optional, boolean rest, Collection<SmartPsiElementPointer<HaxeClass>> types){}

  public HaxeIntroduceMethodFromTypeIntention(@NotNull SpecificFunctionReference functionReference, HaxeReferenceExpression referenceExpression, @NotNull HaxeClass targetClass) {
    super(referenceExpression);
    this.methodName = referenceExpression.getLastChild().getText();
    returnTypeText = functionReference.getReturnType().toPresentationString();
    argumentInfo =functionReference.getArguments().stream()
              .map(argument -> new ArgumentData(argument.getName(),
                      argument.getType().toPresentationString(),
                      argument.isOptional(),
                      argument.isRest(),
                      collectHaxeClassesAsSmartPointers(argument.getType())
              ))
              .toList();

    this.myPsiTargetPointer  = createPointer(targetClass);
  }

  private List<SmartPsiElementPointer<HaxeClass>> collectHaxeClassesAsSmartPointers(ResultHolder type) {
    return collectHaxeClasses(type).stream()
            .map( e-> Optional.ofNullable(e).map(this::createPointer).orElse(null))
            .toList();
  }

  private String getQname(ResultHolder type) {
    if(type.getClassType() != null) {
      HaxeClass haxeClass = type.getClassType().getHaxeClass();
      if(haxeClass != null) {
        return haxeClass.getQualifiedName();
      }
    }
    return null;
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
      findTypesRequiringImportsAndAddToFile(methodDeclaration, anchor.getContainingFile());
    }
    return anchor.getContainingFile();
  }

  private void findTypesRequiringImportsAndAddToFile(PsiElement methodDeclaration, PsiFile containingFile) {
    if (methodDeclaration instanceof HaxeMethodDeclaration declaration) {
      Set<String> qNamesToImport = new HashSet<>();
      List<HaxeParameterModel> parameters = declaration.getModel().getParameters();
      for (int i = 0; i < parameters.size(); i++) {
        HaxeParameterModel parameter = parameters.get(i);
        ResultHolder type = parameter.getType();
        ArgumentData argumentData = argumentInfo.get(i);
        List<SmartPsiElementPointer<HaxeClass>> typesInOriginal = new ArrayList<>(argumentData.types);
        List<HaxeClass> typesInGenerated = new ArrayList<>(PsiTreeUtil.collectElementsOfType(type.getContext(), HaxeClass.class));

        for (int j = 0; j < typesInGenerated.size(); j++) {
          SmartPsiElementPointer<HaxeClass> pointer = typesInOriginal.get(j);
          HaxeClass orgHaxeClass =  pointer == null ? null : pointer.getElement();
          HaxeClass newHaxeClass = typesInGenerated.get(j);
          if (newHaxeClass == null && orgHaxeClass != null) {
            qNamesToImport.add(orgHaxeClass.getQualifiedName());
          }
        }
      }
      for (String qNames : qNamesToImport) {
        HaxeAddImportHelper.addImport(qNames, containingFile);
      }
    }
  }


  private PsiElement generateDeclaration(@NotNull Project project) {
    String returnType = returnTypeText;
    String returnStatement = determineReturnStatement(returnType);
    String optionalStaticKeyword = needsToBeStatic() ? "static" : "";
    String privateKeyword = needsToBePublic() ? "public" : "private";
    String function = """
      %s %s function %s (%s):%s {
        %s
      }
      """
      .formatted(privateKeyword, optionalStaticKeyword, methodName, generateParameterList(), returnType, returnStatement);

    return HaxeElementGenerator.createMethodDeclaration(project, function);
  }

  private String determineReturnStatement(String type) {
    if (Objects.equals(type, SpecificHaxeClassReference.VOID)) return "";
    if (Objects.equals(type, SpecificHaxeClassReference.INT)) return " return 0;";
    if (Objects.equals(type, SpecificHaxeClassReference.FLOAT)) return " return 0.0;";
    if (Objects.equals(type, SpecificHaxeClassReference.BOOL)) return " return false;";
    return "return null;";
  }


  private String generateParameterList() {
    StringBuilder builder = new StringBuilder();
      for (int i = 0; i < argumentInfo.size(); i++) {
        ArgumentData argumentData = argumentInfo.get(i);
        String paramName = argumentData.name != null ? argumentData.name : "p" + i;
        String prefix = argumentData.rest ? "..." : (argumentData.optional ? ":" : "");
        String typeTag = ":" + argumentData.typeText;
        builder.append(prefix).append(paramName).append(typeTag);
        if (i + 1 != argumentInfo.size()) {
          builder.append(",");
        }
    }
    return builder.toString();
  }

}