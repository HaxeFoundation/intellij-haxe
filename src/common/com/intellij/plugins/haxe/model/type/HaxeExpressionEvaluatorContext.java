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
import com.intellij.plugins.haxe.ide.highlight.HaxeSyntaxHighlighterColors;
import com.intellij.plugins.haxe.model.HaxeDocumentModel;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.psi.PsiElement;

import java.util.ArrayList;
import java.util.List;

public class HaxeExpressionEvaluatorContext {
  public SpecificTypeReferenceHolder result;
  private List<SpecificTypeReferenceHolder> returns = new ArrayList<SpecificTypeReferenceHolder>();
  private List<PsiElement> returnElements = new ArrayList<PsiElement>();
  private List<ReturnInfo> returnInfos = new ArrayList<ReturnInfo>();

  public AnnotationHolder holder;
  private HaxeScope<SpecificTypeReferenceHolder> scope = new HaxeScope<SpecificTypeReferenceHolder>();
  public PsiElement root;

  public void addReturnType(SpecificTypeReferenceHolder type, PsiElement element) {
    this.returns.add(type);
    this.returnElements.add(element);
    this.returnInfos.add(new ReturnInfo(element, type));
  }

  public SpecificTypeReference getReturnType() {
    if (returns.isEmpty()) return SpecificHaxeClassReference.getVoid(root);
    return HaxeTypeUnifier.unify(SpecificTypeReferenceHolder.types(returns));
  }

  public List<SpecificTypeReferenceHolder> getReturnValues() {
    return returns;
  }

  public List<ReturnInfo> getReturnInfos() {
    return returnInfos;
  }

  public List<PsiElement> getReturnElements() {
    return returnElements;
  }

  public HaxeDocumentModel getDocument() {
    return HaxeDocumentModel.fromElement(root);
  }

  public void beginScope() {
    scope = new HaxeScope<SpecificTypeReferenceHolder>(scope);
  }

  public void endScope() {
    scope = scope.parent;
  }

  public void setLocal(String key, SpecificTypeReferenceHolder value) {
    this.scope.set(key, value);
  }

  public void setLocalWhereDefined(String key, SpecificTypeReferenceHolder value) {
    this.scope.setWhereDefined(key, value);
  }

  public boolean has(String key) {
    return this.scope.has(key);
  }

  public SpecificTypeReferenceHolder get(String key) {
    return this.scope.get(key);
  }

  public Annotation addError(PsiElement element, String error, HaxeFixer... fixers) {
    if (holder == null) return null;
    Annotation annotation = holder.createErrorAnnotation(element, error);
    for (HaxeFixer fixer : fixers) {
      annotation.registerFix(fixer);
    }
    return annotation;
  }

  public Annotation addWarning(PsiElement element, String error, HaxeFixer... fixers) {
    if (holder == null) return null;
    Annotation annotation = holder.createWarningAnnotation(element, error);
    for (HaxeFixer fixer : fixers) {
      annotation.registerFix(fixer);
    }
    return annotation;
  }

  public Annotation addUnreachable(PsiElement element) {
    if (holder == null) return null;
    Annotation annotation = holder.createInfoAnnotation(element, null);
    annotation.setTextAttributes(HaxeSyntaxHighlighterColors.LINE_COMMENT);
    return annotation;
  }
}

class ReturnInfo {
  final public SpecificTypeReferenceHolder type;
  final public PsiElement element;

  public ReturnInfo(PsiElement element, SpecificTypeReferenceHolder type) {
    this.element = element;
    this.type = type;
  }
}