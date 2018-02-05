/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2017 Ilya Malanin
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
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMember;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.lang.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class HaxeClassModel implements HaxeExposableModel {
  public final HaxeClass haxeClass;

  public HaxeClassModel(@NotNull HaxeClass haxeClass) {
    this.haxeClass = haxeClass;
  }

  public HaxeClassModel getParentClass() {
    List<HaxeType> list = haxeClass.getHaxeExtendsList();
    if (!list.isEmpty()) {
      PsiElement haxeClass = list.get(0).getReferenceExpression().resolve();
      if (haxeClass != null && haxeClass instanceof HaxeClass) {
        return ((HaxeClass)haxeClass).getModel();
      }
    }
    return null;
  }

  static public boolean isValidClassName(String name) {
    return name.substring(0, 1).equals(name.substring(0, 1).toUpperCase());
  }

  public HaxeClassReference getReference() {
    return new HaxeClassReference(this, this.getPsi());
  }

  @NotNull
  public ResultHolder getInstanceType() {
    return SpecificHaxeClassReference.withoutGenerics(getReference()).createHolder();
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

  @Nullable
  public HaxeTypeOrAnonymous getAbstractUnderlyingType() {
    if (!isAbstract()) return null;
    HaxeAbstractClassDeclaration abstractDeclaration = (HaxeAbstractClassDeclaration)haxeClass;
    HaxeUnderlyingType underlyingType = abstractDeclaration.getUnderlyingType();
    if (underlyingType != null) {
      List<HaxeTypeOrAnonymous> list = underlyingType.getTypeOrAnonymousList();
      if (!list.isEmpty()) {
        return list.get(0);
      }

      // TODO: What about function types?
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
    HaxeClassModel parentClass = getParentClass();
    while (parentClass != null) {
      HaxeMethodModel constructorMethod = parentClass.getConstructor();
      if (constructorMethod != null) {
        return constructorMethod;
      }
      parentClass = parentClass.getParentClass();
    }
    return null;
  }

  public HaxeMemberModel getMember(String name) {
    if (name == null) return null;
    final HaxeMethodModel method = getMethod(name);
    final HaxeFieldModel field = getField(name);
    return (method != null) ? method : field;
  }

  public List<HaxeMemberModel> getMembers() {
    LinkedList<HaxeMemberModel> members = new LinkedList<>();
    members.addAll(getMethods());
    members.addAll(getFields());
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
    HaxePsiField field = (HaxePsiField)haxeClass.findHaxeFieldByName(name);
    if (field instanceof HaxeVarDeclaration || field instanceof HaxeAnonymousTypeField || field instanceof HaxeEnumValueDeclaration) {
      return new HaxeFieldModel(field);
    }
    return null;
  }

  public HaxeMethodModel getMethod(String name) {
    HaxeMethodPsiMixin method = (HaxeMethodPsiMixin)haxeClass.findHaxeMethodByName(name);
    return method != null ? method.getModel() : null;
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

  @NotNull
  public HaxeDocumentModel getDocument() {
    return new HaxeDocumentModel(haxeClass);
  }

  public String getName() {
    return haxeClass.getName();
  }

  @Override
  public PsiElement getBasePsi() {
    return this.haxeClass;
  }

  @Nullable
  @Override
  public HaxeExposableModel getExhibitor() {
    return HaxeFileModel.fromElement(haxeClass.getContainingFile());
  }

  @Nullable
  @Override
  public FullyQualifiedInfo getQualifiedInfo() {
    HaxeExposableModel exhibitor = getExhibitor();
    if (exhibitor != null) {
      FullyQualifiedInfo containerInfo = exhibitor.getQualifiedInfo();
      if (containerInfo != null) {
        return new FullyQualifiedInfo(containerInfo.packagePath, containerInfo.fileName, getName(), null);
      }
    }

    return null;
  }

  public void addMethodsFromPrototype(List<HaxeMethodModel> methods) {
    throw new NotImplementedException("Not implemented HaxeClassMethod.addMethodsFromPrototype() : check HaxeImplementMethodHandler");
  }

  public List<HaxeFieldModel> getFields() {
    HaxePsiCompositeElement body = PsiTreeUtil.getChildOfAnyType(haxeClass, isEnum() ? HaxeEnumBody.class : HaxeClassBody.class);

    if (body != null) {
      return PsiTreeUtil.getChildrenOfAnyType(body, HaxeVarDeclaration.class, HaxeAnonymousTypeField.class, HaxeEnumValueDeclaration.class)
        .stream()
        .map(HaxeFieldModel::new)
        .collect(Collectors.toList());
    } else {
      return Collections.emptyList();
    }
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

  @Override
  public List<HaxeModel> getExposedMembers() {
    // TODO ClassModel concept should be reviewed. We need to separate logic of abstracts, regular classes, enums, etc. Right now this class a bunch of if-else conditions. It looks dirty.
    ArrayList<HaxeModel> out = new ArrayList<>();
    if (isClass()) {
      HaxeClassBody body = UsefulPsiTreeUtil.getChild(haxeClass, HaxeClassBody.class);
      if (body != null) {
        for (HaxeNamedComponent declaration : PsiTreeUtil.getChildrenOfAnyType(body, HaxeVarDeclaration.class, HaxeMethod.class)) {
          if (!(declaration instanceof PsiMember)) continue;
          if (declaration instanceof HaxeVarDeclaration) {
            HaxeVarDeclaration varDeclaration = (HaxeVarDeclaration)declaration;
            if (varDeclaration.isPublic() && varDeclaration.isStatic()) {
              out.add(new HaxeFieldModel((HaxeVarDeclaration)declaration));
            }
          } else {
            HaxeMethod method = (HaxeMethod)declaration;
            if (method.isStatic() && method.isPublic()) {
              out.add(new HaxeMethodModel(method));
            }
          }
        }
      }
    } else if (isEnum()) {
      HaxeEnumBody body = UsefulPsiTreeUtil.getChild(haxeClass, HaxeEnumBody.class);
      if (body != null) {
        List<HaxeEnumValueDeclaration> declarations = body.getEnumValueDeclarationList();
        for (HaxeEnumValueDeclaration declaration : declarations) {
          out.add(new HaxeFieldModel(declaration));
        }
      }
    }
    return out;
  }

  public static HaxeClassModel fromElement(PsiElement element) {
    HaxeClass haxeClass = PsiTreeUtil.getParentOfType(element, HaxeClass.class);
    if (haxeClass != null) {
      return new HaxeClassModel(haxeClass);
    }

    return null;
  }

  public boolean isPublic() {
    return haxeClass.isPublic();
  }
}
