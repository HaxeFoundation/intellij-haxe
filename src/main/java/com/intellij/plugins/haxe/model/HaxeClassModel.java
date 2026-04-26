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

import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxePsiClass;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeObjectLiteralImpl;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeClassStub;
import com.intellij.plugins.haxe.metadata.HaxeMetadataList;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataCompileTimeMeta;
import com.intellij.plugins.haxe.metadata.psi.impl.HaxeMetadataTypeName;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.*;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.intellij.plugins.haxe.HaxeComponentType.*;
import static com.intellij.plugins.haxe.model.type.HaxeTypeResolver.getTypeFromFunctionType;

public class HaxeClassModel implements HaxeCommonMembersModel {
  public final HaxeClass haxeClass;

  private HaxeModifiersModel _modifiers;

  private SpecificHaxeClassReference reference;

  public HaxeClassModel(@NotNull HaxeClass haxeClass) {
    this.haxeClass = haxeClass;
  }

  public HaxeClassModel getParentClass() {
    // TODO: Anonymous structures can extend several structs.  Need to be able to find/check/use all of them.
    List<HaxeType> list = getExtendsList();
    if (!list.isEmpty()) {
      PsiElement haxeClass = list.getFirst().getReferenceExpression().resolve();
      if (haxeClass instanceof HaxeClass parentClass) {
        return parentClass.getModel();
      }
    }
    return null;
  }

  static public boolean isValidClassName(String name) {
    return name.substring(0, 1).equals(name.substring(0, 1).toUpperCase());
  }

  @NotNull
  public HaxeClassReference getReference() {
    return new HaxeClassReference(this, this.getPsi());
  }
  @NotNull
  public HaxeClassReference createReference(PsiElement context) {
    return new HaxeClassReference(this, context);
  }

  @NotNull
  public ResultHolder getInstanceType() {
    return getInstanceReference().createHolder();
  }
  public SpecificHaxeClassReference getInstanceReference() {
    if (!isInstanceReferenceValid()) {
        reference = SpecificHaxeClassReference.withGenerics(getReference(),getSpecifics());
    }
    return reference;
  }
  @NotNull
  public SpecificHaxeClassReference createSpecificReference(PsiElement context) {
        return SpecificHaxeClassReference.withGenerics(createReference(context),getSpecifics());
  }

  private boolean isInstanceReferenceValid() {
    if (reference == null) return false;
    HaxeClass haxeClass1 = reference.getHaxeClass();
    if (haxeClass1 != null) {
      if (!reference.getHaxeClass().isValid()) return false;
      for (@NotNull ResultHolder specific : reference.getSpecifics()) {
        if (!specific.getType().context.isValid()) {
          return false;
        }
      }
    }
    return true;
  }

  @NotNull
  public List<HaxeClassReferenceModel> getExtendingTypes() {
    List<HaxeType> list = getExtendsList();
    List<HaxeClassReferenceModel> out = new ArrayList<HaxeClassReferenceModel>();
    for (HaxeType type : list) {
      out.add(new HaxeClassReferenceModel(type));
    }
    return out;
  }

  @NotNull
  public List<HaxeClassReferenceModel> getImplementingInterfaces() {
    List<HaxeType> list = getImplementsList();
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
    return !this.isAbstractType() && (haxeClass.getComponentType() == CLASS);
  }

  public boolean isInterface() {
    return haxeClass.getComponentType() == INTERFACE;
  }

  public boolean isEnum() {
    return haxeClass.isEnum();
  }

  public boolean isTypedef() {
    return haxeClass.getComponentType() == TYPEDEF;
  }

  public boolean isTypeParameter() {
    return  haxeClass instanceof HaxeGenericListPart;
  }
  public boolean isAbstractType() {
    return haxeClass instanceof HaxeAbstractTypeDeclaration;
  }
  public boolean isAnonymous() {
    return haxeClass instanceof HaxeAnonymousType;
  }
  public boolean isObjectLiteral() {
    return haxeClass instanceof HaxeObjectLiteralImpl;
  }

  public boolean isCoreType() {
    return hasCompileTimeMeta(HaxeMeta.CORE_TYPE);
  }

  public boolean hasCompileTimeMeta(@NotNull HaxeMetadataTypeName name) {
    return haxeClass.hasCompileTimeMeta(name);
  }
  public boolean isCallable() {
    return haxeClass.hasCompileTimeMeta(HaxeMeta.CALLABLE);
  }

    public boolean isGenericBuild() {
        return haxeClass.hasCompileTimeMeta(HaxeMeta.GENERIC_BUILD);
    }

