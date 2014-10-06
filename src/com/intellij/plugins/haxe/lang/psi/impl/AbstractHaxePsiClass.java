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
    System.out.println("\n>>>\tgetVarDeclarations();");
    return HaxeResolveUtil.getClassVarDeclarations(this);
  }

  @Nullable
  @Override
  public HaxeNamedComponent findHaxeFieldByName(@NotNull final String name) {
    System.out.println("\n>>>\tfindHaxeFieldByName( >>> " + name + " <<< );");
    return ContainerUtil.find(getHaxeFields(), new Condition<HaxeNamedComponent>() {
      @Override
      public boolean value(HaxeNamedComponent component) {
        return name.equals(component.getName());
      }
    });
  }

  @Override
  public HaxeNamedComponent findHaxeMethodByName(@NotNull final String name) {
    System.out.println("\n>>>\tfindHaxeMethodByName( >>> " + name + " <<< );");
    return ContainerUtil.find(getHaxeMethods(), new Condition<HaxeNamedComponent>() {
      @Override
      public boolean value(HaxeNamedComponent component) {
        return name.equals(component.getName());
      }
    });
  }

  @Override
  public boolean isGeneric() {
    System.out.println("\n>>>\tisGeneric();");
    return getGenericParam() != null;
  }

  @Override
  public boolean isEnum() {
    System.out.println("\n>>>\tisEnum();");
    return (HaxeComponentType.typeOf(this) == HaxeComponentType.ENUM);
  }

  @Override
  public boolean isAnnotationType() {
    System.out.println("\n>>>\tisAnnotationType();");
    /* both: annotation & typedef in haxe are treated as typedef! */
    return (HaxeComponentType.typeOf(this) == HaxeComponentType.TYPEDEF);
  }

  @Override
  public boolean isDeprecated() {
    System.out.println("\n>>>\tisDeprecated();");
    /* not applicable to Haxe language */
    return false;
  }

  @Override
  @NotNull
  public PsiClass[] getSupers() {
    System.out.println("\n>>>\tgetSupers();");
    // Extends and Implements in one list
    return PsiClassImplUtil.getSupers(this);
  }

  @Override
  public PsiClass getSuperClass() {
    System.out.println("\n>>>\tgetSuperClass();");
    return PsiClassImplUtil.getSuperClass(this);
  }

  @Override
  @NotNull
  public PsiClassType[] getSuperTypes() {
    System.out.println("\n>>>\tgetSuperTypes();");
    return PsiClassImplUtil.getSuperTypes(this);
  }

  @Override
  public PsiElement getScope() {
    System.out.println("\n>>>\tgetScope();");
    return getParent();
  }

  @Override
  public PsiClass getContainingClass() {
    System.out.println("\n>>>\tgetContainingClass();");
    PsiElement parent = getParent();
    return (parent instanceof PsiClass ? (PsiClass)parent : null);
  }

  @Override
  public PsiClass[] getInterfaces() {  // Extends and Implements in one list
    System.out.println("\n>>>\tgetInterfaces();");
    return PsiClassImplUtil.getInterfaces(this);
  }

  @Override
  @Nullable
  public PsiReferenceList getExtendsList() {
    System.out.println("\n>>>\tgetExtendsList();");
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
    System.out.println("\n>>>\tgetExtendsListTypes();");
    final PsiReferenceList extendsList = this.getExtendsList();
    if (extendsList != null) {
      return extendsList.getReferencedTypes();
    }
    return PsiClassType.EMPTY_ARRAY;
  }

  @Override
  @Nullable
  public PsiReferenceList getImplementsList() {
    System.out.println("\n>>>\tgetImplementsList();");
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
    System.out.println("\n>>>\tgetImplementsListTypes();");
    final PsiReferenceList implementsList = this.getImplementsList();
    if (implementsList != null) {
      return implementsList.getReferencedTypes();
    }
    return PsiClassType.EMPTY_ARRAY;
  }

  @Override
  public boolean isInheritor(@NotNull PsiClass baseClass, boolean checkDeep) {
    System.out.println("\n>>>\tisInheritor();");
    return InheritanceImplUtil.isInheritor(this, baseClass, checkDeep);
  }

  @Override
  public boolean isInheritorDeep(PsiClass baseClass, @Nullable PsiClass classToByPass) {
    System.out.println("\n>>>\tisInheritorDeep();");
    return InheritanceImplUtil.isInheritorDeep(this, baseClass, classToByPass);
  }

  @Override
  @NotNull
  public PsiClassInitializer[] getInitializers() {
    System.out.println("\n>>>\tgetInitializers();");
    // XXX: This may be needed during implementation of refactoring feature
    // Needs change in BNF to detect initializer patterns, load them as accessible constructs in a class object
    // For now, this will be empty
    return PsiClassInitializer.EMPTY_ARRAY;
  }

  @Override
  @NotNull
  public PsiField[] getFields() {
    System.out.println("\n>>>\tgetFields();");
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
    System.out.println("\n>>>\tgetAllFields();");
    return PsiClassImplUtil.getAllFields(this);
  }

  @Override
  @Nullable
  public PsiField findFieldByName(@NonNls String name, boolean checkBases) {
    System.out.println("\n>>>\tfindFieldByName( >>> " + name + " <<< );");
    return PsiClassImplUtil.findFieldByName(this, name, checkBases);
  }

  @Override
  @NotNull
  public PsiMethod[] getMethods() {
    System.out.println("\n>>>\tgetMethods();");
    //// TODO: broken after change 49d2848 ... EB's fixes are needed
    //List<HaxeMethod> haxeMethods = getHaxeMethods();
    //PsiMethod[] returntype = new PsiMethod[haxeMethods.size()];
    //return haxeMethods.toArray(returntype);
    PsiMethod[] returnValue = {};
    return returnValue;
  }

  @Override
  @NotNull
  public PsiMethod[] getAllMethods() {
    System.out.println("\n>>>\tgetAllMethods();");
    return PsiClassImplUtil.getAllMethods(this);
  }

  @Override
  @NotNull
  public PsiMethod[] getConstructors() {
    System.out.println("\n>>>\tgetConstructors();");
    return PsiClassImplUtil.findMethodsByName(this, "new", false);
  }

  @Override
  @Nullable
  public PsiMethod findMethodBySignature(final PsiMethod psiMethod, final boolean checkBases) {
    System.out.println("\n>>>\tfindMethodBySignature();");
    return PsiClassImplUtil.findMethodBySignature(this, psiMethod, checkBases);
  }

  @Override
  @NotNull
  public PsiMethod[] findMethodsByName(@NonNls String name, boolean checkBases) {
    System.out.println("\n>>>\tfindMethodsByName();");
    return PsiClassImplUtil.findMethodsByName(this, name, checkBases);
  }

  @Override
  @NotNull
  public PsiMethod[] findMethodsBySignature(PsiMethod patternMethod, boolean checkBases) {
    System.out.println("\n>>>\tfindMethodsBySignature();");
    return PsiClassImplUtil.findMethodsBySignature(this, patternMethod, checkBases);
  }

  @Override
  @NotNull
  public List<Pair<PsiMethod, PsiSubstitutor>> getAllMethodsAndTheirSubstitutors() {
    System.out.println("\n>>>\tgetAllMethodsAndTheirSubstitutors();");
    return PsiClassImplUtil.getAllWithSubstitutorsByMap(this, PsiClassImplUtil.MemberType.METHOD);
  }

  @Override
  @NotNull
  public List<Pair<PsiMethod, PsiSubstitutor>> findMethodsAndTheirSubstitutorsByName(@NonNls String name, boolean checkBases) {
    System.out.println("\n>>>\tfindMethodsAndTheirSubstitutorsByName();");
    return PsiClassImplUtil.findMethodsAndTheirSubstitutorsByName(this, name, checkBases);
  }

  @Override
  public boolean hasTypeParameters() {
    System.out.println("\n>>>\thasTypeParameters();");
    return PsiImplUtil.hasTypeParameters(this);
  }

  @Override
  @Nullable
  public PsiTypeParameterList getTypeParameterList() {
    System.out.println("\n>>>\tgetTypeParameterList();");
    return new PsiTypeParameterListImpl(this.getNode());
  }

  @Override
  @NotNull
  public PsiTypeParameter[] getTypeParameters() {
    System.out.println("\n>>>\tgetTypeParameters();");
    return PsiImplUtil.getTypeParameters(this);
  }

  @Override
  public PsiElement getLBrace() {
    System.out.println("\n>>>\tgetLBrace();");
    return findChildByRoleAsPsiElement(ChildRole.LBRACE);
  }

  @Override
  public PsiElement getRBrace() {
    System.out.println("\n>>>\tgetRBrace();");
    return findChildByRoleAsPsiElement(ChildRole.RBRACE);
  }

  private boolean isPrivate() {
    System.out.println("\n>>>\tisPrivate();");

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
    System.out.println("\n>>>\tisPublic();");
    return (!isPrivate() && super.isPublic()); // do not change the order of- and the- expressions
  }

  @NotNull
  @Override
  public HaxeModifierList getModifierList() {
    System.out.println("\n>>>\tgetModifierList();");

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
    System.out.println("\n>>>\thasModifierProperty();");
    return this.getModifierList().hasModifierProperty(name);
  }

  @Override
  @Nullable
  public PsiDocComment getDocComment() {
    System.out.println("\n>>>\tgetDocComment();");
    PsiComment psiComment = HaxeResolveUtil.findDocumentation(this);
    return ((psiComment != null)? new HaxePsiDocComment(this, psiComment) : null);
  }

  @Override
  @NotNull
  public PsiElement getNavigationElement() {
    System.out.println("\n>>>\tgetNavigationElement();");
    return this;
  }

  @Override
  @Nullable
  public PsiIdentifier getNameIdentifier() {
    System.out.println("\n>>>\tgetNameIdentifier();");
    return ((PsiIdentifier) findChildByRoleAsPsiElement(ChildRole.NAME));
  }

  @Override
  @NotNull
  public Collection<HierarchicalMethodSignature> getVisibleSignatures() {
    // TODO: broken after change 49d2848 ... EB's fixes are needed
    System.out.println("\n>>>\tBEFORE:\tPsiSuperMethodImplUtil.getVisibleSignatures(this);");
    Collection<HierarchicalMethodSignature> list = PsiSuperMethodImplUtil.getVisibleSignatures(this);
    System.out.println(">>>\tRESULT:\t"+ list.size());
    System.out.println(">>>\tAFTER:\tPsiSuperMethodImplUtil.getVisibleSignatures(this);\n");
    return list;
    //return PsiSuperMethodImplUtil.getVisibleSignatures(this);
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
