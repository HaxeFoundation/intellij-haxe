/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2018 Ilya Malanin
 * Copyright 2018-2020 Eric Bishton
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

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.psi.impl.HaxeMetadataTypeName;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
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

import static com.intellij.plugins.haxe.HaxeComponentType.*;

public class HaxeClassModel implements HaxeExposableModel {
  public final HaxeClass haxeClass;

  public HaxeClassModel(@NotNull HaxeClass haxeClass) {
    this.haxeClass = haxeClass;
  }

  public HaxeClassModel getParentClass() {
    // TODO: Anonymous structures can extend several structs.  Need to be able to find/check/use all of them.
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
    return !this.isAbstract() && (typeOf(haxeClass) == CLASS);
  }

  public boolean isInterface() {
    return typeOf(haxeClass) == INTERFACE;
  }

  public boolean isEnum() {
    return haxeClass.isEnum();
  }

  public boolean isTypedef() {
    return typeOf(haxeClass) == TYPEDEF;
  }

  public boolean isAbstract() {
    return haxeClass instanceof HaxeAbstractClassDeclaration;
  }

  public boolean isCoreType() {
    return hasCompileTimeMeta(HaxeMeta.CORE_TYPE);
  }

  public boolean hasCompileTimeMeta(@NotNull HaxeMetadataTypeName name) {
    return haxeClass.hasCompileTimeMeta(name);
  }

  @Nullable
  public HaxeClassModifierList getModifiers() {
    // TODO: This should really be returning a HaxeModifiersModel, and that class needs to be updated
    //       to use HaxeClassModifierLists.  Right now, it's using the PsiModifiers from the IntelliJ Java implementation.

    if (haxeClass instanceof HaxeClassDeclaration) {
      HaxeClassDeclaration clazz = (HaxeClassDeclaration)haxeClass;
      return clazz.getClassModifierList();
    }
    if (haxeClass instanceof HaxeExternClassDeclaration) {
      HaxeExternClassDeclaration clazz = (HaxeExternClassDeclaration)haxeClass;
      return clazz.getExternClassModifierList();
    }
    return null;
  }

  @Nullable
  public HaxeTypeOrAnonymous getUnderlyingType() {
    if (isAbstract()) {
      HaxeAbstractClassDeclaration abstractDeclaration = (HaxeAbstractClassDeclaration)haxeClass;
      HaxeUnderlyingType underlyingType = abstractDeclaration.getUnderlyingType();
      if (underlyingType != null) {
        return underlyingType.getTypeOrAnonymous();
      }
    } else if(isTypedef()) {
      HaxeTypedefDeclaration typedef = (HaxeTypedefDeclaration)haxeClass;
      return typedef.getTypeOrAnonymous();
    }

    // TODO: What about function types?

    return null;
  }

  @Nullable
  public SpecificHaxeClassReference getUnderlyingClassReference(HaxeGenericResolver resolver) {
    if (!isAbstract()) return null;

    PsiElement element = getBasePsi();
    HaxeTypeOrAnonymous typeOrAnon = getUnderlyingType();
    if (typeOrAnon != null) {
      HaxeType type = typeOrAnon.getType();
      if (type != null) {
        HaxeClass aClass = HaxeResolveUtil.tryResolveClassByQName(type);
        if (aClass != null) {
          ResultHolder[] specifics =  HaxeTypeResolver.resolveDeclarationParametersToTypes(aClass, resolver);
          return SpecificHaxeClassReference.withGenerics(new HaxeClassReference(aClass.getModel(), element), specifics, element);
        }
      } else { // Anonymous type
        HaxeAnonymousType anon = typeOrAnon.getAnonymousType();
        if (anon != null) {
          // Anonymous types don't have parameters of their own, but when they are part of a typedef, they use the parameters from it.
          return SpecificHaxeClassReference.withGenerics(new HaxeClassReference(anon.getModel(), element), resolver.getSpecifics(), element);
        }
      }
    } else {
      // No typeOrAnon.  This must be Null<T>?
      if ("Null".equals(getName())) {
        List<HaxeGenericParamModel> typeParams = getGenericParams();
        if (typeParams.size() == 1) {
          HaxeGenericParamModel param = typeParams.get(0);
          ResultHolder result = resolver.resolve(param.getName());
          return result.getClassType();
        }
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
        PsiElement sibling = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(id);
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
        PsiElement sibling = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(id);
        if (sibling instanceof HaxeType) {
          types.add((HaxeType)sibling);
        }
      }
    }
    return types;
  }