    // @:genericBuild macro supports "rest"/vararg typeParameters, so we ignore typeParameters mismatch.
    // https://haxe.org/manual/macro-generic-build.html
    // https://gist.github.com/nadako/b086569b9fffb759a1b5
    public boolean isGenericBuildWithRestTypeParam() {
      if(reference != null) {
        HaxeClassModel haxeClassModel = reference.getHaxeClassModel();
        if (haxeClassModel != null && haxeClassModel.isGenericBuild()) {
          List<HaxeGenericParamModel> genericParams = haxeClassModel.getGenericParams();
          if (!genericParams.isEmpty()) {
            HaxeGenericParamModel last = genericParams.getLast();
            String name = last.haxeClass.getName();
            return name != null && name.equals("Rest");
          }
        }
      }
        return false;
    }


  @Nullable
  public HaxeModifiersModel getModifiers() {

    if (haxeClass instanceof HaxeClassDeclaration classDeclaration) {
      // haxe declaration might be without any modifier
      HaxeClassModifierList list = classDeclaration.getClassModifierList();
      _modifiers = new HaxeModifiersModel(list != null ? list : classDeclaration);
    }

    if (haxeClass instanceof HaxeEnumDeclaration enumDeclaration) {
      PsiModifierList list = enumDeclaration.getModifierList();
      _modifiers = new HaxeModifiersModel(list != null ? list : enumDeclaration);
    }

    if (haxeClass instanceof HaxeInterfaceDeclaration  interfaceDeclaration) {
      PsiModifierList list = interfaceDeclaration.getModifierList();
      _modifiers = new HaxeModifiersModel(list != null ? list : interfaceDeclaration);
    }

    if (haxeClass instanceof HaxeExternInterfaceDeclaration  interfaceDeclaration) {
      PsiModifierList list = interfaceDeclaration.getModifierList();
      _modifiers = new HaxeModifiersModel(list != null ? list : interfaceDeclaration);
    }

    if (haxeClass instanceof HaxeExternClassDeclaration  externClassDeclaration) {
      _modifiers = new HaxeModifiersModel(externClassDeclaration.getExternClassModifierList());
    }
    return _modifiers;
  }

