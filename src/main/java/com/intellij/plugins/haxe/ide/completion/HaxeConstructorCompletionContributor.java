/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2018 Ilya Malanin
 * Copyright 2018 Eric Bishton
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
package com.intellij.plugins.haxe.ide.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.extapi.psi.StubBasedPsiElementBase;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.ide.lookup.HaxeConstructorLookupElement;
import com.intellij.plugins.haxe.lang.psi.stubs.index.specialized.HaxeConstructorStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeClassStub;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeMethodStub;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static com.intellij.plugins.haxe.ide.completion.HaxeCommonCompletionPattern.*;

public class HaxeConstructorCompletionContributor extends CompletionContributor {
  public HaxeConstructorCompletionContributor() {
    extend(CompletionType.BASIC, identifierInNewExpression,
           new CompletionProvider<>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet result) {
               final PsiFile file = parameters.getOriginalFile();
               addVariantsFromIndex(result, file);
             }
           });
  }

  private static void addVariantsFromIndex(final CompletionResultSet resultSet, final PsiFile targetFile) {
    final Project project = targetFile.getProject();
    final GlobalSearchScope scope = HaxeResolveUtil.getScopeForElement(targetFile);
    final Collection<HaxeMethod> constructors = HaxeConstructorStubIndex.getConstructors(project, scope);

    //TODO mlo: add support for overloads
    for (HaxeMethod constructor : constructors) {
      final HaxeClass containingClass = PsiTreeUtil.getStubOrPsiParentOfType(constructor, HaxeClass.class);
      if (containingClass == null) continue;

      // Extract data from stubs to avoid AST load where possible
      boolean hasParameters = getHasParametersFromStub(constructor);
      String className = null;
      String packageName = "";
      com.intellij.plugins.haxe.HaxeComponentType componentType = null;

      if (containingClass instanceof StubBasedPsiElementBase<?> stubPsi) {
        StubElement<?> stub = stubPsi.getStub();
        if (stub instanceof HaxeClassStub classStub) {
          className = classStub.getName();
          componentType = classStub.getComponentType();
          String qname = classStub.getQualifiedName();
          if (qname != null && qname.contains(".")) {
            packageName = qname.substring(0, qname.lastIndexOf('.'));
          }
        }
      }
      if (className == null) className = containingClass.getName();
      if (className == null) continue;
      if (componentType == null) componentType = containingClass.getComponentType();

      resultSet.addElement(new HaxeConstructorLookupElement(
              constructor, className, packageName, hasParameters, componentType));
    }
  }

  private static boolean getHasParametersFromStub(@NotNull HaxeMethod method) {
    if (method instanceof StubBasedPsiElementBase<?> stubPsi) {
      StubElement<?> stub = stubPsi.getStub();
      if (stub instanceof HaxeMethodStub methodStub) {
        return methodStub.hasParameters();
      }
    }
    // AST fallback (PSI already loaded)
    return method.getParameterList().getParametersCount() > 0;
  }
}