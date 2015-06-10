/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 TiVo Inc.
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
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.InheritanceImplUtil;
import com.intellij.psi.impl.PsiClassImplUtil;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.impl.PsiSuperMethodImplUtil;
import com.intellij.psi.impl.source.tree.ChildRole;
import com.intellij.psi.impl.source.tree.java.PsiTypeParameterListImpl;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
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

  static {
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

  private HaxeClassModel _model = null;
  public HaxeClassModel getModel() {
    if (_model == null) _model = new HaxeClassModel(this);
    return _model;
  }

  private boolean isAncillaryClass(String name, String fileName) {
    return (!(this instanceof  HaxeExternClassDeclaration)) &&
           (!fileName.equals(name)) &&
           (HaxeResolveUtil.findComponentDeclaration(getContainingFile(), fileName) != null);
  }

  @Override
  public boolean isExtern() {
    return (this instanceof HaxeExternClassDeclaration || this instanceof HaxeExternInterfaceDeclaration);
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

  @Override
   public PsiElement add(@NotNull PsiElement element) throws IncorrectOperationException {
    return super.add(element);
  }

  @NotNull
  @Override
  public List<HaxeMethod> getHaxeMethods() {
    // XXX: This implementation is equivalent to getAllMethods().  That
    //      may not be what we want.
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
    return HaxeResolveUtil.getClassVarDeclarations(this);
  }

  @Nullable
  @Override
  public HaxeNamedComponent findHaxeFieldByName(@NotNull final String name) {
    return ContainerUtil.find(getHaxeFields(), new Condition<HaxeNamedComponent>() {
      @Override
      public boolean value(HaxeNamedComponent component) {
        return name.equals(component.getName());
      }
    });
  }

  @Override
  public HaxeNamedComponent findHaxeMethodByName(@NotNull final String name) {
    return ContainerUtil.find(getHaxeMethods(), new Condition<HaxeNamedComponent>() {
      @Override
      public boolean value(HaxeNamedComponent component) {
        return name.equals(component.getName());
      }
    });
  }

  @Override
  public boolean isGeneric() {
    return getGenericParam() != null;
  }

  @Override
  public boolean isEnum() {
    return (HaxeComponentType.typeOf(this) == HaxeComponentType.ENUM);
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
    // LOG.debug("\n>>>\tgetExtendsList();");
    HaxeInheritList inh = PsiTreeUtil.getChildOfType(this, HaxeInheritList.class);
    return null == inh ? null : PsiTreeUtil.getChildOfType(inh, HaxeExtendsDeclaration.class);
  }

  @Override
  @NotNull
  public PsiClassType[] getExtendsListTypes() {
    final PsiReferenceList extendsList = this.getExtendsList();
    if (extendsList != null) {
      return extendsList.getReferencedTypes();
    }
    return PsiClassType.EMPTY_ARRAY;
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
    List<HaxeNamedComponent> haxeFields = getHaxeFields();
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

  @Override
  @NotNull
  public PsiMethod[] getMethods() {
    final List<HaxeNamedComponent> alltypes = HaxeResolveUtil.getNamedSubComponents(this);
    final List<HaxeNamedComponent> methods = HaxeResolveUtil.filterNamedComponentsByType(alltypes, HaxeComponentType.METHOD);
    return methods.toArray(PsiMethod.EMPTY_ARRAY); // size is irrelevant
  }

  @Override
  @NotNull
  public PsiMethod[] getAllMethods() {
    return PsiClassImplUtil.getAllMethods(this);
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
    return findChildByRoleAsPsiElement(ChildRole.LBRACE);
  }

  @Override
   public PsiElement getRBrace() {
    return findChildByRoleAsPsiElement(ChildRole.RBRACE);
  }

  private boolean isPrivate() {
    HaxePrivateKeyWord privateKeyWord = null;
    if (this instanceof HaxeClassDeclaration) { // concrete class
      privateKeyWord = ((HaxeClassDeclaration) this).getPrivateKeyWord();
    }
    else if (this instanceof HaxeAbstractClassDeclaration) { // abstract class
      privateKeyWord = ((HaxeAbstractClassDeclaration) this).getPrivateKeyWord();
    }
    else if (this instanceof HaxeExternClassDeclaration) { // extern class
      privateKeyWord = ((HaxeExternClassDeclaration)this).getPrivateKeyWord();
    }
    else if (this instanceof HaxeTypedefDeclaration) { // typedef
      privateKeyWord = ((HaxeTypedefDeclaration) this).getPrivateKeyWord();
    }
    else if (this instanceof HaxeInterfaceDeclaration) { // interface
      privateKeyWord = ((HaxeInterfaceDeclaration) this).getPrivateKeyWord();
    }
    else if (this instanceof HaxeEnumDeclaration) { // enum
      privateKeyWord = ((HaxeEnumDeclaration) this).getPrivateKeyWord();
    }
    return (privateKeyWord != null);
  }

  @Override
  public boolean isPublic() {
    return (!isPrivate() && super.isPublic()); // do not change the order of- and the- expressions
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

    if (this instanceof HaxeAbstractClassDeclaration) { // is abstract class
      list.setModifierProperty(HaxePsiModifier.ABSTRACT, true);
    }

    // XXX: Users of HaxeModifierList generally check for the existence of the property, not it's value.
    //      So, don't set it.
    //list.setModifierProperty(HaxePsiModifier.STATIC, false); // Haxe does not have static classes, yet!
    LOG.assertTrue(!list.hasModifierProperty(HaxePsiModifier.STATIC), "Haxe classes cannot be static.");

    return list;
  }

  @Override
  public boolean hasModifierProperty(@HaxePsiModifier.ModifierConstant @NonNls @NotNull String name) {
    return this.getModifierList().hasModifierProperty(name);
  }

  @Override
  @Nullable
  public PsiDocComment getDocComment() {
    // TODO: Fix 'public PsiDocComment getDocComment()'
    //PsiComment psiComment = HaxeResolveUtil.findDocumentation(this);
    //return ((psiComment != null)? new HaxePsiDocComment(this, psiComment) : null);
    return null;
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


  public static AbstractHaxePsiClass EMPTY_FACADE = new AbstractHaxePsiClass(new HaxeDummyASTNode("EMPTY_FACADE")) {
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
  };

}
