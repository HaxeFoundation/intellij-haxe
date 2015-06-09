/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
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
package com.intellij.plugins.haxe.model.type;

import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.plugins.haxe.model.HaxeDocumentModel;
import com.intellij.psi.PsiElement;

import java.util.ArrayList;
import java.util.List;

public class HaxeExpressionEvaluatorContext {
  public SpecificTypeReference result;
  public List<SpecificTypeReference> returns = new ArrayList<SpecificTypeReference>();
  public AnnotationHolder holder;
  private HaxeScope<SpecificTypeReference> scope = new HaxeScope<SpecificTypeReference>();
  public PsiElement root;

  public HaxeDocumentModel getDocument() {
    return HaxeDocumentModel.fromElement(root);
  }

  public void beginScope() {
    scope = new HaxeScope<SpecificTypeReference>(scope);
  }

  public void endScope() {
    scope = scope.parent;
  }

  public void setLocal(String key, SpecificTypeReference value) {
    this.scope.set(key, value);
  }

  public void setLocalWhereDefined(String key, SpecificTypeReference value) {
    this.scope.setWhereDefined(key, value);
  }

  public boolean has(String key) {
    return this.scope.has(key);
  }

  public SpecificTypeReference get(String key) {
    return this.scope.get(key);
  }

  public Annotation addError(PsiElement element, String error, HaxeFixer... fixers) {
    if (holder != null) {
      Annotation annotation = holder.createErrorAnnotation(element, error);
      for (HaxeFixer fixer : fixers) {
        annotation.registerFix(fixer);
      }
      return annotation;
    }
    return null;
  }
}
