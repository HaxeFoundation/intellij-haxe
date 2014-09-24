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
import com.intellij.lang.Language;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.impl.PsiSuperMethodImplUtil;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.MethodSignature;
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.security.ProviderException;
import java.util.List;


/**
 * @author: Srikanth.Ganapavarapu
 */
public class HaxePsiMethod extends AbstractHaxeNamedComponent implements PsiMethod {

  private HaxeComponentWithDeclarationList mHaxeMethodComponent;
  public HaxePsiMethod(@NotNull HaxeComponentWithDeclarationList inHaxeNamedComponent) {
    super(inHaxeNamedComponent.getNode());
    mHaxeMethodComponent = inHaxeNamedComponent;
  }

  public HaxeComponentWithDeclarationList getDelegate() {
    return (mHaxeMethodComponent);
  }

  @NotNull
  @Override
  public String getName() {
    if (this.isConstructor()) {
      return "new";
    }
    else {
      return getDelegate().getName();
    }
  }

  @Override
  public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
    super.setName(name);
    return getDelegate().setName(name);
  }

  @Nullable
  @Override
  public ItemPresentation getPresentation() {
    return getDelegate().getPresentation();
  }

  @Override
  public void navigate(boolean requestFocus) {
    getDelegate().navigate(requestFocus);
  }

  @Override
  public boolean canNavigate() {
    return getDelegate().canNavigate();
  }

  @Override
  public boolean canNavigateToSource() {
    return getDelegate().canNavigateToSource();
  }

  @NotNull
  @Override
  public Project getProject() throws PsiInvalidElementAccessException {
    return getDelegate().getProject();
  }

  @NotNull
  @Override
  public Language getLanguage() {
    return getDelegate().getLanguage();
  }

  @NotNull
  @Override
  public PsiElement[] getChildren() {
    return getDelegate().getChildren();
  }

  @Override
  public PsiElement getParent() {
    return getDelegate().getParent();
  }

  @Override
  public PsiElement getFirstChild() {
    return getDelegate().getFirstChild();
  }

  @Override
  public PsiElement getLastChild() {
    return getDelegate().getLastChild();
  }

  @Override
  public PsiElement getNextSibling() {
    return getDelegate().getNextSibling();
  }

  @Override
  public PsiElement getPrevSibling() {
    return getDelegate().getPrevSibling();
  }

  @Override
  public PsiFile getContainingFile() throws PsiInvalidElementAccessException {
    return getDelegate().getContainingFile();
  }

  @Override
  public TextRange getTextRange() {
    return getDelegate().getTextRange();
  }

  @Override
  public int getStartOffsetInParent() {
    return getDelegate().getStartOffsetInParent();
  }

  @Override
  public int getTextLength() {
    return getDelegate().getTextLength();
  }

  @Nullable
  @Override
  public PsiElement findElementAt(int offset) {
    return getDelegate().findElementAt(offset);
  }

  @Nullable
  @Override
  public PsiReference findReferenceAt(int offset) {
    return getDelegate().findReferenceAt(offset);
  }

  @Override
  public int getTextOffset() {
    return getDelegate().getTextOffset();
  }

  @Override
  public String getText() {
    return getDelegate().getText();
  }

  @NotNull
  @Override
  public char[] textToCharArray() {
    return getDelegate().textToCharArray();
  }

  @Override
  public PsiElement getNavigationElement() {
    return getDelegate().getNavigationElement();
  }

  @Override
  public PsiElement getOriginalElement() {
    return getDelegate().getOriginalElement();
  }

  @Override
  public boolean textMatches(@NotNull @NonNls CharSequence text) {
    return getDelegate().textMatches(text);
  }

  @Override
  public boolean textMatches(@NotNull PsiElement element) {
    return getDelegate().textMatches(element);
  }

  @Override
  public boolean textContains(char c) {
    return getDelegate().textContains(c);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    getDelegate().accept(visitor);
  }

  @Override
  public void acceptChildren(@NotNull PsiElementVisitor visitor) {
    getDelegate().acceptChildren(visitor);
  }

  @Override
  public PsiElement copy() {
    return getDelegate().copy();
  }

  @Override
  public PsiElement add(@NotNull PsiElement element) throws IncorrectOperationException {
    return getDelegate().add(element);
  }

  @Override
  public PsiElement addBefore(@NotNull PsiElement element, @Nullable PsiElement anchor) throws IncorrectOperationException {
    return getDelegate().addBefore(element, anchor);
  }

  @Override
  public PsiElement addAfter(@NotNull PsiElement element, @Nullable PsiElement anchor) throws IncorrectOperationException {
    return getDelegate().addAfter(element, anchor);
  }

  @Deprecated
  @Override
  public void checkAdd(@NotNull PsiElement element) throws IncorrectOperationException {
    getDelegate().checkAdd(element);
  }

  @Override
  public PsiElement addRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
    return getDelegate().addRange(first, last);
  }

  @Override
  public PsiElement addRangeBefore(@NotNull PsiElement first, @NotNull PsiElement last, PsiElement anchor)
    throws IncorrectOperationException {
    return getDelegate().addRangeBefore(first, last, anchor);
  }

  @Override
  public PsiElement addRangeAfter(PsiElement first, PsiElement last, PsiElement anchor) throws IncorrectOperationException {
    return getDelegate().addRangeAfter(first, last, anchor);
  }

  @Override
  public void delete() throws IncorrectOperationException {
    getDelegate().delete();
  }

  @Deprecated
  @Override
  public void checkDelete() throws IncorrectOperationException {
    getDelegate().checkDelete();
  }

  @Override
  public void deleteChildRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
    getDelegate().deleteChildRange(first, last);
  }

  @Override
  public PsiElement replace(@NotNull PsiElement newElement) throws IncorrectOperationException {
    return getDelegate().replace(newElement);
  }

  @Override
  public boolean isValid() {
    return getDelegate().isValid();
  }

  @Override
  public boolean isWritable() {
    return getDelegate().isWritable();
  }

  @Nullable
  @Override
  public PsiReference getReference() {
    return getDelegate().getReference();
  }

  @NotNull
  @Override
  public PsiReference[] getReferences() {
    return getDelegate().getReferences();
  }

  @Nullable
  @Override
  public <T> T getCopyableUserData(Key<T> key) {
    return getDelegate().getCopyableUserData(key);
  }

  @Override
  public <T> void putCopyableUserData(Key<T> key, @Nullable T value) {
    getDelegate().putCopyableUserData(key, value);
  }

  @Override
  public boolean processDeclarations(@NotNull PsiScopeProcessor processor,
                                     @NotNull ResolveState state,
                                     @Nullable PsiElement lastParent,
                                     @NotNull PsiElement place) {
    return getDelegate().processDeclarations(processor, state, lastParent, place);
  }

  @Nullable
  @Override
  public PsiElement getContext() {
    return getDelegate().getContext();
  }

  @Override
  public boolean isPhysical() {
    return getDelegate().isPhysical();
  }

  @NotNull
  @Override
  public GlobalSearchScope getResolveScope() {
    return getDelegate().getResolveScope();
  }

  @NotNull
  @Override
  public SearchScope getUseScope() {
    return getDelegate().getUseScope();
  }

  @Override
  public ASTNode getNode() {
    return getDelegate().getNode();
  }

  @Override
  public boolean isEquivalentTo(PsiElement another) {
    return getDelegate().isEquivalentTo(another);
  }

  @Override
  public Icon getIcon(@IconFlags int flags) {
    return getDelegate().getIcon(flags);
  }

  @Nullable
  @Override
  public <T> T getUserData(@NotNull Key<T> key) {
    return getDelegate().getUserData(key);
  }

  @Override
  public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
    getDelegate().putUserData(key, value);
  }

  @Nullable
  // @ O v e r r i d e
  public HaxeComponentName getComponentName() {
    return getDelegate().getComponentName();
  }

  @Nullable
  @Override
  public PsiType getReturnType() {
    /* TODO: [TiVo]: translate below returned objects into PsiType */
    // HaxeReturnStatement returnStatement = getDelegate().getReturnStatement();
    // HaxeFunctionType type = getDelegate().getTypeTag().getFunctionType();
    return null;
  }

  @Nullable
  @Override
  public PsiTypeElement getReturnTypeElement() {
    /* TODO: [TiVo]: translate below returned objects into PsiTypeElement */
    // HaxeReturnStatement returnStatement = getDelegate().getReturnStatement();
    // HaxeFunctionType type = getDelegate().getTypeTag().getFunctionType();
    return null;
  }

  @NotNull
  @Override
  public HaxeParameterList getParameterList() {

    // HACK HACK HACK
    // This breaks the compiler's type and error checking.
    // HaxeComponentWithDeclarationList should implement or derive
    // from HaxePsiMethod.  We shouldn't be checking and calling
    // specific types.  This is the easy way out for the moment.

    HaxeComponentWithDeclarationList delegate = getDelegate();
    if (delegate instanceof HaxeFunctionDeclarationWithAttributes) {
      return ((HaxeFunctionDeclarationWithAttributes)delegate).getParameterList();
    }
    if (delegate instanceof HaxeFunctionPrototypeDeclarationWithAttributes) {
      return ((HaxeFunctionPrototypeDeclarationWithAttributes)delegate).getParameterList();
    }
    if (delegate instanceof HaxeExternFunctionDeclaration) {
      return ((HaxeExternFunctionDeclaration)delegate).getParameterList();
    }

    throw new UnknownSubclassEncounteredException(delegate.getClass().toString());
  }

  @NotNull
  @Override
  public PsiReferenceList getThrowsList() {

    // HACK HACK HACK See above comment.

    PsiReferenceList prl;
    HaxeComponentWithDeclarationList delegate = getDelegate();
    if (delegate instanceof HaxeExternFunctionDeclaration) {
      prl = new HaxePsiReferenceList(((HaxeExternFunctionDeclaration)delegate).getThrowStatement().getNode());
    } else if (delegate instanceof HaxeFunctionPrototypeDeclarationWithAttributes) {
      prl = new HaxePsiReferenceList(new HaxeDummyASTNode("ThrowsList"));
    } else if (delegate instanceof HaxeFunctionDeclarationWithAttributes) {
      prl = new HaxePsiReferenceList(((HaxeFunctionDeclarationWithAttributes)delegate).getThrowStatement().getNode());
    } else {
      throw new UnknownSubclassEncounteredException(delegate.getClass().toString());
    }
    return prl;
  }

  @Nullable
  @Override
  public PsiCodeBlock getBody() {

    // HACK HACK HACK See above comment.

    PsiCodeBlock pcb;
    HaxeComponentWithDeclarationList delegate = getDelegate();
    if (delegate instanceof HaxeFunctionDeclarationWithAttributes) {
      pcb = ((HaxeFunctionDeclarationWithAttributes)delegate).getBlockStatement().getCodeBlock();
    } else if (delegate instanceof HaxeFunctionPrototypeDeclarationWithAttributes) {
      pcb = null;
    } else if (delegate instanceof HaxeExternFunctionDeclaration) {
      pcb = ((HaxeExternFunctionDeclaration)delegate).getBlockStatement().getCodeBlock();
    } else {
      throw new UnknownSubclassEncounteredException(delegate.getClass().toString());
    }
    return pcb;
  }

  @Override
  public boolean isConstructor() {
    if (getDelegate().getName()==null &&
        getDelegate().getComponentName()==null &&
        getDelegate().getText().contains("function new(") &&
        !getDelegate().isStatic() &&
        !getDelegate().isOverride()) {
      return true;
    }
    return false;
  }

  @Override
  public boolean isVarArgs() {
    /* TODO: [TiVo]: Implement */
    return false;
  }

  @Override
  public boolean hasModifierProperty(@PsiModifier.ModifierConstant @NonNls @NotNull String name) {
    if (PsiModifier.PUBLIC.equals(name)) {
      return getDelegate().isPublic();
    }
    else if (PsiModifier.PRIVATE.equals(name)) {
      return (! getDelegate().isPublic());
    }
    else if (PsiModifier.STATIC.equals(name)) {
      return (getDelegate().isStatic());
    }
    else if (getModifierList() != null) {
      return getModifierList().hasModifierProperty(name);
    }
    return false;
  }

  @Nullable
  @Override
  public PsiDocComment getDocComment() {
    return new HaxePsiDocComment(getDelegate());
  }

  @Override
  public boolean isDeprecated() {
    return false;
  }

  @Override
  public boolean hasTypeParameters() {
    return PsiImplUtil.hasTypeParameters(this);
  }

  @NotNull
  @Override
  public PsiTypeParameter[] getTypeParameters() {
    return PsiImplUtil.getTypeParameters(this);
  }

  @Nullable
  @Override
  public PsiClass getContainingClass() {
    return PsiTreeUtil.getParentOfType(this, HaxeClass.class, true);
  }

  @NotNull
  @Override
  public MethodSignature getSignature(@NotNull PsiSubstitutor substitutor) {
    /* TODO: [TiVo]: Implement */
    return null;
  }

  @Nullable
  @Override
  public PsiIdentifier getNameIdentifier() {
    PsiIdentifier foundName = null;
    ASTNode node = getNode();
    if (null != node) {
      ASTNode element = node.findChildByType(HaxeTokenTypes.IDENTIFIER);
      if (null != element) {
        foundName = (PsiIdentifier) element.getPsi();
      }
    }
    return foundName;
  }

  @NotNull
  @Override
  public PsiMethod[] findSuperMethods() {
    return PsiSuperMethodImplUtil.findSuperMethods(this);
  }

  @NotNull
  @Override
  public PsiMethod[] findSuperMethods(boolean checkAccess) {
    return PsiSuperMethodImplUtil.findSuperMethods(this, checkAccess);
  }

  @NotNull
  @Override
  public PsiMethod[] findSuperMethods(PsiClass parentClass) {
    return PsiSuperMethodImplUtil.findSuperMethods(this, parentClass);
  }

  @NotNull
  @Override
  public List<MethodSignatureBackedByPsiMethod> findSuperMethodSignaturesIncludingStatic(boolean checkAccess) {
    return PsiSuperMethodImplUtil.findSuperMethodSignaturesIncludingStatic(this, checkAccess);
  }

  @Deprecated
  @Nullable
  @Override
  public PsiMethod findDeepestSuperMethod() {
    return PsiSuperMethodImplUtil.findDeepestSuperMethod(this);
  }

  @NotNull
  @Override
  public PsiMethod[] findDeepestSuperMethods() {
    return PsiSuperMethodImplUtil.findDeepestSuperMethods(this);
  }

  @Nullable
  @Override
  public PsiTypeParameterList getTypeParameterList() {
    /* TODO: [TiVo]: Implement */
    return null;
  }

  @NotNull
  @Override
  public PsiModifierList getModifierList() {
    // TODO: [TiVo]: is this even needed? can we get away without implementing?
    return null;
  }

  @NotNull
  @Override
  public HierarchicalMethodSignature getHierarchicalMethodSignature() {
    return PsiSuperMethodImplUtil.getHierarchicalMethodSignature(this);
  }

  // If we get rid of the above type hacks, then we don't need this
  // exception any more.
  /** Thrown when an unexpected type is encountered while trying to
   * disambiguate classes.
   */
  class UnknownSubclassEncounteredException extends ProviderException {
    UnknownSubclassEncounteredException(String s) {super(s);}
  }

}
