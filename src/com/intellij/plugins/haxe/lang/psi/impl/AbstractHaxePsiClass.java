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
    return getParent();
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
    final PsiReferenceList extendsList = this.getExtendsList();
    if (extendsList != null) {
      return extendsList.getReferencedTypes();
    }
    return PsiClassType.EMPTY_ARRAY;
  }

  @Override
  @Nullable
  public PsiReferenceList getImplementsList() {
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
  public PsiField[] getFields() {
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
    // TODO: broken after change 49d2848 ... EB's fixes are needed
    List<HaxeMethod> haxeMethods = getHaxeMethods();
    PsiMethod[] returntype = new PsiMethod[haxeMethods.size()];
    return haxeMethods.toArray(returntype);
  }

  @Override
  @NotNull
  public PsiMethod[] getAllMethods() {
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
    return PsiClassImplUtil.findMethodBySignature(this, psiMethod, checkBases);
  }

  @Override
  @NotNull
  public PsiMethod[] findMethodsByName(@NonNls String name, boolean checkBases) {
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
    return (!isPrivate() && super.isPublic()); // do not change the order of- and the- expressions
  }

  @NotNull
  @Override
  public HaxeModifierList getModifierList() {


    if (true) {
      // Now that the modifier list is parsed directly into the tree, we only
      // have to find it, not create it on the fly.

      HaxeModifierList list = PsiTreeUtil.findChildOfType(this, HaxeMacroClassList.class);
      if (null == list) {
        list = new HaxeModifierListImpl(this.getNode());
      }
      return list;
    }


    HaxeModifierList haxePsiModifierList = new HaxeModifierListImpl(this.getNode());

    if (this instanceof HaxeAbstractClassDeclaration) { // is abstract class
      haxePsiModifierList.setModifierProperty(HaxePsiModifier.ABSTRACT, true);
    }

    if (isPrivate()) {
      haxePsiModifierList.setModifierProperty(HaxePsiModifier.PRIVATE, true);
    }

    // whether this class has public access is anyway provided by isPublic()
    // do not set PUBLIC modifier (as Haxe classes are public, by default)

    haxePsiModifierList.setModifierProperty(HaxePsiModifier.STATIC, false); // Haxe does not have static classes, yet!

    HaxeMacroClass macroClass = (HaxeMacroClass) UsefulPsiTreeUtil.getChildOfType(this, HaxeTokenTypes.MACRO_CLASS);
    // COMPLETED:
    //  -->  BNF changes required to fetch annotations/access-modifiers from class declarations.
    //  -->  Traversing those structures to read those annotations'/modifiers' status
    if (macroClass != null) {

      HaxeSimpleMeta simpleMeta = macroClass.getSimpleMeta();
      if (simpleMeta != null) {
        if (simpleMeta.getBindMeta() != null) {
          haxePsiModifierList.setModifierProperty(HaxePsiModifier.BIND, true);
        }
        if (simpleMeta.getCoreApiMeta() != null) {
          haxePsiModifierList.setModifierProperty(HaxePsiModifier.COREAPI, true);
        }
        if (simpleMeta.getFinalMeta() != null) {
          haxePsiModifierList.setModifierProperty(HaxePsiModifier.FINAL, true);
        }
        if (simpleMeta.getHackMeta() != null) {
          haxePsiModifierList.setModifierProperty(HaxePsiModifier.HACK, true);
        }
        if (simpleMeta.getKeepMeta() != null) {
          haxePsiModifierList.setModifierProperty(HaxePsiModifier.KEEP, true);
        }
        if (simpleMeta.getMacroMeta() != null) {
          haxePsiModifierList.setModifierProperty(HaxePsiModifier.MACRO, true);
        }
        if (simpleMeta.getUnreflectiveMeta() != null) {
          haxePsiModifierList.setModifierProperty(HaxePsiModifier.UNREFLECTIVE, true);
        }
      }

      //
      // XXX: UNCOMMENT & FIX below code as-and-when-needed e.g. when implementing refactoring actions
      //  -->  Translating/loading them into HaxePsiModifierList (is-a PsiModifierList)
      //

      //HaxeAutoBuildMacro autoBuildMacro = macroClass.getAutoBuildMacro();
      //if (autoBuildMacro != null) {
      //  HaxeExpression expression = autoBuildMacro.getExpression();
      //  if (expression != null) {
      //    // XXX: populate expression's required fields/data into haxePsiModifierList
      //  }
      //}
      //
      //HaxeBitmapMeta bitmapMeta = macroClass.getBitmapMeta();
      //if (bitmapMeta != null) {
      //  HaxeStringLiteralExpression stringLiteralExpression = bitmapMeta.getStringLiteralExpression();
      //  if (stringLiteralExpression != null) {
      //    List<HaxeLongTemplateEntry> longTemplateEntries = stringLiteralExpression.getLongTemplateEntryList();
      //    List<HaxeShortTemplateEntry> shortTemplateEntries = stringLiteralExpression.getShortTemplateEntryList();
      //    for (HaxeLongTemplateEntry longTemplateEntry : longTemplateEntries) {
      //      HaxeExpression expression = longTemplateEntry.getExpression();
      //      if (expression != null) {
      //        // XXX: populate expression's required fields/data into haxePsiModifierList
      //      }
      //    }
      //    for (HaxeShortTemplateEntry shortTemplateEntry : shortTemplateEntries) {
      //      HaxeExpression expression = shortTemplateEntry.getExpression();
      //      if (expression != null) {
      //        // XXX: populate expression's required fields/data into haxePsiModifierList
      //      }
      //    }
      //  }
      //}
      //
      //HaxeBuildMacro buildMacro = macroClass.getBuildMacro();
      //if (buildMacro != null) {
      //  HaxeExpression expression = buildMacro.getExpression();
      //  if (expression != null) {
      //    // XXX: populate expression's required fields/data into haxePsiModifierList
      //  }
      //}
      //
      //HaxeCustomMeta customMeta = macroClass.getCustomMeta();
      //if (customMeta != null) {
      //  HaxeExpressionList haxeExpressionList = customMeta.getExpressionList();
      //  if (haxeExpressionList != null) {
      //    List<HaxeExpression> haxeExpressions = haxeExpressionList.getExpressionList();
      //    for (HaxeExpression expression : haxeExpressions) {
      //      // XXX: populate expression's required fields/data into haxePsiModifierList
      //    }
      //  }
      //}
      //
      //HaxeFakeEnumMeta fakeEnumMeta = macroClass.getFakeEnumMeta();
      //if (fakeEnumMeta != null) {
      //  HaxeType haxeType = fakeEnumMeta.getType();
      //  if (haxeType != null) {
      //    HaxeReferenceExpression haxeReferenceExpression = haxeType.getReferenceExpression();
      //    // XXX: populate 'haxeReferenceExpression' into haxePsiModifierList
      //    HaxeTypeParam haxeTypeParam = haxeType.getTypeParam();
      //    if (haxeTypeParam != null) {
      //      List<HaxeTypeListPart> haxeTypeListParts = haxeTypeParam.getTypeList().getTypeListPartList();
      //      for (HaxeTypeListPart haxeTypeListPart : haxeTypeListParts) {
      //        // XXX: populate 'haxeTypeListPart' into haxePsiModifierList
      //      }
      //    }
      //  }
      //}
      //
      //HaxeJsRequireMeta jsRequireMeta = macroClass.getJsRequireMeta();
      //if (jsRequireMeta != null) {
      //  HaxeStringLiteralExpression stringLiteralExpression = jsRequireMeta.getStringLiteralExpression();
      //  if (stringLiteralExpression != null) {
      //    List<HaxeLongTemplateEntry> longTemplateEntries = stringLiteralExpression.getLongTemplateEntryList();
      //    List<HaxeShortTemplateEntry> shortTemplateEntries = stringLiteralExpression.getShortTemplateEntryList();
      //    for (HaxeLongTemplateEntry longTemplateEntry : longTemplateEntries) {
      //      HaxeExpression expression = longTemplateEntry.getExpression();
      //      if (expression != null) {
      //        // XXX: populate expression's required fields/data into haxePsiModifierList
      //      }
      //    }
      //    for (HaxeShortTemplateEntry shortTemplateEntry : shortTemplateEntries) {
      //      HaxeExpression expression = shortTemplateEntry.getExpression();
      //      if (expression != null) {
      //        // XXX: populate expression's required fields/data into haxePsiModifierList
      //      }
      //    }
      //  }
      //}
      //
      //HaxeMetaMeta metaMeta = macroClass.getMetaMeta();
      //if (metaMeta != null) {
      //  List<HaxeMetaKeyValue> haxeMetaKeyValueList = metaMeta.getMetaKeyValueList();
      //  for (HaxeMetaKeyValue keyValue : haxeMetaKeyValueList) {
      //    HaxeStringLiteralExpression stringLiteralExpression = keyValue.getStringLiteralExpression();
      //    if (stringLiteralExpression != null) {
      //      List<HaxeLongTemplateEntry> longTemplateEntries = stringLiteralExpression.getLongTemplateEntryList();
      //      List<HaxeShortTemplateEntry> shortTemplateEntries = stringLiteralExpression.getShortTemplateEntryList();
      //      for (HaxeLongTemplateEntry longTemplateEntry : longTemplateEntries) {
      //        HaxeExpression expression = longTemplateEntry.getExpression();
      //        if (expression != null) {
      //          // XXX: populate expression's required fields/data into haxePsiModifierList
      //        }
      //      }
      //      for (HaxeShortTemplateEntry shortTemplateEntry : shortTemplateEntries) {
      //        HaxeExpression expression = shortTemplateEntry.getExpression();
      //        if (expression != null) {
      //          // XXX: populate expression's required fields/data into haxePsiModifierList
      //        }
      //      }
      //    }
      //  }
      //}
      //
      //HaxeNativeMeta nativeMeta = macroClass.getNativeMeta();
      //if (nativeMeta != null) {
      //  HaxeStringLiteralExpression stringLiteralExpression = nativeMeta.getStringLiteralExpression();
      //  if (stringLiteralExpression != null) {
      //    List<HaxeLongTemplateEntry> longTemplateEntries = stringLiteralExpression.getLongTemplateEntryList();
      //    List<HaxeShortTemplateEntry> shortTemplateEntries = stringLiteralExpression.getShortTemplateEntryList();
      //    for (HaxeLongTemplateEntry longTemplateEntry : longTemplateEntries) {
      //      HaxeExpression expression = longTemplateEntry.getExpression();
      //      if (expression != null) {
      //        // XXX: populate expression's required fields/data into haxePsiModifierList
      //      }
      //    }
      //    for (HaxeShortTemplateEntry shortTemplateEntry : shortTemplateEntries) {
      //      HaxeExpression expression = shortTemplateEntry.getExpression();
      //      if (expression != null) {
      //        // XXX: populate expression's required fields/data into haxePsiModifierList
      //      }
      //    }
      //  }
      //}
      //
      //HaxeNsMeta nsMeta = macroClass.getNsMeta();
      //if (nsMeta  != null) {
      //  HaxeStringLiteralExpression stringLiteralExpression = nsMeta.getStringLiteralExpression();
      //  if (stringLiteralExpression != null) {
      //    List<HaxeLongTemplateEntry> longTemplateEntries = stringLiteralExpression.getLongTemplateEntryList();
      //    List<HaxeShortTemplateEntry> shortTemplateEntries = stringLiteralExpression.getShortTemplateEntryList();
      //    for (HaxeLongTemplateEntry longTemplateEntry : longTemplateEntries) {
      //      HaxeExpression expression = longTemplateEntry.getExpression();
      //      if (expression != null) {
      //        // XXX: populate expression's required fields/data into haxePsiModifierList
      //      }
      //    }
      //    for (HaxeShortTemplateEntry shortTemplateEntry : shortTemplateEntries) {
      //      HaxeExpression expression = shortTemplateEntry.getExpression();
      //      if (expression != null) {
      //        // XXX: populate expression's required fields/data into haxePsiModifierList
      //      }
      //    }
      //  }
      //}
      //
      //HaxeRequireMeta requireMeta = macroClass.getRequireMeta();
      //if (requireMeta != null) {
      //  HaxeIdentifier identifier = requireMeta.getIdentifier();
      //  if (identifier != null) {
      //    // XXX: populate 'identifier' into haxePsiModifierList
      //  }
      //}
    }

    return haxePsiModifierList;
  }

  @Override
  public boolean hasModifierProperty(@HaxePsiModifier.ModifierConstant @NonNls @NotNull String name) {
    if (HaxePsiModifier.PUBLIC.equals(name)) {
      return isPublic();
    }
    return getModifierList().hasModifierProperty(name);
  }

  @Override
  @Nullable
  public PsiDocComment getDocComment() {
    PsiComment psiComment = HaxeResolveUtil.findDocumentation(this);
    return ((psiComment != null)? new HaxePsiDocComment(this, psiComment) : null);
  }

  @Override
  @NotNull
  public PsiElement getNavigationElement() {
    return this;
  }

  @Override
  @Nullable
  public PsiIdentifier getNameIdentifier() {
    return ((PsiIdentifier) findChildByRoleAsPsiElement(ChildRole.NAME));
  }

  @Override
  @NotNull
  public Collection<HierarchicalMethodSignature> getVisibleSignatures() {
    // TODO: broken after change 49d2848 ... EB's fixes are needed
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
