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
package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxePsiUtils;
import com.intellij.psi.PsiElement;
import org.apache.commons.lang.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class HaxeClassModel {
  public HaxeClass haxeClass;

  public HaxeClassModel(HaxeClass haxeClass) {
    this.haxeClass = haxeClass;
  }

  public HaxeClassReferenceModel getParentClass() {
    List<HaxeType> list = haxeClass.getHaxeExtendsList();
    if (list.size() == 0) return null;
    return new HaxeClassReferenceModel(list.get(0));
  }

  public List<HaxeClassReferenceModel> getInterfaceExtendingInterfaces() {
    List<HaxeType> list = haxeClass.getHaxeExtendsList();
    List<HaxeClassReferenceModel> out = new ArrayList<HaxeClassReferenceModel>();
    for (HaxeType type : list) {
      out.add(new HaxeClassReferenceModel(type));
    }
    return out;
  }

  public List<HaxeClassReferenceModel> getImplementingInterfaces() {
    List<HaxeType> list = haxeClass.getHaxeImplementsList();
    List<HaxeClassReferenceModel> out = new ArrayList<HaxeClassReferenceModel>();
    for (HaxeType type : list) {
      out.add(new HaxeClassReferenceModel(type));
    }
    return out;
  }

  public boolean isExtern() {
    return haxeClass.isExtern();
  }

  public boolean isClass() {
    return !this.isAbstract() && (HaxeComponentType.typeOf(haxeClass) == HaxeComponentType.CLASS);
  }

  public boolean isInterface() {
    return HaxeComponentType.typeOf(haxeClass) == HaxeComponentType.INTERFACE;
  }

  public boolean isEnum() {
    return HaxeComponentType.typeOf(haxeClass) == HaxeComponentType.ENUM;
  }

  public boolean isTypedef() {
    return HaxeComponentType.typeOf(haxeClass) == HaxeComponentType.TYPEDEF;
  }

  public boolean isAbstract() {
    return haxeClass instanceof HaxeAbstractClassDeclaration;
  }

  public boolean hasMethod(String name) {
    return getMethod(name) != null;
  }

  public boolean hasMethodSelf(String name) {
    HaxeMethodModel method = getMethod(name);
    if (method == null) return false;
    return (method.getDeclaringClass() == this);
  }

  public HaxeMethodModel getMethodSelf(String name) {
    HaxeMethodModel method = getMethod(name);
    if (method == null) return null;
    return (method.getDeclaringClass() == this) ? method : null;
  }

  public HaxeMethodModel getConstructorSelf() {
    return getMethodSelf("new");
  }

  public HaxeMethodModel getConstructor() {
    return getMethod("new");
  }

  public HaxeMethodModel getParentConstructor() {
    HaxeClassReferenceModel parentClass = getParentClass();
    if (parentClass == null) return null;
    return parentClass.getHaxeClass().getMethod("new");
  }

  public HaxeMethodModel getMethod(String name) {
    HaxeMethodPsiMixin name1 = (HaxeMethodPsiMixin)haxeClass.findHaxeMethodByName(name);
    return name1 != null ? name1.getModel() : null;
  }

  public List<HaxeMethodModel> getMethods() {
    List<HaxeMethodModel> models = new ArrayList<HaxeMethodModel>();
    for (HaxeMethod method : haxeClass.getHaxeMethods()) {
      models.add(method.getModel());
    }
    return models;
  }

  public List<HaxeMethodModel> getMethodsSelf() {
    List<HaxeMethodModel> models = new ArrayList<HaxeMethodModel>();
    for (HaxeMethod method : haxeClass.getHaxeMethods()) {
      if (method.getContainingClass() == this.haxeClass) models.add(method.getModel());
    }
    return models;
  }

  public List<HaxeMethodModel> getAncestorMethods() {
    List<HaxeMethodModel> models = new ArrayList<HaxeMethodModel>();
    for (HaxeMethod method : haxeClass.getHaxeMethods()) {
      if (method.getContainingClass() != this.haxeClass) models.add(method.getModel());
    }
    return models;
  }

  public HaxeClass getPsi() {
    return haxeClass;
  }

  public PsiElement getNamePsi() {
    return haxeClass.getNameIdentifier();
  }

  private HaxeDocumentModel _document = null;
  @NotNull
  public HaxeDocumentModel getDocument() {
    if (_document == null) _document = new HaxeDocumentModel(haxeClass);
    return _document;
  }

  public String getName() {
    return haxeClass.getName();
  }

  public void addMethodsFromPrototype(List<HaxeMethodModel> methods) {
    throw new NotImplementedException("Not implemented HaxeClassMethod.addMethodsFromPrototype() : check HaxeImplementMethodHandler");
  }

  public List<HaxeFieldModel> getFields() {
    HaxeClassBody body = HaxePsiUtils.getChild(haxeClass, HaxeClassBody.class);
    LinkedList<HaxeFieldModel> out = new LinkedList<HaxeFieldModel>();
    if (body != null) {
      for (HaxeVarDeclaration declaration : HaxePsiUtils.getChilds(body, HaxeVarDeclaration.class)) {
        out.add(new HaxeFieldModel(declaration));
      }
    }
    return out;
  }
}
