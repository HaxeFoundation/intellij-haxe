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
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.ide.highlight.HaxeSyntaxHighlighterColors;
import com.intellij.plugins.haxe.model.HaxeDocumentModel;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class HaxeExpressionEvaluatorContext {
  public ResultHolder result;
  private List<ResultHolder> returns = new ArrayList<ResultHolder>();
  private List<PsiElement> returnElements = new ArrayList<PsiElement>();
  private List<ReturnInfo> returnInfos = new ArrayList<ReturnInfo>();

  public AnnotationHolder holder;
  private HaxeScope<ResultHolder> scope = new HaxeScope<ResultHolder>();
  public PsiElement root;

  public HaxeExpressionEvaluatorContext(@NotNull PsiElement body, @Nullable AnnotationHolder holder) {
    this.root = body;
    this.holder = holder;
  }

  public HaxeExpressionEvaluatorContext createChild(PsiElement body) {
    HaxeExpressionEvaluatorContext that = new HaxeExpressionEvaluatorContext(body, this.holder);
    that.scope = this.scope;
    that.beginScope();
    return that;
  }

  public void addReturnType(ResultHolder type, PsiElement element) {
    this.returns.add(type);
    this.returnElements.add(element);
    this.returnInfos.add(new ReturnInfo(element, type));
  }

  public ResultHolder getReturnType() {
    if (returns.isEmpty()) return SpecificHaxeClassReference.getVoid(root).createHolder();
    return HaxeTypeUnifier.unify(ResultHolder.types(returns), root).createHolder();
  }

  public List<ResultHolder> getReturnValues() {
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
    scope = new HaxeScope<ResultHolder>(scope);
  }

  public void endScope() {
    scope = scope.parent;
  }

  public void setLocal(String key, ResultHolder value) {
    this.scope.set(key, value);
  }

  public void setLocalWhereDefined(String key, ResultHolder value) {
    this.scope.setWhereDefined(key, value);
  }

  public boolean has(String key) {
    return this.scope.has(key);
  }

  public ResultHolder get(String key) {
    return this.scope.get(key);
  }

  @NotNull
  public Annotation addError(PsiElement element, String error, HaxeFixer... fixers) {
    if (holder == null) return createDummyAnnotation();
    Annotation annotation = holder.createErrorAnnotation(element, error);
    for (HaxeFixer fixer : fixers) {
      annotation.registerFix(fixer);
    }
    return annotation;
  }

  @NotNull
  public Annotation addWarning(PsiElement element, String error, HaxeFixer... fixers) {
    if (holder == null) return createDummyAnnotation();
    Annotation annotation = holder.createWarningAnnotation(element, error);
    for (HaxeFixer fixer : fixers) {
      annotation.registerFix(fixer);
    }
    return annotation;
  }

  private Annotation createDummyAnnotation() {
    return new Annotation(0, 0, HighlightSeverity.ERROR, "", "");
  }

  @NotNull
  public Annotation addUnreachable(PsiElement element) {
    if (holder == null) return createDummyAnnotation();
    Annotation annotation = holder.createInfoAnnotation(element, null);
    annotation.setTextAttributes(HaxeSyntaxHighlighterColors.LINE_COMMENT);
    return annotation;
  }

  final public List<HaxeExpressionEvaluatorContext> lambdas = new LinkedList<HaxeExpressionEvaluatorContext>();
  public void addLambda(HaxeExpressionEvaluatorContext child) {
    lambdas.add(child);
  }
}

class ReturnInfo {
  final public ResultHolder type;
  final public PsiElement element;

  public ReturnInfo(PsiElement element, ResultHolder type) {
    this.element = element;
    this.type = type;
  }
}