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
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import org.apache.commons.lang.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HaxeClassModel {
  public HaxeClass haxeClass;

  public HaxeClassModel(HaxeClass haxeClass) {
    this.haxeClass = haxeClass;
  }

  public HaxeClassReferenceModel getParentClassReference() {
    List<HaxeType> list = haxeClass.getHaxeExtendsList();
    if (list.size() == 0) return null;
    return new HaxeClassReferenceModel(list.get(0));
  }

  static public boolean isValidClassName(String name) {
    return name.substring(0, 1).equals(name.substring(0, 1).toUpperCase());
  }

  public HaxeClassModel getParentClass() {
    final HaxeClassReferenceModel reference = this.getParentClassReference();
    return (reference != null) ? reference.getHaxeClass() : null;
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

  // @TODO: Create AbstractHaxeClassModel extending this class for these methods?
  // @TODO: this should be properly parsed in haxe.bnf so searching for the underlying type is not required
  @Nullable
  public HaxeTypeOrAnonymous getAbstractUnderlyingType() {
    if (!isAbstract()) return null;
    PsiElement[] children = getPsi().getChildren();
    if (children.length >= 2) {
      if (children[1] instanceof HaxeTypeOrAnonymous) {
        return (HaxeTypeOrAnonymous)children[1];
      }
    }
    return null;
  }

  // @TODO: this should be properly parsed in haxe.bnf so searching for to is not required
  public List<HaxeType> getAbstractToList() {
    if (!isAbstract()) return Collections.emptyList();
    List<HaxeType> types = new LinkedList<HaxeType>();
    for (HaxeIdentifier id : UsefulPsiTreeUtil.getChildren(haxeClass, HaxeIdentifier.class)) {
      if (id.getText().equals("to")) {
        PsiElement sibling = UsefulPsiTreeUtil.getNextSiblingNoSpaces(id);
        if (sibling instanceof HaxeType) {
          types.add((HaxeType)sibling);
        }
      }
    }
    return types;
  }

  // @TODO: this should be properly parsed in haxe.bnf so searching for from is not required
  public List<HaxeType> getAbstractFromList() {
    if (!isAbstract()) return Collections.emptyList();
    List<HaxeType> types = new LinkedList<HaxeType>();
    for (HaxeIdentifier id : UsefulPsiTreeUtil.getChildren(haxeClass, HaxeIdentifier.class)) {
      if (id.getText().equals("from")) {
        PsiElement sibling = UsefulPsiTreeUtil.getNextSiblingNoSpaces(id);
        if (sibling instanceof HaxeType) {
          types.add((HaxeType)sibling);
        }
      }
    }
    return types;
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

  public boolean hasConstructor() {
    return getConstructor() != null;
  }

  public HaxeMethodModel getParentConstructor() {
    HaxeClassReferenceModel parentClass = getParentClassReference();
    if (parentClass == null) return null;
    return parentClass.getHaxeClass().getMethod("new");
  }

  public HaxeMemberModel getMember(String name) {
    final HaxeMethodModel method = getMethod(name);
    final HaxeFieldModel field = getField(name);
    return (method != null) ? method : field;
  }

  public List<HaxeMemberModel> getMembers() {
    LinkedList<HaxeMemberModel> members = new LinkedList<HaxeMemberModel>();
    for (HaxeMethodModel method : getMethods()) members.add(method);
    for (HaxeFieldModel field : getFields()) members.add(field);
    return members;
  }

  @NotNull
  public List<HaxeMemberModel> getMembersSelf() {
    LinkedList<HaxeMemberModel> members = new LinkedList<HaxeMemberModel>();
    HaxeClassBody body = UsefulPsiTreeUtil.getChild(haxeClass, HaxeClassBody.class);
    if (body != null) {
      for (PsiElement element : body.getChildren()) {
        if (element instanceof HaxeMethod || element instanceof HaxeVarDeclaration) {
          HaxeMemberModel model = HaxeMemberModel.fromPsi(element);
          if (model != null) {
            members.add(model);
          }
        }
      }
    }
    return members;
  }

  public HaxeFieldModel getField(String name) {
    HaxeVarDeclaration name1 = (HaxeVarDeclaration)haxeClass.findHaxeFieldByName(name);
    return name1 != null ? new HaxeFieldModel(name1) : null;
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

  @NotNull
  public HaxeClass getPsi() {
    return haxeClass;
  }

  @Nullable
  public HaxeClassBody getBodyPsi() {
    return (haxeClass instanceof HaxeClassDeclaration) ? ((HaxeClassDeclaration)haxeClass).getClassBody() : null;
  }

  @Nullable
  public PsiIdentifier getNamePsi() {
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
    HaxeClassBody body = UsefulPsiTreeUtil.getChild(haxeClass, HaxeClassBody.class);
    LinkedList<HaxeFieldModel> out = new LinkedList<HaxeFieldModel>();
    if (body != null) {
      for (HaxeVarDeclaration declaration : UsefulPsiTreeUtil.getChildren(body, HaxeVarDeclaration.class)) {
        out.add(new HaxeFieldModel(declaration));
      }
    }
    return out;
  }

  public List<HaxeFieldModel> getFieldsSelf() {
    HaxeClassBody body = UsefulPsiTreeUtil.getChild(haxeClass, HaxeClassBody.class);
    LinkedList<HaxeFieldModel> out = new LinkedList<HaxeFieldModel>();
    if (body != null) {
      for (HaxeVarDeclaration declaration : UsefulPsiTreeUtil.getChildren(body, HaxeVarDeclaration.class)) {
        if (declaration.getContainingClass() == this.haxeClass) {
          out.add(new HaxeFieldModel(declaration));
        }
      }
    }
    return out;
  }

  public Set<HaxeClassModel> getCompatibleTypes() {
    final Set<HaxeClassModel> output = new LinkedHashSet<HaxeClassModel>();
    writeCompatibleTypes(output);
    return output;
  }

  public void writeCompatibleTypes(Set<HaxeClassModel> output) {
    // Own
    output.add(this);

    final HaxeClassModel parentClass = this.getParentClass();

    // Parent classes
    if (parentClass != null) {
      if (!output.contains(parentClass)) {
        parentClass.writeCompatibleTypes(output);
      }
    }

    // Interfaces
    for (HaxeClassReferenceModel model : this.getImplementingInterfaces()) {
      if (model == null) continue;
      final HaxeClassModel aInterface = model.getHaxeClass();
      if (aInterface == null) continue;
      if (!output.contains(aInterface)) {
        aInterface.writeCompatibleTypes(output);
      }
    }

    // @CHECK abstract FROM
    for (HaxeType type : getAbstractFromList()) {
      final ResultHolder aTypeRef = HaxeTypeResolver.getTypeFromType(type);
      SpecificHaxeClassReference classType = aTypeRef.getClassType();
      if (classType != null) {
        classType.getHaxeClassModel().writeCompatibleTypes(output);
      }
    }

    // @CHECK abstract TO
    for (HaxeType type : getAbstractToList()) {
      final ResultHolder aTypeRef = HaxeTypeResolver.getTypeFromType(type);
      SpecificHaxeClassReference classType = aTypeRef.getClassType();
      if (classType != null) {
        classType.getHaxeClassModel().writeCompatibleTypes(output);
      }
    }
  }

  public List<HaxeGenericParamModel> getGenericParams() {
    List<HaxeGenericParamModel> out = new LinkedList<HaxeGenericParamModel>();
    if (getPsi().getGenericParam() != null) {
      int index = 0;
      for (HaxeGenericListPart part : getPsi().getGenericParam().getGenericListPartList()) {
        out.add(new HaxeGenericParamModel(part, index));
        index++;
      }
    }
    return out;
  }

  public void addField(String name, SpecificTypeReference type) {
    this.getDocument().addTextAfterElement(getBodyPsi(), "\npublic var " + name + ":" + type.toStringWithoutConstant() + ";\n");
  }

  public void addMethod(String name) {
    this.getDocument().addTextAfterElement(getBodyPsi(), "\npublic function " + name + "() {\n}\n");
  }
}
