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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeType;
import com.intellij.plugins.haxe.lang.psi.HaxeTypeParam;
import com.intellij.psi.*;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ArrayFactory;
import com.intellij.util.IncorrectOperationException;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * A class to encapsulate/override PsiType so that its interface can be used with Haxe.
 *
 * This class is used to get around the fact that PsiType isn't an interface.
 * Instead, we re-implement everything and introduce a has-a relationship
 * to the erstwhile child class.
 *
 * Created by ebishton on 10/18/14.
 */
public class HaxePsiTypeAdapter extends PsiType implements HaxeType {

  static final Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.lang.psi.impl.HaxePsiTypeAdapter");
  static {
    LOG.setLevel(Level.DEBUG);
  }

  public static final PsiPrimitiveType DYNAMIC = new PsiPrimitiveType("Dynamic", PsiAnnotation.EMPTY_ARRAY);

  HaxeType myType = null;

  public HaxePsiTypeAdapter(@NotNull HaxeType haxeType) {
    // Haxe doesn't use annotations in the same way that Java does.  Instead, they're all
    // modifiers, including macros (which are more like annotations).
    super(PsiAnnotation.EMPTY_ARRAY);
    myType = haxeType;
  }


  //
  // ParameterListOwner methods.
  //

  @Override
  public boolean hasTypeParameters() {
    return myType.hasTypeParameters();
  }

  @Nullable
  @Override
  public PsiTypeParameterList getTypeParameterList() {
    return myType.getTypeParameterList();
  }

  @NotNull
  @Override
  public PsiTypeParameter[] getTypeParameters() {
    return myType.getTypeParameters();
  }

  //
  // PsiMember methods.
  //

  @Nullable
  @Override
  public PsiClass getContainingClass() {
    return myType.getContainingClass();
  }

  //
  // PsiModifierListOwner methods.
  //

  @Nullable
  @Override
  public PsiModifierList getModifierList() {
    return myType.getModifierList();
  }

  @Override
  public boolean hasModifierProperty(@PsiModifier.ModifierConstant @NonNls @NotNull String name) {
    return myType.hasModifierProperty(name);
  }


  //
  // PsiType methods.
  //

  public static final HaxePsiTypeAdapter[] EMPTY_ARRAY = new HaxePsiTypeAdapter[0];
  public static final ArrayFactory<HaxePsiTypeAdapter> ARRAY_FACTORY = new ArrayFactory<HaxePsiTypeAdapter>() {
    @NotNull
    @Override
    public HaxePsiTypeAdapter[] create(int count) {
      return count == 0 ? EMPTY_ARRAY : new HaxePsiTypeAdapter[count];
    }
  };

  @NotNull
  public static HaxePsiTypeAdapter[] createArray(int count) {
    return ARRAY_FACTORY.create(count);
  }


  @NotNull
  @Override
  public PsiArrayType createArrayType() {
    // Wrong answer, but until we need it, we can punt.
    // TODO: Implement HaxePsiAdapter.createArrayType
    LOG.debug("Implement HaxePsiAdapter.createArrayType");
    return new PsiArrayType(this);
  }

  @NotNull
  @Override
  public PsiArrayType createArrayType(@NotNull PsiAnnotation... annotations) {
    // Wrong answer, but until we need it, we can punt.
    // TODO: Implement HaxePsiAdapter.createArrayType
    LOG.debug("Implement HaxePsiAdapter.createArrayType");
    return new PsiArrayType(this);
  }

  @NotNull
  @Override
  public String getPresentableText() {
    return myType.getText();
  }

  @NotNull
  @Override
  public String getCanonicalText(boolean annotated) {
    return getCanonicalText();
  }

  @NotNull
  @Override
  public String getCanonicalText() {
    return myType.getText();
  }

  @NotNull
  @Override
  public String getInternalCanonicalText() {
    return myType.getText();
  }

  @Override
  public boolean isValid() {
    return myType.isValid();
  }

  @Override
  public boolean isAssignableFrom(@NotNull PsiType type) {
    // Java uses the TypeConversionUtil.canAssignToFrom, which will most likely fail.
    // TODO: Implement HaxePsiAdapter.isAssignableFrom
    LOG.debug("Implement HaxePsiAdapter.isAssignableFrom()");
    return super.isAssignableFrom(type);
  }

  @Override
  public boolean isConvertibleFrom(@NotNull PsiType type) {
    // Java uses the TypeConversionUtil.isConvertable, which will most likely fail.
    // TODO: Implement HaxePsiAdapter.isConvertibleFrom
    LOG.debug("Implement HaxePsiAdapter.isConvertibleFrom()");
    return super.isConvertibleFrom(type);
  }

