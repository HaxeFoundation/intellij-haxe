/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 TiVo Inc.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2020 Eric Bishton
 * Copyright 2017-2018 Ilya Malanin
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
package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.LogLevel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;

import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.util.HaxeNamedSubComponentUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.InheritanceImplUtil;
import com.intellij.psi.impl.PsiClassImplUtil;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.impl.PsiSuperMethodImplUtil;
import com.intellij.psi.impl.source.tree.ChildRole;
import com.intellij.psi.impl.source.tree.java.PsiTypeParameterListImpl;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import lombok.CustomLog;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author: Fedor.Korotkov
 */
@CustomLog
public abstract class AbstractHaxePsiClass extends AbstractHaxeNamedComponent implements HaxeClass {


  private Boolean _isPrivate = null;

  static {
    log.info("Loaded AbstractHaxePsiClass");
    log.setLevel(LogLevel.DEBUG);
  }

  public AbstractHaxePsiClass(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public HaxeNamedComponent getTypeComponent() {
    return this;
  }

  @NotNull
  @Override
  public String getQualifiedName() {
    String name = getName();
    if (getParent() == null) {
      return name == null ? "" : name;
    }

    if (name == null && this instanceof HaxeAnonymousType) {
      // restore name from parent
      final HaxeTypedefDeclaration typedefDecl = UsefulPsiTreeUtil.getParentOfType(this, HaxeTypedefDeclaration.class);
      if (null != typedefDecl) {
        name = typedefDecl.getName();
      }
    }

    PsiFile file = getContainingFile();
    if (file == null) return name == null ? "" : name;

    final String fileName = FileUtil.getNameWithoutExtension(file.getName());
    String packageName = HaxeResolveUtil.getPackageName(file);

    if (name != null && isAncillaryClass(packageName, name, fileName)) {
      packageName = HaxeResolveUtil.joinQName(packageName, fileName);
    }

    return HaxeResolveUtil.joinQName(packageName, name);
  }

  private HaxeClassModel _model = null;

  @NotNull
  public HaxeClassModel getModel() {
    if (_model == null) {
      // note HaxeGenericConstraintPartImpl implements anonymous interface so it must be first
      // this is a temp workaround until the class hierarchy and abstractClass can be replaced with more flexible interfaces
      if (this instanceof HaxeConstraintTypeListImpl constraintTypeList) {
        _model = new HaxeConstraintTypeListModel(constraintTypeList);
      } else if (this instanceof HaxeAnonymousType anonymousType) {
        _model = new HaxeAnonymousTypeModel(anonymousType);
      } else if (this instanceof HaxeEnumDeclaration enumDeclaration) {
        _model = new HaxeEnumModelImpl(enumDeclaration);
      } else if (this instanceof HaxeExternClassDeclaration externClassDeclaration) {
        _model = new HaxeExternClassModel(externClassDeclaration);
      } else if (this instanceof HaxeObjectLiteralImpl objectLiteral) {
        _model =  new HaxeObjectLiteralClassModel(objectLiteral);
      } else if (this instanceof HaxeGenericListPart genericListPart) {
        _model = new HaxeGenericParamModel(genericListPart);
      } else if (this instanceof HaxeAbstractTypeDeclaration abstractDeclaration) {
        if (abstractDeclaration.isEnum()) {
          _model = new HaxeAbstractEnumModel(abstractDeclaration);
        } else {
          _model = new HaxeAbstractClassModel(abstractDeclaration);
        }
      } else {
        _model = new HaxeClassModel(this);
      }
    }

    return _model;
  }

  // check if class is declared inside haxe module `MyClass.MySupportType`
  private boolean isAncillaryClass(@NotNull String packageName, @NotNull String name, @NotNull String fileName) {
    // if file name matches type name
    if (fileName.equals(name)) {
      return false;
    }
    // if StdTypes
    if (packageName.isEmpty() && fileName.equals("StdTypes")) {
      return false;
    }
    // file contains valid type declaration
    return HaxeResolveUtil.findComponentDeclaration(getContainingFile(), name) != null;
  }

  @Override
  public boolean isExtern() {
    return (this instanceof HaxeExternClassDeclaration || this instanceof HaxeExternInterfaceDeclaration);
  }

  @Override
  public boolean isAbstractType() {
    return (this instanceof HaxeAbstractTypeDeclaration);
  }
  public boolean isObjectLiteralType() {
    return (this instanceof HaxeObjectLiteral);
  }


  @Override
  public boolean isInterface() {
    return HaxeComponentType.typeOf(this) == HaxeComponentType.INTERFACE;
  }
  @Override
  public boolean isTypeDef() {
    return  HaxeComponentType.typeOf(this)  == HaxeComponentType.TYPEDEF;
  }
  @Override
  public boolean isAnonymousType() {
    return  false;
  }

  public boolean isTypeParameter() {
    return  false;
  }




  @NotNull
  @Override
  public List<HaxeType> getHaxeExtendsList() {
    return HaxeResolveUtil.findExtendsList(PsiTreeUtil.getChildOfType(this, HaxeInheritList.class));
  }

  @NotNull
  @Override
  public List<HaxeType> getHaxeImplementsList() {
    return HaxeResolveUtil.getImplementsList(PsiTreeUtil.getChildOfType(this, HaxeInheritList.class));
  }
  public @Nullable HaxeInheritList getHaxeImplementsListPsi() {
    return PsiTreeUtil.getChildOfType(this, HaxeInheritList.class);
  }

  @Override
  public PsiElement add(@NotNull PsiElement element) throws IncorrectOperationException {
    return super.add(element);
  }

  @NotNull
  private static List<HaxeMethod> getHaxeMethodsSelfCached(@NotNull HaxeClass haxeClass) {
    return CachedValuesManager.getCachedValue(haxeClass, () -> {
      final List<HaxeNamedComponent> classMembers = HaxeNamedSubComponentUtil.getNamedSubComponentsFromClassType(haxeClass);
      final List<HaxeNamedComponent> methods = HaxeNamedSubComponentUtil.filterNamedComponentsByType(classMembers, HaxeComponentType.METHOD);
      final List<HaxeMethod> result = new ArrayList<>();
      for (HaxeNamedComponent method : methods) {
        result.add((HaxeMethod) method);
      }
      return new CachedValueProvider.Result<>(result, haxeClass);
    });
  }

  @NotNull
  @Override
  public List<HaxeMethod> getHaxeMethodsSelf(@Nullable HaxeGenericResolver resolver) {
    return getHaxeMethodsSelfCached(this);
  }

  @NotNull
  private static List<HaxeNamedComponent> getHaxeFieldsSelfCached(@NotNull HaxeClass haxeClass) {
    return CachedValuesManager.getCachedValue(haxeClass, () -> {
      final List<HaxeNamedComponent> result = HaxeNamedSubComponentUtil.getNamedSubComponents(haxeClass, false);
      List<HaxeNamedComponent> components = HaxeNamedSubComponentUtil.filterNamedComponentsByType(result, HaxeComponentType.FIELD);
      return new CachedValueProvider.Result<>(components, haxeClass);
    });
  }

  @NotNull
  @Override
  public List<HaxeNamedComponent> getHaxeFieldsSelf(@Nullable HaxeGenericResolver resolver) {
    return getHaxeFieldsSelfCached(this);
  }

  @NotNull
  @Override
  public List<HaxeFieldDeclaration> getFieldSelf(@Nullable HaxeGenericResolver resolver) {
    return HaxeResolveUtil.getClassVarDeclarations(this);
  }

  @Nullable
  @Override
  public HaxeNamedComponent findHaxeFieldByName(@NotNull final String name, @Nullable HaxeGenericResolver resolver) {
    List<HaxeNamedComponent> all = getHaxeFieldAll(HaxeComponentType.INTERFACE);
    return ContainerUtil.find(all, component -> name.equals(component.getName()));
  }



  @Override
  public HaxeNamedComponent findHaxeMethodByName(@NotNull final String name, @Nullable HaxeGenericResolver resolver) {
    List<HaxeMethod> all = getHaxeMethodsAll(HaxeComponentType.INTERFACE);
    return ContainerUtil.find(all, (Condition<HaxeNamedComponent>)component -> name.equals(component.getName()));
  }

  /** Optimized path to replace findHaxeMethod and findHaxeField when used together. */
  @Override
  public HaxeNamedComponent findHaxeMemberByName(@NotNull final String name, @Nullable HaxeGenericResolver resolver) {
    List<HaxeNamedComponent> namedSubComponents = HaxeNamedSubComponentUtil.getAllNamedSubComponentsInType(this, resolver);
    return ContainerUtil.find(namedSubComponents,
                              component -> {
      HaxeComponentType type = HaxeComponentType.typeOf(component);
      return ((type == HaxeComponentType.FIELD || type == HaxeComponentType.METHOD)
              && name.equals(component.getName()));
                              });
  }

  @Nullable
  @Override
  public HaxeNamedComponent findArrayAccessGetter(@Nullable HaxeGenericResolver resolver) {
    HaxeNamedComponent accessor = ContainerUtil.find(getHaxeMethodsSelf(resolver), new Condition<HaxeNamedComponent>() {
      @Override
      public boolean value(HaxeNamedComponent component) {
        if (component instanceof HaxeMethod) {
          HaxeMethodModel model = ((HaxeMethod)component).getModel();
          return model != null && model.isArrayAccessor() && model.getParameterCount() == 1;
        }
        return false;
      }
    });
    // Maybe old style getter?
    if (null == accessor) {
      accessor = findHaxeMethodByName("__get", resolver);
    }
    // maybe ArrayAccess interface for externs (see hackish workaround where findArrayAccessGetter is used)
    if (null == accessor) {
      if (this.isExtern()){
        Optional<HaxeType> arrayAccess = this.getHaxeImplementsList().stream()
          .filter(haxeType -> haxeType.getReferenceExpression().getQualifiedName().equals("ArrayAccess")).findFirst();
        if(arrayAccess.isPresent()) {
          return arrayAccess.get().getReferenceExpression().resolveHaxeClass().getHaxeClass();
        }
      }
    }
    return accessor;
  }
  @Nullable
  @Override
  public HaxeNamedComponent findArrayAccessSetter(@Nullable HaxeGenericResolver resolver) {
    HaxeNamedComponent accessor = ContainerUtil.find(getHaxeMethodsSelf(resolver), new Condition<HaxeNamedComponent>() {
      @Override
      public boolean value(HaxeNamedComponent component) {
        if (component instanceof HaxeMethod) {
          HaxeMethodModel model = ((HaxeMethod)component).getModel();
          return model != null && model.isArrayAccessor() && model.getParameterCount() == 2;
        }
        return false;
      }
    });
    // Maybe old style getter?
    if (null == accessor) {
      accessor = findHaxeMethodByName("__set", resolver);
    }
    // maybe ArrayAccess interface for externs (see hackish workaround where findArrayAccessGetter is used)
    if (null == accessor) {
      if (this.isExtern()){
        Optional<HaxeType> arrayAccess = this.getHaxeImplementsList().stream()
          .filter(haxeType -> haxeType.getReferenceExpression().getQualifiedName().equals("ArrayAccess")).findFirst();
        if(arrayAccess.isPresent()) {
          return arrayAccess.get().getReferenceExpression().resolveHaxeClass().getHaxeClass();
        }
      }
    }
    return accessor;
  }

  @Override
  public HaxeGenericResolver getMemberResolver(HaxeGenericResolver resolver) {
    return resolver;
  }

  @Override
  public boolean isGeneric() {
    return getGenericParam() != null;
  }

  @Override
  public boolean isEnum() {
    if (HaxeComponentType.typeOf(this) == HaxeComponentType.ENUM) return true;
    if (isAbstractType()) {
      return hasCompileTimeMeta(HaxeMeta.ENUM) || ((HaxeAbstractTypeDeclaration)this).getAbstractClassType().getFirstChild().textMatches("enum");
    }
    return false;
  }

  @Override
  public boolean isAnnotationType() {
    /* both: annotation & typedef in haxe are treated as typedef! */
    return (HaxeComponentType.typeOf(this) == HaxeComponentType.TYPEDEF);
  }

  @Override
  public boolean isDeprecated() {
    /* not applicable to Haxe language */
    return false;
  }

  @Override
  @NotNull
  public PsiClass[] getSupers() {
    // Extends and Implements in one list
    return PsiClassImplUtil.getSupers(this);
  }

  @Override
  public PsiClass getSuperClass() {
    return PsiClassImplUtil.getSuperClass(this);
  }

  @Override
  @NotNull
  public PsiClassType[] getSuperTypes() {
    return PsiClassImplUtil.getSuperTypes(this);
  }

  @Override
  public PsiElement getScope() {
    String name = this.getName();
    if (null == name || "".equals(name)) {
      // anonymous class inherits containing class' search scope
      return this.getContainingClass();
    }
    return this.getContainingFile();
  }

  @Override
  public PsiClass getContainingClass() {
    PsiElement parent = getParent();
    return (parent instanceof PsiClass ? (PsiClass)parent : null);
  }

  @Override
  public PsiClass[] getInterfaces() {  // Extends and Implements in one list
    return PsiClassImplUtil.getInterfaces(this);
  }

  @Override
  @Nullable
  public PsiReferenceList getExtendsList() {
    HaxeInheritList inh = PsiTreeUtil.getChildOfType(this, HaxeInheritList.class);
    return null == inh ? null : PsiTreeUtil.getChildOfType(inh, HaxeExtendsDeclaration.class);
  }

  @Override
  @NotNull
  public PsiClassType[] getExtendsListTypes() {
    final PsiReferenceList extendsList = this.getExtendsList();
    if (extendsList != null) {
      return extendsList.getReferencedTypes();
    }else if (this instanceof  HaxeTypedefDeclaration typeDeclaration) {
      HaxeTypeOrAnonymous typeOrAnonymous = typeDeclaration.getTypeOrAnonymous();
      if (typeOrAnonymous != null) {
        HaxeType type = typeOrAnonymous.getType();
        if(type != null) {
          HaxeReferenceExpression referenceExpression = type.getReferenceExpression();
          return new PsiClassType[]{getReferencedType(referenceExpression)};
        }
      }
    }
    return PsiClassType.EMPTY_ARRAY;
  }

  @NotNull
  private PsiClassType getReferencedType(HaxeReferenceExpression referenceExpression) {
    PsiElementFactory factory = JavaPsiFacade.getInstance(getProject()).getElementFactory();
    return factory.createType(referenceExpression);
  }

  @Override
  @Nullable
  public PsiReferenceList getImplementsList() {
    HaxeInheritList inh = PsiTreeUtil.getChildOfType(this, HaxeInheritList.class);
    return null == inh ? null : PsiTreeUtil.getChildOfType(inh, HaxeImplementsDeclaration.class);
  }

  @Override
  @NotNull
  public PsiClassType[] getImplementsListTypes() {
    final PsiReferenceList implementsList = this.getImplementsList();
    if (implementsList != null) {
      return implementsList.getReferencedTypes();
    }
    return PsiClassType.EMPTY_ARRAY;
  }

  @Override
  public boolean isInheritor(@NotNull PsiClass baseClass, boolean checkDeep) {
    return InheritanceImplUtil.isInheritor(this, baseClass, checkDeep);
  }

  @Override
  public boolean isInheritorDeep(PsiClass baseClass, @Nullable PsiClass classToByPass) {
    return InheritanceImplUtil.isInheritorDeep(this, baseClass, classToByPass);
  }

  @Override
  @NotNull
  public PsiClassInitializer[] getInitializers() {
    // XXX: This may be needed during implementation of refactoring feature
    // Needs change in BNF to detect initializer patterns, load them as accessible constructs in a class object
    // For now, this will be empty
    return PsiClassInitializer.EMPTY_ARRAY;
  }

  @Override
  @NotNull
  public HaxePsiField[] getFields() {
    List<HaxeNamedComponent> haxeFields = getHaxeFieldsSelf(null);
    HaxePsiField[] psiFields = new HaxePsiField[haxeFields.size()];
    return haxeFields.toArray(psiFields);
  }


  @Override
  @NotNull
  public PsiField[] getAllFields() {
      return PsiClassImplUtil.getAllFields(this);
  }

  @Override
  @Nullable
  public PsiField findFieldByName(@NonNls String name, boolean checkBases) {
    return PsiClassImplUtil.findFieldByName(this, name, checkBases);
  }

  @NotNull
  private static PsiMethod[] getMethodsCached(HaxeClass haxeClass) {
    return CachedValuesManager.getCachedValue(haxeClass, () -> {
      final List<HaxeNamedComponent> alltypes = HaxeNamedSubComponentUtil.getNamedSubComponentsFromClassType(haxeClass);
      final List<HaxeNamedComponent> methods = HaxeNamedSubComponentUtil.filterNamedComponentsByType(alltypes, HaxeComponentType.METHOD);
      PsiMethod[] array = methods.toArray(PsiMethod.EMPTY_ARRAY);
      return new CachedValueProvider.Result<>(array, haxeClass);
    });
  }
  @Override
  @NotNull
  public PsiMethod[] getMethods() {
    return getMethodsCached(this);
  }

  @Override
  @NotNull
  public PsiMethod[] getAllMethods() {
    return PsiClassImplUtil.getAllMethods(this);
  }

  @NotNull
  public List<HaxeMethod> getHaxeMethodsAll(HaxeComponentType... excludeTypesFilter) {
    List<HaxeNamedComponent> methods = getAllHaxeNamedComponents(HaxeComponentType.METHOD, excludeTypesFilter);
    final List<HaxeMethod> result = new ArrayList<>();
    for (HaxeNamedComponent method : methods) {
      result.add((HaxeMethod)method);
    }

    return result;
  }
  @NotNull
  public List<HaxeMethod> getHaxeMethodsAncestor(boolean unique) {
    List<HaxeNamedComponent> methods = getAncestorHaxeNamedComponents(HaxeComponentType.METHOD, unique);
    final List<HaxeMethod> result = new ArrayList<>();
    for (HaxeNamedComponent method : methods) {
      result.add((HaxeMethod)method);
    }
    return result;
  }
  @NotNull
  public List<HaxePsiField> getHaxeFieldsAncestor(boolean unique) {
    List<HaxeNamedComponent> fields = getAncestorHaxeNamedComponents(HaxeComponentType.FIELD, unique);
    final List<HaxePsiField> result = new ArrayList<>();
    for (HaxeNamedComponent field : fields) {
      result.add((HaxePsiField)field);
    }
    return result;
  }

  @NotNull
  public List<HaxeNamedComponent> getHaxeFieldAll(HaxeComponentType... excludeTypesFilter) {
    List<HaxeNamedComponent> fields = getAllHaxeNamedComponents(HaxeComponentType.FIELD, excludeTypesFilter);
      return new ArrayList<>(fields);
  }

  @NotNull
  public List<HaxeNamedComponent>getAllHaxeNamedComponents(HaxeComponentType componentType, HaxeComponentType... excludeTypesFilter) {
    final List<HaxeNamedComponent> allNamedComponents = HaxeNamedSubComponentUtil.getAllNamedSubComponentsFromClassType(this, excludeTypesFilter);
    return HaxeNamedSubComponentUtil.filterNamedComponentsByType(allNamedComponents, componentType);
  }

  @NotNull
  public List<HaxeNamedComponent>getAncestorHaxeNamedComponents(HaxeComponentType componentType, boolean unique) {
    List<HaxeClass> supers = new ArrayList<>();
    for (PsiClass superType : this.getSupers()) {
      if (superType instanceof HaxeClass superClass) {
        supers.add(superClass);
      }
    }

    HaxeClass[] supersArray = supers.toArray(HaxeClass.EMPTY_ARRAY);
    List<HaxeNamedComponent> allNamedComponents = HaxeNamedSubComponentUtil.getAllNamedSubComponentsFromClassTypes(supers);
    if(unique) allNamedComponents = HaxeNamedSubComponentUtil.uniqueNamedSubComponents(allNamedComponents);
    return HaxeNamedSubComponentUtil.filterNamedComponentsByType(allNamedComponents, componentType);
  }


  @Override
  @NotNull
  public PsiMethod[] getConstructors() {
    return PsiClassImplUtil.findMethodsByName(this, HaxeTokenTypes.ONEW.toString(), false);
  }

  @Override
  @Nullable
  public PsiMethod findMethodBySignature(final PsiMethod psiMethod, final boolean checkBases) {
    return PsiClassImplUtil.findMethodBySignature(this, psiMethod, checkBases);
  }

  @Override
  @NotNull
  public PsiMethod[] findMethodsByName(@NonNls String name, boolean checkBases) {
    if ("main".equals(name)) { checkBases = false; }
    return PsiClassImplUtil.findMethodsByName(this, name, checkBases);
  }

  @Override
  @NotNull
  public PsiMethod[] findMethodsBySignature(PsiMethod patternMethod, boolean checkBases) {
    return PsiClassImplUtil.findMethodsBySignature(this, patternMethod, checkBases);
  }

  @Override
  @NotNull
  public List<Pair<PsiMethod, PsiSubstitutor>> getAllMethodsAndTheirSubstitutors() {
    return PsiClassImplUtil.getAllWithSubstitutorsByMap(this, PsiClassImplUtil.MemberType.METHOD);
  }

  @Override
  @NotNull
  public List<Pair<PsiMethod, PsiSubstitutor>> findMethodsAndTheirSubstitutorsByName(@NonNls String name, boolean checkBases) {
    return PsiClassImplUtil.findMethodsAndTheirSubstitutorsByName(this, name, checkBases);
  }

  @Override
  public boolean hasTypeParameters() {
    return PsiImplUtil.hasTypeParameters(this);
  }

  @Override
  @Nullable
  public PsiTypeParameterList getTypeParameterList() {
    return new PsiTypeParameterListImpl(this.getNode());
  }

  @Override
  @NotNull
  public PsiTypeParameter[] getTypeParameters() {
    return PsiImplUtil.getTypeParameters(this);
  }

  @Override
  public PsiElement getLBrace() {
    PsiElement body = getBody();
    return findChildByRoleAsPsiElementIn(body, ChildRole.LBRACE);
  }

  @Override
  public PsiElement getRBrace() {
    PsiElement body = getBody();
    return findChildByRoleAsPsiElementIn(body, ChildRole.RBRACE);
  }

  public PsiElement getBody() {
      if (this instanceof HaxeClassDeclaration classDeclaration) {  // concrete class
        return classDeclaration.getClassBody();
      } else if (this instanceof HaxeAbstractTypeDeclaration typeDeclaration) {  // abstract
        return typeDeclaration.getAbstractBody();
      } else if (this instanceof HaxeExternClassDeclaration externClassDeclaration) { // extern class
        return externClassDeclaration.getExternClassDeclarationBody();
      } else if (this instanceof HaxeTypedefDeclaration typedefDeclaration) {  // typedef
        return typedefDeclaration.getTypeOrAnonymous();
      } else if (this instanceof HaxeInterfaceDeclaration interfaceDeclaration) { // interface
        return interfaceDeclaration.getInterfaceBody();
      } else if (this instanceof HaxeEnumDeclaration enumDeclaration) { // enum
        return enumDeclaration.getEnumBody();
      }
    return this;
  }
  private boolean isPrivate() {
    if(_isPrivate == null) {
      HaxePrivateKeyWord privateKeyWord = null;
      if (this instanceof HaxeClassDeclaration) { // concrete class
        privateKeyWord = getPrivateKeyWord(((HaxeClassDeclaration)this).getClassModifierList());
      } else if (this instanceof HaxeAbstractTypeDeclaration) { // abstract class
        privateKeyWord = ((HaxeAbstractTypeDeclaration)this).getPrivateKeyWord();
      } else if (this instanceof HaxeExternClassDeclaration) { // extern class
        privateKeyWord = getPrivateKeyWord(((HaxeExternClassDeclaration)this).getExternClassModifierList());
      } else if (this instanceof HaxeTypedefDeclaration) { // typedef
        privateKeyWord = ((HaxeTypedefDeclaration)this).getPrivateKeyWord();
      } else if (this instanceof HaxeInterfaceDeclaration) { // interface
        privateKeyWord = ((HaxeInterfaceDeclaration)this).getPrivateKeyWord();
      } else if (this instanceof HaxeEnumDeclaration) { // enum
        privateKeyWord = ((HaxeEnumDeclaration)this).getPrivateKeyWord();
      }
      _isPrivate =  (privateKeyWord != null);
    }
    return _isPrivate;
  }

  private HaxePrivateKeyWord getPrivateKeyWord(HaxeClassModifierList list) {
    if (null != list) {
      for (HaxeClassModifier cm: list.getClassModifierList()) {
        HaxePrivateKeyWord pvtKeyword = cm.getPrivateKeyWord();
        if (null != pvtKeyword) {
          return pvtKeyword;
        }
      }
    }
    return null;
  }

  @Override
  public boolean isPublic() {
    return !isPrivate();
  }

  @NotNull
  @Override
  public HaxeModifierList getModifierList() {

    HaxeModifierList list = super.getModifierList();

    if (null == list) {
      list = new HaxeModifierListImpl(this.getNode());
    }

    // -- below modifiers need to be set individually
    //    because, they cannot be enforced through macro-list

    if (isPrivate()) {
      list.setModifierProperty(HaxePsiModifier.PRIVATE, true);
    }

    if (this instanceof HaxeAbstractTypeDeclaration) { // is abstract class
      list.setModifierProperty(HaxePsiModifier.ABSTRACT, true);
    }

    // XXX: Users of HaxeModifierList generally check for the existence of the property, not it's value.
    //      So, don't set it.
    //list.setModifierProperty(HaxePsiModifier.STATIC, false); // Haxe does not have static classes, yet!
    log.assertTrue(!list.hasModifierProperty(HaxePsiModifier.STATIC), "Haxe classes cannot be static.");

    return list;
  }

  @Override
  public boolean hasModifierProperty(@HaxePsiModifier.ModifierConstant @NonNls @NotNull String name) {
    return this.getModifierList().hasModifierProperty(name);
  }

  @Override
  @Nullable
  public PsiDocComment getDocComment() {
    PsiComment psiComment = HaxeResolveUtil.findDocumentation(this);
    return (psiComment != null) ? new HaxePsiDocComment(this, psiComment) : null;
  }

  @Override
  @NotNull
  public PsiElement getNavigationElement() {
    return this;
  }

  @Override
  @Nullable
  public PsiIdentifier getNameIdentifier() {
    // For a HaxeClass, the identifier is three children below.  The first is
    // the component name, then a reference, and finally the identifier.
    HaxeComponentName name = PsiTreeUtil.getChildOfType(this, HaxeComponentName.class);
    return null == name ? null : name.getIdentifier();
  }

  @Override
  @NotNull
  public Collection<HierarchicalMethodSignature> getVisibleSignatures() {
    return PsiSuperMethodImplUtil.getVisibleSignatures(this);
  }

  @Override
  @NotNull
  public PsiClass[] getInnerClasses() {
    return PsiClass.EMPTY_ARRAY;
  }

  @Override
  @NotNull
  public PsiClass[] getAllInnerClasses() {
    return PsiClass.EMPTY_ARRAY;
  }

  @Override
  @Nullable
  public PsiClass findInnerClassByName(@NonNls String name, boolean checkBases) {
    return null;
  }


  @NotNull
  public static AbstractHaxePsiClass createEmptyFacade(Project project) {
    return new AbstractHaxePsiClass(new HaxeDummyASTNode("EMPTY_FACADE", project)) {
      @Nullable
      @Override
      public HaxeGenericParam getGenericParam() {
        return null;
      }

      @Nullable
      @Override
      public HaxeComponentName getComponentName() {
        return null;
      }

      @NotNull
      @Override
      public Project getProject() {
        return ((HaxeDummyASTNode)getNode()).getProject();
      }
    };
  }

  @Override
  public void delete() {
    // FIX: for twice deletion of file in project view (issue #424)
    final HaxeFile file = (HaxeFile)getContainingFile();
    super.delete();
    if (file != null && file.getClasses().length == 0) {
      file.delete();
    }
  }
}
