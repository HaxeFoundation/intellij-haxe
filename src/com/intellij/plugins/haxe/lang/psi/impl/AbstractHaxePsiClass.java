/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
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
package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.*;
import com.intellij.psi.impl.source.tree.ChildRole;
import com.intellij.psi.impl.source.tree.java.PsiTypeParameterListImpl;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public abstract class AbstractHaxePsiClass extends AbstractHaxeNamedComponent implements HaxeClass {

  private static final Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxePsiClass");
  {
    LOG.info("Loaded AbstractHaxePsiClass");
    LOG.setLevel(Level.DEBUG);
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
    final String name = getName();
    if (getParent() == null) {
      return name == null ? "" : name;
    }
    final String fileName = FileUtil.getNameWithoutExtension(getContainingFile().getName());
    String packageName = HaxeResolveUtil.getPackageName(getContainingFile());
    if (isAncillaryClass(name, fileName)) {
      packageName = HaxeResolveUtil.joinQName(packageName, fileName);
    }
    return HaxeResolveUtil.joinQName(packageName, name);
  }

  private boolean isAncillaryClass(String name, String fileName) {
    return (!(this instanceof  HaxeExternClassDeclaration)) &&
           (!fileName.equals(name)) &&
           (HaxeResolveUtil.findComponentDeclaration(getContainingFile(), fileName) != null);
  }

  @Override
  public boolean isInterface() {
    // LOG.debug("\n>>>\tisInterface();");
    return HaxeComponentType.typeOf(this) == HaxeComponentType.INTERFACE;
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

  @NotNull
  @Override
  public List<HaxeMethod> getHaxeMethods() {
    final List<HaxeNamedComponent> alltypes = HaxeResolveUtil.findNamedSubComponents(this);
    final List<HaxeNamedComponent> methods = HaxeResolveUtil.filterNamedComponentsByType(alltypes, HaxeComponentType.METHOD);
    final List<HaxeMethod> result = new ArrayList<HaxeMethod>();
    for ( HaxeNamedComponent method : methods ) {
      result.add((HaxeMethod)method);
    }
    return result;
  }

  @NotNull
  @Override
  public List<HaxeNamedComponent> getHaxeFields() {
    final List<HaxeNamedComponent> result = HaxeResolveUtil.findNamedSubComponents(this);
    return HaxeResolveUtil.filterNamedComponentsByType(result, HaxeComponentType.FIELD);
  }

  @NotNull
  @Override
  public List<HaxeVarDeclaration> getVarDeclarations() {
    // LOG.debug("\n>>>\tgetVarDeclarations();");
    return HaxeResolveUtil.getClassVarDeclarations(this);
  }

  @Nullable
  @Override
  public HaxeNamedComponent findHaxeFieldByName(@NotNull final String name) {
    // LOG.debug("\n>>>\tfindHaxeFieldByName( >>> " + name + " <<< );");
    return ContainerUtil.find(getHaxeFields(), new Condition<HaxeNamedComponent>() {
      @Override
      public boolean value(HaxeNamedComponent component) {
        return name.equals(component.getName());
      }
    });
  }

  @Override
  public HaxeNamedComponent findHaxeMethodByName(@NotNull final String name) {
    // LOG.debug("\n>>>\tfindHaxeMethodByName( >>> " + name + " <<< );");
    return ContainerUtil.find(getHaxeMethods(), new Condition<HaxeNamedComponent>() {
      @Override
      public boolean value(HaxeNamedComponent component) {
        return name.equals(component.getName());
      }
    });
  }

  @Override
  public boolean isGeneric() {
    // LOG.debug("\n>>>\tisGeneric();");
    return getGenericParam() != null;
  }

  @Override
  public boolean isEnum() {
    // LOG.debug("\n>>>\tisEnum();");
    return (HaxeComponentType.typeOf(this) == HaxeComponentType.ENUM);
  }

  @Override
  public boolean isAnnotationType() {
    // LOG.debug("\n>>>\tisAnnotationType();");
    /* both: annotation & typedef in haxe are treated as typedef! */
    return (HaxeComponentType.typeOf(this) == HaxeComponentType.TYPEDEF);
  }

  @Override
  public boolean isDeprecated() {
    // LOG.debug("\n>>>\tisDeprecated();");
    /* not applicable to Haxe language */
    return false;
  }

  @Override
  @NotNull
  public PsiClass[] getSupers() {
    // LOG.debug("\n>>>\tgetSupers();");
    // Extends and Implements in one list
    return PsiClassImplUtil.getSupers(this);
  }

  @Override
  public PsiClass getSuperClass() {
    // LOG.debug("\n>>>\tgetSuperClass();");
    return PsiClassImplUtil.getSuperClass(this);
  }

  @Override
  @NotNull
  public PsiClassType[] getSuperTypes() {
    // LOG.debug("\n>>>\tgetSuperTypes();");
    return PsiClassImplUtil.getSuperTypes(this);
  }

  @Override
  public PsiElement getScope() {
    // LOG.debug("\n>>>\tgetScope();");
    return getParent();
  }

  @Override
  public PsiClass getContainingClass() {
    // LOG.debug("\n>>>\tgetContainingClass();");
    PsiElement parent = getParent();
    return (parent instanceof PsiClass ? (PsiClass)parent : null);
  }

  @Override
  public PsiClass[] getInterfaces() {  // Extends and Implements in one list
    // LOG.debug("\n>>>\tgetInterfaces();");
    return PsiClassImplUtil.getInterfaces(this);
  }

  @Override
  @Nullable
  public PsiReferenceList getExtendsList() {
    // LOG.debug("\n>>>\tgetExtendsList();");
    final List<HaxeType> haxeTypeList = getHaxeExtendsList();
    HaxePsiReferenceList psiReferenceList = new HaxePsiReferenceList(this, getNode(), PsiReferenceList.Role.EXTENDS_LIST);
    for (HaxeType haxeTypeElement : haxeTypeList) {
      psiReferenceList.addReferenceElements(haxeTypeElement.getReferenceExpression().getIdentifier().getChildren());
    }
    return psiReferenceList;
  }

  @Override
  @NotNull
  public PsiClassType[] getExtendsListTypes() {
    // LOG.debug("\n>>>\tgetExtendsListTypes();");
    final PsiReferenceList extendsList = this.getExtendsList();
    if (extendsList != null) {
      return extendsList.getReferencedTypes();
    }
    return PsiClassType.EMPTY_ARRAY;
  }

  @Override
  @Nullable
  public PsiReferenceList getImplementsList() {
    // LOG.debug("\n>>>\tgetImplementsList();");
    final List<HaxeType> haxeTypeList = getHaxeImplementsList();
    HaxePsiReferenceList psiReferenceList = new HaxePsiReferenceList(this, getNode(), PsiReferenceList.Role.IMPLEMENTS_LIST);
    for (HaxeType haxeTypeElement : haxeTypeList) {
      psiReferenceList.addReferenceElements(haxeTypeElement.getReferenceExpression().getIdentifier().getChildren());
    }
    return psiReferenceList;
  }

  @Override
  @NotNull
  public PsiClassType[] getImplementsListTypes() {
    // LOG.debug("\n>>>\tgetImplementsListTypes();");
    final PsiReferenceList implementsList = this.getImplementsList();
    if (implementsList != null) {
      return implementsList.getReferencedTypes();
    }
    return PsiClassType.EMPTY_ARRAY;
  }

  @Override
  public boolean isInheritor(@NotNull PsiClass baseClass, boolean checkDeep) {
    // LOG.debug("\n>>>\tisInheritor( " + baseClass.getQualifiedName()  + " );");
    return InheritanceImplUtil.isInheritor(this, baseClass, checkDeep);
  }

  @Override
  public boolean isInheritorDeep(PsiClass baseClass, @Nullable PsiClass classToByPass) {
    // LOG.debug("\n>>>\tisInheritorDeep( " + baseClass.getQualifiedName()  + " );");
    return InheritanceImplUtil.isInheritorDeep(this, baseClass, classToByPass);
  }

  @Override
  @NotNull
  public PsiClassInitializer[] getInitializers() {
    // LOG.debug("\n>>>\tgetInitializers();");
    // XXX: This may be needed during implementation of refactoring feature
    // Needs change in BNF to detect initializer patterns, load them as accessible constructs in a class object
    // For now, this will be empty
    return PsiClassInitializer.EMPTY_ARRAY;
  }

  @Override
  @NotNull
  public PsiField[] getFields() {
    // LOG.debug("\n>>>\tgetFields();");
    List<HaxeNamedComponent> haxeFields = getHaxeFields();
    int index = 0;
    HaxePsiField[] psiFields = new HaxePsiField[haxeFields.size()];
    for (HaxeNamedComponent element : haxeFields) {
      psiFields[index++] = new HaxePsiField(element);
    }
    return psiFields;
  }

  @Override
  @NotNull
  public PsiField[] getAllFields() {
    // LOG.debug("\n>>>\tgetAllFields();");
    return PsiClassImplUtil.getAllFields(this);
  }

  @Override
  @Nullable
  public PsiField findFieldByName(@NonNls String name, boolean checkBases) {
    // LOG.debug("\n>>>\tfindFieldByName( >>> " + name + " <<< );");
    return PsiClassImplUtil.findFieldByName(this, name, checkBases);
  }

  @Override
  @NotNull
  public PsiMethod[] getMethods() {
    // TODO: fix
    List<HaxeMethod> haxeMethods = getHaxeMethods();
    LOG.debug("\n>>>\tgetHaxeMethods() : >>> " + haxeMethods.size());
    PsiMethod[] returntype = new PsiMethod[haxeMethods.size()];
    return haxeMethods.toArray(returntype);
    //PsiMethod[] returnValue = {};
    //return returnValue;
  }

  @Override
  @NotNull
  public PsiMethod[] getAllMethods() {
    // LOG.debug("\n>>>\tgetAllMethods();");
    return PsiClassImplUtil.getAllMethods(this);
  }

  @Override
  @NotNull
  public PsiMethod[] getConstructors() {
    return PsiClassImplUtil.findMethodsByName(this, "new", false);
  }

  @Override
  @Nullable
  public PsiMethod findMethodBySignature(final PsiMethod psiMethod, final boolean checkBases) {
    // LOG.debug("\n>>>\tfindMethodBySignature( " + psiMethod.getName()  + " );");
    return PsiClassImplUtil.findMethodBySignature(this, psiMethod, checkBases);
  }

  @Override
  @NotNull
  public PsiMethod[] findMethodsByName(@NonNls String name, boolean checkBases) {
    LOG.debug(">>>\tfindMethodsByName( " + name + " );");
    return PsiClassImplUtil.findMethodsByName(this, name, checkBases);
  }

  @Override
  @NotNull
  public PsiMethod[] findMethodsBySignature(PsiMethod patternMethod, boolean checkBases) {
    // LOG.debug("\n>>>\tfindMethodsBySignature( " + patternMethod.getName()  + " );");
    return PsiClassImplUtil.findMethodsBySignature(this, patternMethod, checkBases);
  }

  @Override
  @NotNull
  public List<Pair<PsiMethod, PsiSubstitutor>> getAllMethodsAndTheirSubstitutors() {
    // LOG.debug("\n>>>\tgetAllMethodsAndTheirSubstitutors();");
    return PsiClassImplUtil.getAllWithSubstitutorsByMap(this, PsiClassImplUtil.MemberType.METHOD);
  }

  @Override
  @NotNull
  public List<Pair<PsiMethod, PsiSubstitutor>> findMethodsAndTheirSubstitutorsByName(@NonNls String name, boolean checkBases) {
    // LOG.debug("\n>>>\tfindMethodsAndTheirSubstitutorsByName();");
    return PsiClassImplUtil.findMethodsAndTheirSubstitutorsByName(this, name, checkBases);
  }

  @Override
  public boolean hasTypeParameters() {
    // LOG.debug("\n>>>\thasTypeParameters();");
    return PsiImplUtil.hasTypeParameters(this);
  }

  @Override
  @Nullable
  public PsiTypeParameterList getTypeParameterList() {
    // LOG.debug("\n>>>\tgetTypeParameterList();");
    return new PsiTypeParameterListImpl(this.getNode());
  }

  @Override
  @NotNull
  public PsiTypeParameter[] getTypeParameters() {
    // LOG.debug("\n>>>\tgetTypeParameters();");
    return PsiImplUtil.getTypeParameters(this);
  }

  @Override
  public PsiElement getLBrace() {
    // LOG.debug("\n>>>\tgetLBrace();");
    return findChildByRoleAsPsiElement(ChildRole.LBRACE);
  }

  @Override
  public PsiElement getRBrace() {
    // LOG.debug("\n>>>\tgetRBrace();");
    return findChildByRoleAsPsiElement(ChildRole.RBRACE);
  }

  private boolean isPrivate() {
    // LOG.debug("\n>>>\tisPrivate();");

    HaxePrivateKeyWord privateKeyWord = null;

    if (this instanceof HaxeClassDeclaration) { // concrete class
      privateKeyWord = ((HaxeClassDeclaration) this).getPrivateKeyWord();
    }
    else if (this instanceof HaxeAbstractClassDeclaration) { // abstract class
      privateKeyWord = ((HaxeAbstractClassDeclaration) this).getPrivateKeyWord();
    }
    else if (this instanceof HaxeExternClassDeclaration) { // extern class
      List<HaxeExternOrPrivate> externOrPrivateList = ((HaxeExternClassDeclaration) this).getExternOrPrivateList();
      for (HaxeExternOrPrivate externOrPrivate : externOrPrivateList) {
        if (externOrPrivate.getPrivateKeyWord() != null) {
          privateKeyWord = externOrPrivate.getPrivateKeyWord();
          break; // XXX: does this need further searching / refining?
        }
      }
    }
    else {
      HaxeExternOrPrivate externOrPrivate = null;

      if (this instanceof HaxeTypedefDeclaration) { // typedef
        externOrPrivate = ((HaxeTypedefDeclaration) this).getExternOrPrivate();
      }
      else if (this instanceof HaxeInterfaceDeclaration) { // interface
        externOrPrivate = ((HaxeInterfaceDeclaration) this).getExternOrPrivate();
      }
      else if (this instanceof HaxeEnumDeclaration) { // enum
        externOrPrivate = ((HaxeEnumDeclaration) this).getExternOrPrivate();
      }

      if (externOrPrivate != null) {
        privateKeyWord = externOrPrivate.getPrivateKeyWord();
      }
    }

    return (privateKeyWord != null);
  }

  @Override
  public boolean isPublic() {
    // LOG.debug("\n>>>\tisPublic();");
    return (!isPrivate() && super.isPublic()); // do not change the order of- and the- expressions
  }

  @NotNull
  @Override
  public HaxeModifierList getModifierList() {
    // LOG.debug("\n>>>\tgetModifierList();");

    HaxeModifierList list = super.getModifierList();

    if (null == list) {
      list = new HaxeModifierListImpl(this.getNode());
    }

    // -- below modifiers need to be set individually
    //    because, they cannot be enforced through macro-list

    if (isPrivate()) {
      list.setModifierProperty(HaxePsiModifier.PRIVATE, true);
    }

    if (this instanceof HaxeAbstractClassDeclaration) { // is abstract class
      list.setModifierProperty(HaxePsiModifier.ABSTRACT, true);
    }

    list.setModifierProperty(HaxePsiModifier.STATIC, false); // Haxe does not have static classes, yet!

    return list;
  }

  @Override
  public boolean hasModifierProperty(@HaxePsiModifier.ModifierConstant @NonNls @NotNull String name) {
    boolean result = this.getModifierList().hasModifierProperty(name);
    // LOG.debug("\n>>>\thasModifierProperty( " + name + " ) = " + result);
    return result;
  }

  @Override
  @Nullable
  public PsiDocComment getDocComment() {
    //PsiComment psiComment = HaxeResolveUtil.findDocumentation(this);
    //// LOG.debug("\n>>>\tgetDocComment() : >>> : " + ((psiComment!=null)?psiComment.getText():" NULL "));
    //return ((psiComment != null)? new HaxePsiDocComment(this, psiComment) : null);
    return null;
  }

  @Override
  @NotNull
  public PsiElement getNavigationElement() {
    // LOG.debug("\n>>>\tgetNavigationElement();");
    return this;
  }

  @Override
  @Nullable
  public PsiIdentifier getNameIdentifier() {
    LOG.debug("\n>>>\tgetDocComment() : >>> : " + ((PsiIdentifier) findChildByRoleAsPsiElement(ChildRole.NAME)));
    return ((PsiIdentifier) findChildByRoleAsPsiElement(ChildRole.NAME));
  }

  @Override
  @NotNull
  public Collection<HierarchicalMethodSignature> getVisibleSignatures() {
    // TODO: fix
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
}
