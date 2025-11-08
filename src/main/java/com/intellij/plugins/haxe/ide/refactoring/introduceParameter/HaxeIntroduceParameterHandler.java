/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.ide.refactoring.introduceParameter;

import com.intellij.codeInsight.CodeInsightUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.refactoring.introduceVariable.HaxeIntroduceHandler;
import com.intellij.plugins.haxe.ide.refactoring.introduceVariable.HaxeIntroduceOperation;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiParserFacade;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.intellij.plugins.haxe.util.HaxeElementGenerator.createSemi;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeIntroduceParameterHandler extends HaxeIntroduceHandler {
  public HaxeIntroduceParameterHandler() {
    super(HaxeBundle.message("refactoring.introduce.parameter.dialog.title"));
  }

  protected   @NotNull String getActionName() {
    return "Introduce Parameter";
  }

  @Override
  protected boolean isValidIntroduceContext(PsiElement element) {
    if( isLiteral(element)) {
      if (PsiTreeUtil.getParentOfType(element, HaxeMethodDeclaration.class)!= null) {
        return true;
      }
    }else if (isVarDeclaration(element)) {
        return true;
    }else {
      // check for block parent, we need a block to put assign statement in
        return PsiTreeUtil.getParentOfType(element, HaxeBlockStatement.class) != null;
    }
    return false;
  }

  private boolean isVarDeclaration(PsiElement element) {
    HaxeLocalVarDeclarationList varDeclarations = PsiTreeUtil.getParentOfType(element, HaxeLocalVarDeclarationList.class);
    if(varDeclarations != null && varDeclarations.getLocalVarDeclarationList().size() == 1) {
      HaxeLocalVarDeclaration varDeclaration = varDeclarations.getLocalVarDeclarationList().get(0);
      if (varDeclaration != null && element instanceof HaxeComponentName varName) {
        return varDeclaration.getComponentName() == varName;
      }
    }
    return false;
  }

  @Override
  protected void performActionOnElementOccurrences(HaxeIntroduceOperation operation) {
    if(operation.getInitializer() instanceof  HaxeComponentName componentName) {
      operation.setName(componentName.getIdentifier().getText());
    }
    super.performActionOnElementOccurrences(operation);
  }

  @Nullable
  public PsiElement createDeclaration(HaxeIntroduceOperation operation) {
    PsiElement initializer = operation.getInitializer();


    ResultHolder result = findType(initializer);

    String typeTag = !result.isUnknown() ? ":" + result.toStringWithoutConstant() : "";
    String constant = getConstantString(initializer);
    String assignmentText =  operation.getName() + typeTag + constant;

    PsiElement anchor = operation.isReplaceAll()
                        ? findAnchor(operation.getOccurrences())
                        : findAnchor(initializer);

    return createDeclaration(operation.getProject(), assignmentText, anchor);
  }

  private ResultHolder findType(PsiElement initializer) {
    if(isVarDeclaration(initializer)) {
      HaxeLocalVarDeclaration varDeclaration = PsiTreeUtil.getParentOfType(initializer, HaxeLocalVarDeclaration.class);
      if(varDeclaration != null) return HaxeTypeResolver.getPsiElementType(varDeclaration, null);
    }
    return HaxeExpressionEvaluator.evaluate(initializer, null).result;
  }

  private @NotNull String getConstantString(PsiElement expression) {
     if (isLiteral(expression)) {
      return " = " + expression.getText();
    }
     if (isVarDeclaration(expression)) {
       HaxeLocalVarDeclaration declaration = PsiTreeUtil.getParentOfType(expression, HaxeLocalVarDeclaration.class);
       if (declaration != null && declaration.getVarInit() != null) {
         HaxeExpression init = declaration.getVarInit().getExpression();
         if(isLiteral(init)) {
           return " = " + init.getText();
         }
       }
     }
    return  "";
  }

  private boolean isLiteral(PsiElement element) {
    return element instanceof HaxeLiteralExpression || element instanceof HaxeStringLiteralExpression;
  }

  @Nullable
  protected PsiElement createDeclaration(Project project, String text, PsiElement anchor) {
    return HaxeElementGenerator.createParameter(project, text);
  }

  protected void modifyDeclaration(@NotNull PsiElement declaration, HaxeIntroduceOperation operation) {
    PsiElement expression = operation.getInitializer();
    if (declaration instanceof  HaxeParameter parameter) {
      if (isLiteral(expression)) {
        return; // handled as parameter default value
      }

      if (isVarDeclaration(expression)) {
        replaceVarDeclaration(parameter, expression);
        return;
      }

      insertParameterAssignStatement(parameter, expression);
    }
  }

  private void replaceVarDeclaration(HaxeParameter parameter, PsiElement expression) {
    HaxeLocalVarDeclarationList declarations = PsiTreeUtil.getParentOfType(expression, HaxeLocalVarDeclarationList.class);
    if(declarations.getLocalVarDeclarationList().size() == 1) {
      HaxeLocalVarDeclaration declaration = declarations.getLocalVarDeclarationList().get(0);
      if (declaration.getVarInit() != null) {
        HaxeExpression init = declaration.getVarInit().getExpression();
        if (isLiteral(init)) {
          declarations.delete();
        } else {
          PsiElement replaced = declarations.replace(declaration);
          replaced.add(createSemi(declaration.getProject()));
        }
      }
      else {
        declarations.delete();
      }
    }
  }

  private static void insertParameterAssignStatement(@NotNull HaxeParameter parameter, PsiElement expression) {
    Project project = expression.getProject();

    String paramName = parameter.getComponentName().getText();
    String assignExpression = paramName + " = " + expression.getText()+";";
    PsiElement assign = HaxeElementGenerator.createStatementFromText(project, assignExpression);
    HaxeBlockStatement blockStatement = PsiTreeUtil.getParentOfType(expression, HaxeBlockStatement.class);
    PsiElement anchor = blockStatement.addBefore(assign, findInsertBeforeElement(expression, blockStatement));

    final PsiElement newLineNode = PsiParserFacade.getInstance(parameter.getProject()).createWhiteSpaceFromText("\n");
    blockStatement.addAfter(newLineNode, anchor);
    blockStatement.addAfter(HaxeElementGenerator.createSemi(project), anchor);

    CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(expression.getContainingFile());
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


  @Override
  protected PsiElement addDeclaration(@NotNull final PsiElement expression,
                                      @NotNull final PsiElement declaration,
                                      @NotNull HaxeIntroduceOperation operation) {
    return doIntroduceParameter(expression, declaration, operation.getOccurrences(), operation.isReplaceAll());
  }

  public static PsiElement doIntroduceParameter(PsiElement expression,
                                                PsiElement declaration,
                                                List<PsiElement> occurrences,
                                                boolean replaceAll) {
    PsiElement anchor = replaceAll ? findAnchor(occurrences) : findAnchor(expression);
    assert anchor != null;
    HaxeMethodDeclaration method = PsiTreeUtil.getParentOfType(expression, HaxeMethodDeclaration.class);


    if (method != null) {
      HaxeParameterList parameterList = method.getParameterList();
      List<HaxeParameter> list = parameterList.getParameterList();
      if (list.isEmpty()) {
        return parameterList.add(declaration);
      }else {
        PsiUtilCore.ensureValid(parameterList);
        PsiElement seperator = HaxeElementGenerator.createComma(expression.getProject());
        parameterList.add(seperator);
        return parameterList.add(declaration);
      }

    }
    return  null;
  }
}