  @Override
  public boolean equalsToText(@NotNull @NonNls String text) {
    return text.equals(myType.getText());
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    myType.accept(visitor);
  }


  @Nullable
  @Override
  public GlobalSearchScope getResolveScope() {
    return myType.getResolveScope();
  }

  @NotNull
  @Override
  public PsiType[] getSuperTypes() {
    // TODO: Verify that we don't need to find supertypes through HaxePsiTypeAdapter.getSuperTypes().
    // XXX: This seems like a likely code path for call hierarchies.
    return EMPTY_ARRAY;
  }

  @NotNull
  @Override
  public PsiAnnotation[] getAnnotations() {
    // TODO: ?? Find and convert modifiers to annotations for HaxePsiTypeAdapter.getAnnotations() ??
    return PsiAnnotation.EMPTY_ARRAY;
  }

  @Override
  public PsiAnnotation findAnnotation(@NotNull @NonNls String qualifiedName) {
    // TODO: ?? Find and convert modifiers to annotations for HaxePsiTypeAdapter.findAnnotation() ??
    return super.findAnnotation(qualifiedName);
  }

  @NotNull
  @Override
  public PsiAnnotation addAnnotation(@NotNull @NonNls String qualifiedName) {
    // Duplicate the super's behavior.
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public PsiAnnotation[] getApplicableAnnotations() {
    return PsiAnnotation.EMPTY_ARRAY;
  }

  @Override
  @Deprecated
  protected String getAnnotationsTextPrefix(boolean qualified, boolean leadingSpace, boolean trailingSpace) {
    return "";
  }

  @Override
  public String toString() {
    return "HaxePsiTypeAdapter:" + getPresentableText();
  }

  @Override
  public <A> A accept(@NotNull PsiTypeVisitor<A> visitor) {
    // TODO: Figure out what accept is supposed to do for HaxePsiTypeAdapter.
    LOG.debug("Unimplemented HaxePsiTypeAdapter.accept(PsiTypeVisitor<A>) was called.");
    return null; // visitor.visitPsiType(this); // Hmm, ain't no such thing.
  }

  //
  // HaxeType methods.
  //


  @NotNull
  @Override
  public HaxeReferenceExpression getReferenceExpression() {
    return myType.getReferenceExpression();
  }

  @Nullable
  @Override
  public HaxeTypeParam getTypeParam() {
    return myType.getTypeParam();
  }

  @Nullable
  @Override
  public PsiType getPsiType() {
    return this;
  }

  @Override
  public IElementType getTokenType() {
    return myType.getTokenType();
  }

  @Nullable
  @Override
  public String getName() {
    return myType.getName();
  }

  @Nullable
  @Override
  public ItemPresentation getPresentation() {
    return myType.getPresentation();
  }

  @Override
  public void navigate(boolean requestFocus) {
    myType.navigate(requestFocus);
  }

  @Override
  public boolean canNavigate() {
    return myType.canNavigate();
  }

  @Override
  public boolean canNavigateToSource() {
    return myType.canNavigateToSource();
  }

  @NotNull
  @Override
  public Project getProject() throws PsiInvalidElementAccessException {
    return myType.getProject();
  }

  @NotNull
  @Override
  public Language getLanguage() {
    return myType.getLanguage();
  }

  @Override
  public PsiManager getManager() {
    return myType.getManager();
  }

  @NotNull
  @Override
  public PsiElement[] getChildren() {
    return myType.getChildren();
  }

  @Override
  public PsiElement getParent() {
    return myType.getParent();
  }

  @Override
  public PsiElement getFirstChild() {
    return myType.getFirstChild();
  }

  @Override
  public PsiElement getLastChild() {
    return myType.getLastChild();
  }

  @Override
  public PsiElement getNextSibling() {
    return myType.getNextSibling();
  }

  @Override
  public PsiElement getPrevSibling() {
    return myType.getPrevSibling();
  }

  @Override
  public PsiFile getContainingFile() throws PsiInvalidElementAccessException {
    return myType.getContainingFile();
  }

  @Override
  public TextRange getTextRange() {
    return myType.getTextRange();
  }

  @Override
  public int getStartOffsetInParent() {
    return myType.getStartOffsetInParent();
  }

  @Override
  public int getTextLength() {
    return myType.getTextLength();
  }

  @Nullable
  @Override
  public PsiElement findElementAt(int offset) {
    return myType.findElementAt(offset);
  }

  @Nullable
  @Override
  public PsiReference findReferenceAt(int offset) {
    return myType.findReferenceAt(offset);
  }

  @Override
  public int getTextOffset() {
    return myType.getTextOffset();
  }

  @Override
  public String getText() {
    return myType.getText();
  }

  @NotNull
  @Override
  public char[] textToCharArray() {
    return myType.textToCharArray();
  }

  @Override
  public PsiElement getNavigationElement() {
    return myType.getNavigationElement();
  }

  @Override
  public PsiElement getOriginalElement() {
    return myType.getOriginalElement();
  }

  @Override
  public boolean textMatches(@NotNull @NonNls CharSequence text) {
    return myType.textMatches(text);
  }

  @Override
  public boolean textMatches(@NotNull PsiElement element) {
    return myType.textMatches(element);
  }

  @Override
  public boolean textContains(char c) {
    return myType.textContains(c);
  }


  @Override
  public void acceptChildren(@NotNull PsiElementVisitor visitor) {
    myType.acceptChildren(visitor);
  }

  @Override
  public PsiElement copy() {
    return (HaxePsiTypeAdapter)((HaxeType)myType.copy()).getPsiType();
  }

  @Override
  public PsiElement add(@NotNull PsiElement element) throws IncorrectOperationException {
    return myType.add(element);
  }

  @Override
  public PsiElement addBefore(@NotNull PsiElement element, @Nullable PsiElement anchor) throws IncorrectOperationException {
    return myType.addBefore(element, anchor);
  }

  @Override
  public PsiElement addAfter(@NotNull PsiElement element, @Nullable PsiElement anchor) throws IncorrectOperationException {
    return myType.addAfter(element, anchor);
  }

  @Override
  @Deprecated
  public void checkAdd(@NotNull PsiElement element) throws IncorrectOperationException {
    myType.checkAdd(element);
  }

  @Override
  public PsiElement addRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
    return myType.addRange(first, last);
  }

