/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2019 Eric Bishton
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
package com.intellij.plugins.haxe.ide.refactoring.introduceField;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.ide.refactoring.introduce.HaxeIntroduceHandler;
import com.intellij.plugins.haxe.ide.refactoring.introduce.HaxeIntroduceOperation;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeClassBody;
import com.intellij.plugins.haxe.lang.psi.HaxeExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeFieldDeclaration;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.plugins.haxe.util.HaxeNameSuggesterUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Created by as3boyan on 12.09.14.
 */
public class HaxeIntroduceConstantHandler extends HaxeIntroduceHandler {

  public HaxeIntroduceConstantHandler() {
    super("Introduce Constant");
  }

  @Override
  protected PsiElement addDeclaration(@NotNull final PsiElement expression,
                                      @NotNull final PsiElement declaration,
                                      @NotNull HaxeIntroduceOperation operation) {
    return doIntroduceVariable(expression, declaration, operation.getOccurrences(), operation.isReplaceAll());
  }

  public static PsiElement doIntroduceVariable(PsiElement expression,
                                               PsiElement declaration,
                                               List<PsiElement> occurrences,
                                               boolean replaceAll) {
    HaxeClass haxeClass = PsiTreeUtil.getParentOfType(expression, HaxeClass.class, false);
    if (haxeClass == null) return null;

    HaxeClassBody classBody = PsiTreeUtil.getChildOfType(haxeClass, HaxeClassBody.class);
    if (classBody == null) return null;

    PsiElement child = lastFieldDeclarationInList(classBody);

    if (child == null) {
      child = classBody.getFirstChild();
    }

    return child != null ? classBody.addAfter(declaration, child) : null;
  }

  @Nullable
  private static HaxeFieldDeclaration lastFieldDeclarationInList(HaxeClassBody classBody) {
    List<HaxeFieldDeclaration> list = classBody.getFieldDeclarationList();
    return list.isEmpty() ? null : list.get(list.size() - 1);
  }

  @Nullable
  @Override
  public PsiElement createDeclaration(HaxeIntroduceOperation operation) {
    final Project project = operation.getProject();
    final HaxeExpression initializer = operation.getInitializer();
    InitializerTextBuilder builder = new InitializerTextBuilder();
    initializer.accept(builder);
    String assignmentText = "public static inline var " + operation.getName() + " = " + builder.result() + ";";
    PsiElement anchor = operation.isReplaceAll()
                        ? findAnchor(operation.getOccurrences())
                        : findAnchor(initializer);
    return createDeclaration(project, assignmentText, anchor);
  }

  private static class InitializerTextBuilder extends PsiRecursiveElementVisitor {
    private final StringBuilder myResult = new StringBuilder();

    @Override
    public void visitWhiteSpace(PsiWhiteSpace space) {
      myResult.append(space.getText().replace('\n', ' '));
    }

    @Override
    public void visitElement(PsiElement element) {
      if (element.getChildren().length == 0) {
        myResult.append(element.getText());
      }
      else {
        super.visitElement(element);
      }
    }

    public String result() {
      return myResult.toString();
    }
  }

  @Nullable
  @Override
  protected HaxeFieldDeclaration createDeclaration(Project project, String text, PsiElement anchor) {
    return HaxeElementGenerator.createVarDeclaration(project, text);
  }

  @Override
  protected Collection<String> getSuggestedNames(HaxeExpression expression) {
    return HaxeNameSuggesterUtil.getSuggestedNames(expression, true);
  }
}