  @Nullable
  public HaxeClassModifierList getModifiersList() {
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

  public List<HaxeReferenceExpression> getUsingMetaReferences() {
    HaxeMetadataList meta = haxeClass.getCompileTimeMeta(HaxeMeta.USING);
    if(meta != null) {
      List<HaxeMetadataCompileTimeMeta> compileTimeMeta = meta.getCompileTimeMeta();

      return compileTimeMeta.stream().map(HaxeMetadataCompileTimeMeta::getContent)
        .filter(Objects::nonNull)
        .map(PsiElement::getFirstChild)// Metadata content is lazy so we access it be sure its parsed
        .map(content ->  Arrays.asList(content.getChildren()))
        .flatMap(Collection::stream)
        .map(e -> PsiTreeUtil.getChildOfType(e, HaxeReferenceExpression.class))
        .filter(Objects::nonNull)
        .toList();
    }
    return List.of();
  }



  @Nullable
  public HaxeTypeOrAnonymous getUnderlyingTypeOrAnonymous() {
    if (isAbstractType()) {
      HaxeAbstractTypeDeclaration abstractDeclaration = (HaxeAbstractTypeDeclaration)haxeClass;
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
  public SpecificTypeReference getUnderlyingType() {
    return getUnderlyingType(null);
  }
  public SpecificTypeReference getUnderlyingType(@Nullable HaxeGenericResolver resolver) {
    if (!isAbstractType() && !isTypedef()) return null;
    HaxeTypeOrAnonymous typeOrAnon = getUnderlyingTypeOrAnonymous();
    if (typeOrAnon != null) {
      ResultHolder resultHolder = HaxeTypeResolver.getTypeFromTypeOrAnonymous(typeOrAnon, resolver);
      if (!resultHolder.isUnknown()) return resultHolder.getType();
    }
    HaxeFunctionType type = getUnderlyingFunctionType();
    if (type != null){
      return getTypeFromFunctionType(type).getFunctionType();
    }
    return null;
  }

  private @Nullable HaxeFunctionType getUnderlyingFunctionType() {
    if (isAbstractType() &&  haxeClass instanceof  HaxeAbstractTypeDeclaration abstractDeclaration) {
      HaxeUnderlyingType underlyingType = abstractDeclaration.getUnderlyingType();
      if (underlyingType != null) {
        return underlyingType.getFunctionType();
      }
    }
    return null;
  }

  @Nullable
  //TODO mlo: rewrite:
  //WARNING!  if underlying is just a TypeParameter then  this can resolve to anything and a class return type can not be guarantied
  public SpecificHaxeClassReference getUnderlyingClassReference(@NotNull HaxeGenericResolver resolver) {
    if (!isAbstractType() && !isTypedef()) return null;

    PsiElement element = getBasePsi();
    HaxeTypeOrAnonymous typeOrAnon = getUnderlyingTypeOrAnonymous();
    if (typeOrAnon != null) {
      HaxeType type = typeOrAnon.getType();
      if (type != null) {
        //HaxeClass aClass = HaxeResolveUtil.tryResolveClassByQName(type);
        ResultHolder resolved = HaxeTypeResolver.getTypeFromType(type, resolver);
        SpecificHaxeClassReference classType = resolved.getClassType();
        if (!resolved.isUnknown() && classType != null) {
          HaxeGenericResolver localResolver = new HaxeGenericResolver();
          localResolver.addAll(classType.getGenericResolver());
          localResolver.addAll(resolver);
          HaxeClass aClass = classType.getHaxeClass();
          if (aClass != null) {
            ResultHolder[] specifics = HaxeTypeResolver.resolveDeclarationParametersToTypes(aClass, localResolver);
            return SpecificHaxeClassReference.withGenerics(new HaxeClassReference(aClass.getModel(), aClass.getModel().haxeClass), specifics);
          }
        }
      } else { // Anonymous type
        HaxeAnonymousType anon = typeOrAnon.getAnonymousType();
        if (anon != null) {
          // Anonymous types don't have parameters of their own, but when they are part of a typedef, they use the parameters from it.
          if(element instanceof  HaxeTypedefDeclaration typedefDeclaration && typedefDeclaration.isGeneric()) {
            HaxeGenericResolver memberResolver = typedefDeclaration.getMemberResolver(null);
            if(memberResolver != null) {
              HaxeClassReference classReference = new HaxeClassReference(anon.getModel(), element);
              return SpecificHaxeClassReference.withGenerics(classReference, memberResolver.getSpecificsFor(classReference));
            }
          }
          return SpecificHaxeClassReference.withGenerics(new HaxeClassReference(anon.getModel(), element), resolver.getSpecifics());
        }
      }
    } else {
      // No typeOrAnon.  This must be Null<T>?
      if ("Null".equals(getName())) {
        List<HaxeGenericParamModel> typeParams = getGenericParams();
        if (typeParams.size() == 1) {
          HaxeGenericParamModel param = typeParams.get(0);
          ResultHolder result = resolver.resolveTypeParameter(param.getTypeParameter());
          if (result != null) {
            return result.getClassType();
          }
        }
      }
    }
    return null;
  }
  @Nullable
  public SpecificFunctionReference getUnderlyingFunctionReference(HaxeGenericResolver resolver) {
    if (!isAbstractType() && !isTypedef()) return null;
    PsiElement element = getBasePsi();
    HaxeTypeOrAnonymous typeOrAnon = getUnderlyingTypeOrAnonymous();
    if (typeOrAnon != null) {
      // TODO mlo handle abstracts with functions as type ?
    } else {
      // No typeOrAnon.  This must be Null<T>?
      if ("Null".equals(getName())) {
        List<HaxeGenericParamModel> typeParams = getGenericParams();
        if (typeParams.size() == 1) {
          HaxeGenericParamModel param = typeParams.get(0);
          ResultHolder result = resolver.resolveTypeParameter(param.getTypeParameter());
          if (result != null) {
            return result.getFunctionType();
          }
        }
      }
    }
    return null;
  }

  public List<HaxeType> getAbstractToList() {
    if (!isAbstractType() ) return Collections.emptyList();

    List<HaxeType> types = new LinkedList<HaxeType>();
    if (haxeClass instanceof HaxeAbstractTypeDeclaration abstractClass) {
      List<HaxeAbstractToType> list = abstractClass.getAbstractToTypeList();
      for (HaxeAbstractToType toType : list) {
        if (toType.getTypeOrAnonymous() != null) {
          types.add(toType.getTypeOrAnonymous().getType());
        }
      }
    }
    return types;
  }



  public List<HaxeType> getAbstractFromList() {
    if (!isAbstractType() ) return Collections.emptyList();
    List<HaxeType> types = new LinkedList<HaxeType>();
    if (haxeClass instanceof  HaxeAbstractTypeDeclaration abstractClass) {
      List<HaxeAbstractFromType> list = abstractClass.getAbstractFromTypeList();
      for (HaxeAbstractFromType fromType : list) {
        if (fromType.getTypeOrAnonymous() != null) {
          types.add(fromType.getTypeOrAnonymous().getType());
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
  public List<HaxeMethodModel> getConstructors(@Nullable HaxeGenericResolver resolver) {
      List<HaxeMethodModel> normalConstructors = getMethods(resolver).stream()
            .filter(HaxeMethodModel::isConstructor)
            .toList();

      List<HaxeMethodModel> constructors = new ArrayList<>(normalConstructors);

    for (HaxeMethodModel constructor : normalConstructors) {
      HaxeMethod method = constructor.getMethod();
      if (method.hasCompileTimeMetadata(HaxeMetadataCompileTimeMeta.OVERLOAD)) {
        List<HaxeMethodModel> overloadConstructors = method.getModel().extractOverloadsForMethod().stream()
                .map(HaxeMethodPsiMixin::getModel)
                .toList();

        constructors.addAll(overloadConstructors);
      }
    }
    return constructors;
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

  @Nullable
  public HaxeBaseMemberModel getMember(String name, @Nullable HaxeGenericResolver resolver) {
    if (name == null) return null;
    List<HaxeNamedComponent> members = haxeClass.findHaxeMemberByName(name, resolver);
    if (!members.isEmpty()) {
      HaxeNamedComponent component = members.getFirst();
      return HaxeMemberModel.fromPsi(component);
    }
    return null;
  }

  @NotNull
  public List<HaxeBaseMemberModel> getMembers(String name, @Nullable HaxeGenericResolver resolver) {
    if (name == null) return List.of();
    List<HaxeNamedComponent> members = haxeClass.findHaxeMemberByName(name, resolver);
    return members.stream().map(HaxeBaseMemberModel::fromPsi).toList();
  }

  @NotNull
  public List<HaxeBaseMemberModel> getMembers(@Nullable HaxeGenericResolver resolver) {
    final List<HaxeBaseMemberModel> members = new ArrayList<>();
    members.addAll(getMethods(resolver));
    members.addAll(getFields());
    return members;
  }
  @NotNull
  public List<HaxeBaseMemberModel> getAllMembers(@Nullable HaxeGenericResolver resolver) {
    final List<HaxeBaseMemberModel> members = new ArrayList<>();
    members.addAll(getAllMethods(resolver));
    members.addAll(getAllFields(resolver));
    return members;
  }

  private List<HaxeFieldModel> getAllFields(@Nullable HaxeGenericResolver resolver) {
    List<HaxeFieldModel> models =  getAncestorFields(resolver);
    models.addAll( getFields());

    return models;
  }

  @NotNull
  public List<HaxeBaseMemberModel> getMembersSelf() {
    if(haxeClass instanceof AbstractHaxePsiClass psiClass) {
      HaxeClassStub greenStub = psiClass.getGreenStub();
      if(greenStub != null) {
        List<HaxeBaseMemberModel> list = new ArrayList<>();
        for (StubElement<?> element : greenStub.getChildrenStubs()) {
          HaxeBaseMemberModel model = HaxeBaseMemberModel.fromPsi(element.getPsi());
          if (model != null) {
            list.add(model);
          }
        }
        return list;
      }
    }

    final List<HaxeBaseMemberModel> members = new ArrayList<>();
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

  @Nullable
  public HaxeBaseMemberModel getMemberSelf(String name, @Nullable HaxeGenericResolver resolver) {
    for (HaxeBaseMemberModel model : getMembersSelf()) {
      if (name.equals(model.getName())) {
        return model;
      }
    }
    return null;
  }

  public HaxeFieldModel getField(String name, @Nullable HaxeGenericResolver resolver) {
    HaxePsiField field = (HaxePsiField)haxeClass.findHaxeFieldByName(name, resolver);
    if (field instanceof HaxeFieldDeclaration || field instanceof HaxeAnonymousTypeField || field instanceof HaxeEnumValueDeclaration) {
      return (HaxeFieldModel)field.getModel();
    }
    return null;
  }

  public HaxeMethodModel getMethod(String name, @Nullable HaxeGenericResolver resolver) {
    List<HaxeNamedComponent> methods = haxeClass.findHaxeMethodByName(name, resolver);
    if(!methods.isEmpty()  && methods.getFirst() instanceof HaxeMethodPsiMixin method) return method.getModel();
    return null;
  }

  public List<HaxeMethodModel> getMethods(@Nullable HaxeGenericResolver resolver) {
    List<HaxeMethodModel> models = new ArrayList<HaxeMethodModel>();
    for (HaxeMethod method : haxeClass.getHaxeMethodsSelf(resolver)) {
      models.add(method.getModel());
    }
    return models;
  }
  public List<HaxeMethodModel> getAllMethods(@Nullable HaxeGenericResolver resolver) {
    List<HaxeMethodModel> models =  getAncestorMethods(resolver);
    for (HaxeMethod method : haxeClass.getHaxeMethodsSelf(resolver)) {
      models.add(method.getModel());
    }

    return models;
  }

  public List<HaxeMethodModel> getMethodsSelf(@Nullable HaxeGenericResolver resolver) {
    List<HaxeMethodModel> models = new ArrayList<HaxeMethodModel>();
    for (HaxeMethod method : haxeClass.getHaxeMethodsSelf(resolver)) {
      if (method.getContainingClass() == this.haxeClass) models.add(method.getModel());
    }
    return models;
  }

  public List<HaxeMethodModel> getAncestorMethods(@Nullable HaxeGenericResolver resolver) {
    List<HaxeMethodModel> models = new ArrayList<HaxeMethodModel>();
    for (HaxeMethod method : haxeClass.getHaxeMethodsAncestor(true)) {
        models.add(method.getModel());
    }
    return models;
  }
  public List<HaxeFieldModel> getAncestorFields(@Nullable HaxeGenericResolver resolver) {
    List<HaxeFieldModel> models = new ArrayList<>();
    for (HaxePsiField field : haxeClass.getHaxeFieldsAncestor(true)) {
        models.add((HaxeFieldModel)field.getModel());
    }
    return models;
  }
  public HaxeMethodModel getAncestorMethod(String name, @Nullable HaxeGenericResolver resolver) {
    for (HaxeMethod method : haxeClass.getHaxeMethodsAncestor(true)) {
          HaxeMethodModel methodModel = method.getModel();
          if (name.equals(methodModel.getName())) return  methodModel;
        }
    return null;
  }

  @NotNull
  public HaxeClass getPsi() {
    return haxeClass;
  }

  @Nullable
  public HaxePsiCompositeElement getBodyPsi() {
    return (haxeClass instanceof HaxeClassDeclaration classDeclaration) ? classDeclaration.getClassBody() : null;
  }

  @Nullable
  public PsiIdentifier getNamePsi() {
    return haxeClass.getNameIdentifier();
  }
  @NotNull
  public List<HaxeType> getExtendsList() {
    return CachedValuesManager.getProjectPsiDependentCache(haxeClass, HaxeClassModel::getHaxeExtendsListCached);
  }
  private static List<HaxeType> getHaxeExtendsListCached(@NotNull HaxeClass haxeClass) {
    List<HaxeType> list = haxeClass.getHaxeExtendsList();
    return  List.copyOf(list);
  }

  @NotNull
  public List<HaxeType> getImplementsList() {
    return CachedValuesManager.getProjectPsiDependentCache(haxeClass, HaxeClassModel::getHaxeImplementsListCached);
  }
  private static List<HaxeType> getHaxeImplementsListCached(@NotNull HaxeClass haxeClass) {
    List<HaxeType> list = haxeClass.getHaxeImplementsList();
    return  List.copyOf(list);
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
          return new FullyQualifiedInfo(containerInfo.packagePath, containerInfo.moduleName, getName(), null);
        }
      }
    return null;
  }

  @Override
  public boolean isValid() {
    return haxeClass.isValid();
  }

  public void addMethodsFromPrototype(List<HaxeMethodModel> methods) {
    throw new NotImplementedException("Not implemented HaxeClassMethod.addMethodsFromPrototype() : check HaxeImplementMethodHandler");
  }

  public List<HaxeFieldModel> getFields() {
    // TODO: Figure out if this needs to deal with forwarded fields in abstracts.
    HaxePsiCompositeElement body = PsiTreeUtil.getChildOfAnyType(haxeClass, isEnum() ? HaxeEnumBody.class : HaxeClassBody.class, HaxeInterfaceBody.class, HaxeExternClassDeclarationBody.class);

    if (body != null) {
      List<HaxeFieldModel> list = new ArrayList<>();
      List<HaxePsiField> children = PsiTreeUtil.getChildrenOfAnyType(body, HaxeFieldDeclaration.class, HaxeAnonymousTypeField.class, HaxeEnumValueDeclarationField.class);

      for (HaxePsiField field : children) {
        HaxeFieldModel model = (HaxeFieldModel)field.getModel();
        list.add(model);
      }
      return list;
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
    if(isTypeParameter()) {
      if(haxeClass instanceof HaxeGenericListPart genericListPart) {
        //TODO handle unify generics with constraints
        HaxeGenericConstraintPart constraint = genericListPart.getGenericConstraintPart();
        if (constraint != null) {
          ResultHolder type = HaxeTypeResolver.getTypeFromGenericConstraint(constraint);
          if (type != null && type.getClassType() != null) {
            HaxeClassModel model = type.getClassType().getHaxeClassModel();
            if (model != null) {
              model.writeCompatibleTypes(output);
            }
          }
        }
      }
    }

    // Interfaces
    for (HaxeClassReferenceModel model : this.getImplementingInterfaces()) {
      if (model == null) continue;
      final HaxeClassModel aInterface = model.getHaxeClassModel();
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
    return getGenericParamPsi() != null;
  }

  @NotNull
  public List<HaxeGenericParamModel> getGenericParams() {
    final List<HaxeGenericParamModel> out = new ArrayList<>();
    // anonymous structures does not have TypeParameters on their own, but their parent may declare them.
      HaxeGenericParam genericParam = getGenericParamPsi();
      if (genericParam != null) {
        for (HaxeGenericListPart part : genericParam.getGenericListPartList()) {
          out.add(part.getModel());
        }
      }
    return out;
  }

  @Nullable
  private HaxeGenericParam getGenericParamPsi() {
    return CachedValuesManager.getProjectPsiDependentCache(haxeClass, HaxeClassModel::getGenericParamPsiCached);
  }

  private static HaxeGenericParam getGenericParamPsiCached(@NotNull HaxeClass haxeClass) {
    boolean isAnonymous = haxeClass instanceof HaxeAnonymousType;
    //TODO Should probably rewrite so that changes in parent will invalidate cache
    HaxeGenericParam param = isAnonymous ? getGenericParamFromParent(haxeClass) : haxeClass.getGenericParam();
    return  param;
  }

  /**
   * only intended for typedefs with anonymous structures
   */
  private static HaxeGenericParam getGenericParamFromParent(HaxeClass haxeClass) {
    HaxeTypedefDeclaration type = PsiTreeUtil.getStubOrPsiParentOfType(haxeClass, HaxeTypedefDeclaration.class);
    if (type == null) return null;
    return type.getGenericParam();
  }

  @NotNull
  public HaxeGenericResolver getGenericResolver(@Nullable HaxeGenericResolver parentResolver) {
    HaxeGenericParam param = getGenericParamPsi();
    if (param != null) {

      HaxeGenericResolver resolver = new HaxeGenericResolver();
      for (HaxeGenericListPart part : param.getGenericListPartList()) {
        HaxeGenericParamModel model = part.getModel();
        ResultHolder constraint = model.getConstraint(parentResolver);
        if (null == constraint) {
          constraint = new ResultHolder(SpecificTypeReference.getUnknown(getBasePsi()));
        }
        resolver.addConstraint(model.getTypeParameter(), constraint);
      }

      return resolver;
    }
    return new HaxeGenericResolver();
  }
  @NotNull
  private ResultHolder[] getSpecifics() {
    HaxeGenericParam param = getGenericParamPsi();
    if (param == null) return new ResultHolder[0];
    List<HaxeGenericListPart> list = param.getGenericListPartList();
    ResultHolder[] specifics = new ResultHolder[list.size()];
    for (int i = 0; i < list.size(); i++) {
      HaxeGenericListPart part = list.get(i);
      HaxeGenericParamModel model = part.getModel();
      ResultHolder TypeParam = model.getTypeParameter().getModel().getInstanceType();
      specifics[i] = TypeParam;
    }
    return specifics;
  }

  public void addField(String name, SpecificTypeReference type) {
    this.getDocument().addTextAfterElement(getBodyPsi(), "\npublic var " + name + ":" + type.toStringWithoutConstant() + ";\n");
  }

  public void addMethod(String name) {
    this.getDocument().addTextAfterElement(getBodyPsi(), "\npublic function " + name + "() {\n}\n");
  }

  public void addImplements(String name) {
    if ( haxeClass instanceof AbstractHaxePsiClass psiClass) {
      HaxeInheritList implementsListPsi = psiClass.getHaxeImplementsListPsi();
      if (implementsListPsi != null) {
        List<HaxeImplementsDeclaration> list = implementsListPsi.getImplementsDeclarationList();
        String insertText = " implements " + name + " ";
        if (list.isEmpty()) {
          this.getDocument().addTextAfterElement(implementsListPsi, insertText);
        }else {
          HaxeImplementsDeclaration declaration = list.get(list.size() - 1);
          this.getDocument().addTextAfterElement(declaration, insertText);
        }
      }
    }
  }
  public void changeToInterface(String name) {
    if ( haxeClass instanceof AbstractHaxePsiClass psiClass) {
      HaxeInheritList implementsListPsi = psiClass.getHaxeImplementsListPsi();
      if (implementsListPsi != null) {
        List<HaxeExtendsDeclaration> list = implementsListPsi.getExtendsDeclarationList();
        Optional<HaxeExtendsDeclaration> first = list.stream().filter(declaration -> Objects.equals(declaration.getType().getText(), name)).findFirst();
        String replacementText = "implements " + name;
        if (first.isPresent()) {
          HaxeExtendsDeclaration declaration = first.get();
          this.getDocument().replaceElementText(declaration, replacementText);
        }
      }
    }
  }
  public void changeToExtends(String name) {
    if ( haxeClass instanceof AbstractHaxePsiClass psiClass) {
      HaxeInheritList implementsListPsi = psiClass.getHaxeImplementsListPsi();
      if (implementsListPsi != null) {
        List<HaxeImplementsDeclaration> list = implementsListPsi.getImplementsDeclarationList();
        Optional<HaxeImplementsDeclaration> first = list.stream().filter(declaration -> Objects.equals(declaration.getType().getText(), name)).findFirst();
        String replacementText = "extends " + name;
        if (first.isPresent()) {
          HaxeImplementsDeclaration declaration = first.get();
          this.getDocument().replaceElementText(declaration, replacementText);
        }
      }
    }
  }
  public void addExtends(String name) {
    if ( haxeClass instanceof AbstractHaxePsiClass psiClass) {
      HaxeInheritList implementsListPsi = psiClass.getHaxeImplementsListPsi();
      if (implementsListPsi != null) {
        List<HaxeExtendsDeclaration> list = implementsListPsi.getExtendsDeclarationList();
        String insertText = " extends " + name + " ";
        if (list.isEmpty()) {
          this.getDocument().addTextAfterElement(implementsListPsi, insertText);
        }else {
          HaxeExtendsDeclaration declaration = list.get(list.size() - 1);
          this.getDocument().addTextAfterElement(declaration, insertText);
        }
      }
    }
  }
  public void removeImplements(String name) {
    if ( haxeClass instanceof AbstractHaxePsiClass psiClass) {
      HaxeInheritList implementsListPsi = psiClass.getHaxeImplementsListPsi();
      if (implementsListPsi != null) {

        List<HaxeImplementsDeclaration> list = implementsListPsi.getImplementsDeclarationList();
        Optional<HaxeImplementsDeclaration> first = list.stream().filter(declaration -> Objects.equals(declaration.getType().getText(), name)).findFirst();

        if (first.isPresent()) {
          HaxeImplementsDeclaration declaration = first.get();
          this.getDocument().replaceElementText(declaration, "");
        }
      }
    }
  }
  public void removeExtends(String name) {
    if ( haxeClass instanceof AbstractHaxePsiClass psiClass) {
      HaxeInheritList implementsListPsi = psiClass.getHaxeImplementsListPsi();
      if (implementsListPsi != null) {
        List<HaxeExtendsDeclaration> list = implementsListPsi.getExtendsDeclarationList();
        Optional<HaxeExtendsDeclaration> first = list.stream().filter(declaration -> Objects.equals(declaration.getType().getText(), name)).findFirst();

        if (first.isPresent()) {
          HaxeExtendsDeclaration declaration = first.get();
          this.getDocument().replaceElementText(declaration, "");
        }
      }
    }
  }

  @Override
  public List<HaxeModel> getExposedMembers() {
    // TODO ClassModel concept should be reviewed. We need to separate logic of abstracts, regular classes, enums, etc. Right now this class a bunch of if-else conditions. It looks dirty.
    ArrayList<HaxeModel> out = new ArrayList<>();
    if (isClass()) {
      PsiElement body = getBodyPsi();
      if (body != null) {
        List<? extends HaxeNamedComponent> children = PsiTreeUtil.getChildrenOfAnyType(body, HaxeFieldDeclaration.class, HaxeMethod.class);
        for (HaxeNamedComponent declaration : children) {
          if (!(declaration instanceof PsiMember)) continue;
          if (declaration instanceof HaxeFieldDeclaration varDeclaration) {
            if (varDeclaration.isPublic() && varDeclaration.isStatic()) {
              out.add(varDeclaration.getModel());
            }
          } else {
            HaxeMethodDeclaration method = (HaxeMethodDeclaration)declaration;
            if (method.isStatic() && method.isPublic()) {
              out.add(method.getModel());
            }
          }
        }
      }
    } else if (isEnum()) {
      HaxeEnumBody body = UsefulPsiTreeUtil.getChild(haxeClass, HaxeEnumBody.class);
      if (body != null) {
        for (HaxeEnumValueDeclarationField field : body.getEnumValueDeclarationFieldList()) {
          out.add(field.getModel());
        }
        for (HaxeEnumValueDeclarationConstructor constructor : body.getEnumValueDeclarationConstructorList()) {
          out.add(constructor.getModel());
        }
      }
    }
    return out;
  }

  public static HaxeClassModel fromElement(PsiElement element) {
    if (element == null) return null;

    HaxeClass haxeClass = element instanceof HaxeClass
                          ? (HaxeClass) element
                          : PsiTreeUtil.getStubOrPsiParentOfType(element, HaxeClass.class);

    if (haxeClass != null) {
      return haxeClass.getModel();
    }
    return null;
  }

  public boolean isPublic() {
    return haxeClass.isPublic();
  }

  public boolean isAbstractClass() {
    HaxeModifiersModel modifiers = getModifiers();
    if (modifiers == null) return false;
    return modifiers.hasModifier(HaxePsiModifier.ABSTRACT);
  }

  public boolean isFinal() {
    HaxeModifiersModel modifiers = getModifiers();
    if (modifiers == null) return false;
    return modifiers.hasModifier(HaxePsiModifier.FINAL);
  }

  public List<HaxeMethodModel>  getExtensionMethodsFromMeta() {
    List<HaxeReferenceExpression> referenceExpressions = getUsingMetaReferences();
    if(referenceExpressions.isEmpty()) return List.of();

    HaxeGenericResolver genericResolver = getGenericResolver(null);
    SpecificHaxeClassReference classReference = SpecificHaxeClassReference.withoutGenerics(getReference());
    ResultHolder classRef = classReference.createHolder();

    // NOTE!: DO NOT USE "PsiReference::resolve", on referenceExpressions here.
    // it will in some cases fail to resolve, probably due to recursion guards and other "active" resolves in same package
    // use findClassByQName instead to be sure we try to find the class directly
    List<HaxeClassModel> classModels = referenceExpressions.stream().map(PsiElement::getText)
      .map(qname -> HaxeResolveUtil.findClassByQName(qname, haxeClass))
      .filter(Objects::nonNull)
      .map(HaxeClass::getModel)
      .toList();

    return  classModels.stream()
      .map(classModel -> classModel.getMethods(genericResolver))
      .flatMap(Collection::stream)
      .filter(method -> !method.isConstructor() && method.isStatic() && method.isPublic())
      .filter(method ->  canAssignToFirstParam(method, classRef, genericResolver))
      .toList();

  }

  private boolean canAssignToFirstParam(HaxeMethodModel method, ResultHolder classRef, HaxeGenericResolver resolver) {
    List<HaxeParameterModel> parameters = method.getParameters();
    if (!parameters.isEmpty()) {
      HaxeParameterModel paramModel = parameters.get(0);
      ResultHolder paramResult = paramModel.getType(resolver);
      boolean b = paramResult.canAssign(classRef);
      return b;
    }
    return false;
  }

  public boolean isStructInit() {
    return hasCompileTimeMeta(HaxeMeta.STRUCT_INIT);
  }

  private final RecursionGuard<PsiElement> inheritsFromRecursionGuard = RecursionManager.createGuard("inheritsFromRecursionGuard");

  public boolean inheritsFrom(HaxeClass haxeClass) {
    return Boolean.TRUE.equals(inheritsFromRecursionGuard.doPreventingRecursion(this.haxeClass, true, () -> {
        List<HaxeClassReferenceModel> interfaces = getImplementingInterfaces();
        for (HaxeClassReferenceModel anInterface : interfaces) {
            HaxeClassModel haxeClassModel = anInterface.getHaxeClassModel();
            if (haxeClassModel != null) {
                if (haxeClassModel.haxeClass == haxeClass) return true;
                if (haxeClassModel.inheritsFrom(haxeClass)) return true;
            }
        }
        List<HaxeClassReferenceModel> extendingTypes = getExtendingTypes();
        for (HaxeClassReferenceModel extendingType : extendingTypes) {
            HaxeClassModel haxeClassModel = extendingType.getHaxeClassModel();
            if (haxeClassModel != null) {
                if (haxeClassModel.haxeClass == haxeClass) return true;
                if (haxeClassModel.inheritsFrom(haxeClass)) return true;
            }
        }
        return false;
    }));
  }

    public HaxeModuleModel getModule() {
      HaxeModule module = haxeClass.getModule();
       if(module.getModel() instanceof HaxeModuleModel model) {
         return model;
       }
       return null;
    }
}