  @Override
  public PsiElement addRangeBefore(@NotNull PsiElement first, @NotNull PsiElement last, PsiElement anchor)
    throws IncorrectOperationException {
    return myType.addRangeBefore(first, last, anchor);
  }

  @Override
  public PsiElement addRangeAfter(PsiElement first, PsiElement last, PsiElement anchor) throws IncorrectOperationException {
    return myType.addRangeAfter(first, last, anchor);
  }

  @Override
  public void delete() throws IncorrectOperationException {
    myType.delete();
  }

  @Override
  @Deprecated
  public void checkDelete() throws IncorrectOperationException {
    myType.checkDelete();
  }

  @Override
  public void deleteChildRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
    myType.deleteChildRange(first, last);
  }

  @Override
  public PsiElement replace(@NotNull PsiElement newElement) throws IncorrectOperationException {
    // XXX: Might need to return a PsiType for it, if it's a HaxeType.
    return myType.replace(newElement);
  }

  @Override
  public boolean isWritable() {
    return myType.isWritable();
  }

  @Nullable
  @Override
  public PsiReference getReference() {
    return myType.getReference();
  }

  @NotNull
  @Override
  public PsiReference[] getReferences() {
    return myType.getReferences();
  }

  @Nullable
  @Override
  public <T> T getCopyableUserData(Key<T> key) {
    return myType.getCopyableUserData(key);
  }

  @Override
  public <T> void putCopyableUserData(Key<T> key, @Nullable T value) {
    myType.putCopyableUserData(key, value);
  }

  @Override
  public boolean processDeclarations(@NotNull PsiScopeProcessor processor,
                                     @NotNull ResolveState state,
                                     @Nullable PsiElement lastParent,
                                     @NotNull PsiElement place) {
    return myType.processDeclarations(processor, state, lastParent, place);
  }

  @Nullable
  @Override
  public PsiElement getContext() {
    return myType.getContext();
  }

  @Override
  public boolean isPhysical() {
    return myType.isPhysical();
  }

  @NotNull
  @Override
  public SearchScope getUseScope() {
    return myType.getUseScope();
  }

  @Override
  public ASTNode getNode() {
    return myType.getNode();
  }

  @Override
  public boolean isEquivalentTo(PsiElement another) {
    return myType.isEquivalentTo(another);
  }

  @Override
  public Icon getIcon(@IconFlags int flags) {
    return myType.getIcon(flags);
  }

  @Nullable
  @Override
  public <T> T getUserData(@NotNull Key<T> key) {
    return myType.getUserData(key);
  }

  @Override
  public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
    myType.putUserData(key, value);
  }

}