  public boolean hasMethod(String name, @Nullable HaxeGenericResolver resolver) {
    return getMethod(name, resolver) != null;
  }

  public boolean hasMethodSelf(String name) {
    HaxeMethodModel method = getMethod(name, null);
    if (method == null) return false;
    return (method.getDeclaringClass() == this);
  }

  public HaxeMethodModel getMethodSelf(String name) {
    HaxeMethodModel method = getMethod(name, null);
    if (method == null) return null;
    return (method.getDeclaringClass() == this) ? method : null;
  }

  public HaxeMethodModel getConstructorSelf() {
    return getMethodSelf("new");
  }

  public HaxeMethodModel getConstructor(@Nullable HaxeGenericResolver resolver) {
    return getMethod("new", resolver);
  }

  public boolean hasConstructor(@Nullable HaxeGenericResolver resolver) {
    return getConstructor(resolver) != null;
  }

  public HaxeMethodModel getParentConstructor(@Nullable HaxeGenericResolver resolver) {
    HaxeClassModel parentClass = getParentClass();
    while (parentClass != null) {
      HaxeMethodModel constructorMethod = parentClass.getConstructor(resolver);
      if (constructorMethod != null) {
        return constructorMethod;
      }
      parentClass = parentClass.getParentClass();
    }
    return null;
  }

  public HaxeMemberModel getMember(String name, @Nullable HaxeGenericResolver resolver) {
    if (name == null) return null;
    HaxeNamedComponent component = haxeClass.findHaxeMemberByName(name, resolver);
    if (component != null) {
      return HaxeMemberModel.fromPsi(component);
    }
    return null;
  }

  public List<HaxeMemberModel> getMembers(@Nullable HaxeGenericResolver resolver) {
    final List<HaxeMemberModel> members = new ArrayList<>();
    members.addAll(getMethods(resolver));
    members.addAll(getFields());
    return members;
  }

  @NotNull
  public List<HaxeMemberModel> getMembersSelf() {
    final List<HaxeMemberModel> members = new ArrayList<>();
    HaxePsiCompositeElement body = getBodyPsi();
    if (body != null) {
      for (PsiElement element : body.getChildren()) {
        if (element instanceof HaxeMethod || element instanceof HaxeFieldDeclaration) {
          HaxeMemberModel model = HaxeMemberModel.fromPsi(element);
          if (model != null) {
            members.add(model);
          }
        }
      }
    }
    return members;
  }

  public HaxeFieldModel getField(String name, @Nullable HaxeGenericResolver resolver) {
    HaxePsiField field = (HaxePsiField)haxeClass.findHaxeFieldByName(name, resolver);
    if (field instanceof HaxeFieldDeclaration || field instanceof HaxeAnonymousTypeField || field instanceof HaxeEnumValueDeclaration) {
      return new HaxeFieldModel(field);
    }
    return null;
  }

  public HaxeMethodModel getMethod(String name, @Nullable HaxeGenericResolver resolver) {
    HaxeMethodPsiMixin method = (HaxeMethodPsiMixin)haxeClass.findHaxeMethodByName(name, resolver);
    return method != null ? method.getModel() : null;
  }

  public List<HaxeMethodModel> getMethods(@Nullable HaxeGenericResolver resolver) {
    List<HaxeMethodModel> models = new ArrayList<HaxeMethodModel>();
    for (HaxeMethod method : haxeClass.getHaxeMethods(resolver)) {
      models.add(method.getModel());
    }
    return models;
  }

  public List<HaxeMethodModel> getMethodsSelf(@Nullable HaxeGenericResolver resolver) {
    List<HaxeMethodModel> models = new ArrayList<HaxeMethodModel>();
    for (HaxeMethod method : haxeClass.getHaxeMethods(resolver)) {
      if (method.getContainingClass() == this.haxeClass) models.add(method.getModel());
    }
    return models;
  }

  public List<HaxeMethodModel> getAncestorMethods(@Nullable HaxeGenericResolver resolver) {
    List<HaxeMethodModel> models = new ArrayList<HaxeMethodModel>();
    for (HaxeMethod method : haxeClass.getHaxeMethods(resolver)) {
      if (method.getContainingClass() != this.haxeClass) models.add(method.getModel());
    }
    return models;
  }

  @NotNull
  public HaxeClass getPsi() {
    return haxeClass;
  }

  @Nullable
  public HaxePsiCompositeElement getBodyPsi() {
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
    // TODO: Figure out if this needs to deal with forwarded fields in abstracts.
    HaxePsiCompositeElement body = PsiTreeUtil.getChildOfAnyType(haxeClass, isEnum() ? HaxeEnumBody.class : HaxeClassBody.class);

    if (body != null) {
      return PsiTreeUtil.getChildrenOfAnyType(body, HaxeFieldDeclaration.class, HaxeAnonymousTypeField.class, HaxeEnumValueDeclaration.class)
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
        HaxeClassModel model = classType.getHaxeClassModel();
        if (model != null) {
          model.writeCompatibleTypes(output);
        }
      }
    }

    // @CHECK abstract TO
    for (HaxeType type : getAbstractToList()) {
      final ResultHolder aTypeRef = HaxeTypeResolver.getTypeFromType(type);
      SpecificHaxeClassReference classType = aTypeRef.getClassType();
      if (classType != null) {
        HaxeClassModel model = classType.getHaxeClassModel();
        if (model != null) {
          model.writeCompatibleTypes(output);
        }
      }
    }

    // TODO: Add types from @:from and @:to methods, including inferred method types.
  }

  public boolean hasGenericParams() {
    return getPsi().getGenericParam() != null;
  }

  @NotNull
  public List<HaxeGenericParamModel> getGenericParams() {
    final List<HaxeGenericParamModel> out = new ArrayList<>();
    if (getPsi().getGenericParam() != null) {
      int index = 0;
      for (HaxeGenericListPart part : getPsi().getGenericParam().getGenericListPartList()) {
        out.add(new HaxeGenericParamModel(part, index));
        index++;
      }
    }
    return out;
  }

  /**
   * @return a generic resolver with Unknown or constrained types.
   */
  @NotNull
  public HaxeGenericResolver getGenericResolver(@Nullable HaxeGenericResolver parentResolver) {
    if (getPsi().getGenericParam() != null) {
      HaxeClassResolveResult result = HaxeClassResolveResult.create(getPsi(),
                                      parentResolver == null ? HaxeGenericSpecialization.EMPTY
                                                             : HaxeGenericSpecialization.fromGenericResolver(null, parentResolver));
      HaxeGenericResolver resolver = result.getSpecialization().toGenericResolver(getPsi());
      return resolver;
    }
    return new HaxeGenericResolver();
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
        for (HaxeNamedComponent declaration : PsiTreeUtil.getChildrenOfAnyType(body, HaxeFieldDeclaration.class, HaxeMethod.class)) {
          if (!(declaration instanceof PsiMember)) continue;
          if (declaration instanceof HaxeFieldDeclaration) {
            HaxeFieldDeclaration varDeclaration = (HaxeFieldDeclaration)declaration;
            if (varDeclaration.isPublic() && varDeclaration.isStatic()) {
              out.add(new HaxeFieldModel((HaxeFieldDeclaration)declaration));
            }
          } else {
            HaxeMethodDeclaration method = (HaxeMethodDeclaration)declaration;
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
    if (element == null) return null;

    HaxeClass haxeClass = element instanceof HaxeClass
                          ? (HaxeClass) element
                          : PsiTreeUtil.getParentOfType(element, HaxeClass.class);

    if (haxeClass != null) {
      return new HaxeClassModel(haxeClass);
    }
    return null;
  }

  public boolean isPublic() {
    return haxeClass.isPublic();
  }
}
