/*
 * Copyright 2017 Eric Bishton
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
package com.intellij.plugins.haxe.util;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


/**
 * Creates a delegating wrapper to hold a random PsiElement.  This exists
 * primarily so that an element can be used in a "has-a" relationship when
 * "is-a" is expected by the API, but some extra functionality needs to be
 * controlled (such as the presentation in FindUsages).
 */
public class PsiElementDelegate implements PsiElement {

  protected PsiElement wrappedElement;

  public PsiElementDelegate(@NotNull PsiElement element) {
    wrappedElement = element;
  }

  public PsiElement getWrappedElement() {
    return  wrappedElement;
  }


  @Override
  @NotNull
  @Contract(
      pure = true
  )
  public Project getProject() throws PsiInvalidElementAccessException {
    return wrappedElement.getProject();
  }

  @Override
  @NotNull
  @Contract(
      pure = true
  )
  public Language getLanguage() {
    return wrappedElement.getLanguage();
  }

  @Override
  @Contract(
      pure = true
  )
  public PsiManager getManager() {
    return wrappedElement.getManager();
  }

  @Override
  @NotNull
  @Contract(
      pure = true
  )
  public PsiElement[] getChildren() {
    return wrappedElement.getChildren();
  }

  @Override
  @Contract(
      pure = true
  )
  public PsiElement getParent() {
    return wrappedElement.getParent();
  }

  @Override
  @Contract(
      pure = true
  )
  public PsiElement getFirstChild() {
    return wrappedElement.getFirstChild();
  }

  @Override
  @Contract(
      pure = true
  )
  public PsiElement getLastChild() {
    return wrappedElement.getLastChild();
  }

  @Override
  @Contract(
      pure = true
  )
  public PsiElement getNextSibling() {
    return wrappedElement.getNextSibling();
  }

  @Override
  @Contract(
      pure = true
  )
  public PsiElement getPrevSibling() {
    return wrappedElement.getPrevSibling();
  }

  @Override
  @Contract(
      pure = true
  )
  public PsiFile getContainingFile() throws PsiInvalidElementAccessException {
    return wrappedElement.getContainingFile();
  }

  @Override
  @Contract(
      pure = true
  )
  public TextRange getTextRange() {
    return wrappedElement.getTextRange();
  }

  @Override
  @Contract(
      pure = true
  )
  public int getStartOffsetInParent() {
    return wrappedElement.getStartOffsetInParent();
  }

  @Override
  @Contract(
      pure = true
  )
  public int getTextLength() {
    return wrappedElement.getTextLength();
  }

  @Override
  @Nullable
  @Contract(
      pure = true
  )
  public PsiElement findElementAt(int offset) {
    return wrappedElement.findElementAt(offset);
  }

  @Override
  @Nullable
  @Contract(
      pure = true
  )
  public PsiReference findReferenceAt(int offset) {
    return wrappedElement.findReferenceAt(offset);
  }

  @Override
  @Contract(
      pure = true
  )
  public int getTextOffset() {
    return wrappedElement.getTextOffset();
  }

  @Override
  @NonNls
  @Contract(
      pure = true
  )
  public String getText() {
    return wrappedElement.getText();
  }

  @Override
  @NotNull
  @Contract(
      pure = true
  )
  public char[] textToCharArray() {
    return wrappedElement.textToCharArray();
  }

  @Override
  @Contract(
      pure = true
  )
  public PsiElement getNavigationElement() {
    return wrappedElement.getNavigationElement();
  }

  @Override
  @Contract(
      pure = true
  )
  public PsiElement getOriginalElement() {
    return wrappedElement.getOriginalElement();
  }

  @Override
  @Contract(
      pure = true
  )
  public boolean textMatches(@NotNull CharSequence text) {
    return wrappedElement.textMatches(text);
  }

  @Override
  @Contract(
      pure = true
  )
  public boolean textMatches(@NotNull PsiElement element) {
    return wrappedElement.textMatches(element);
  }

  @Override
  @Contract(
      pure = true
  )
  public boolean textContains(char c) {
    return wrappedElement.textContains(c);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    wrappedElement.accept(visitor);
  }

  @Override
  public void acceptChildren(@NotNull PsiElementVisitor visitor) {
    wrappedElement.acceptChildren(visitor);
  }

  @Override
  public PsiElement copy() {
    return wrappedElement.copy();
  }

  @Override
  public PsiElement add(@NotNull PsiElement element) throws IncorrectOperationException {
    return wrappedElement.add(element);
  }

  @Override
  public PsiElement addBefore(@NotNull PsiElement element, @Nullable PsiElement anchor) throws IncorrectOperationException {
    return wrappedElement.addBefore(element, anchor);
  }

  @Override
  public PsiElement addAfter(@NotNull PsiElement element, @Nullable PsiElement anchor) throws IncorrectOperationException {
    return wrappedElement.addAfter(element, anchor);
  }

  @Override
  public void checkAdd(@NotNull PsiElement element) throws IncorrectOperationException {
    wrappedElement.checkAdd(element);
  }

  @Override
  public PsiElement addRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
    return wrappedElement.addRange(first, last);
  }

  @Override
  public PsiElement addRangeBefore(@NotNull PsiElement first, @NotNull PsiElement last, PsiElement anchor)
    throws IncorrectOperationException {
    return wrappedElement.addRangeBefore(first, last, anchor);
  }

  @Override
  public PsiElement addRangeAfter(PsiElement first, PsiElement last, PsiElement anchor) throws IncorrectOperationException {
    return wrappedElement.addRangeAfter(first, last, anchor);
  }

  @Override
  public void delete() throws IncorrectOperationException {
    wrappedElement.delete();
  }

  @Override
  public void checkDelete() throws IncorrectOperationException {
    wrappedElement.checkDelete();
  }

  @Override
  public void deleteChildRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
    wrappedElement.deleteChildRange(first, last);
  }

  @Override
  public PsiElement replace(@NotNull PsiElement newElement) throws IncorrectOperationException {
    return wrappedElement.replace(newElement);
  }

  @Override
  @Contract(
      pure = true
  )
  public boolean isValid() {
    return wrappedElement.isValid();
  }

  @Override
  @Contract(
      pure = true
  )
  public boolean isWritable() {
    return wrappedElement.isWritable();
  }

  @Override
  @Nullable
  @Contract(
      pure = true
  )
  public PsiReference getReference() {
    return wrappedElement.getReference();
  }

  @Override
  @NotNull
  @Contract(
      pure = true
  )
  public PsiReference[] getReferences() {
    return wrappedElement.getReferences();
  }

  @Override
  @Nullable
  @Contract(
      pure = true
  )
  public <T> T getCopyableUserData(Key<T> key) {
    return wrappedElement.getCopyableUserData(key);
  }

  @Override
  public <T> void putCopyableUserData(Key<T> key, @Nullable T value) {
    wrappedElement.putCopyableUserData(key, value);
  }

  @Override
  public boolean processDeclarations(@NotNull PsiScopeProcessor processor,
                                     @NotNull ResolveState state,
                                     @Nullable PsiElement lastParent, @NotNull PsiElement place) {
    return wrappedElement.processDeclarations(processor, state, lastParent, place);
  }

  @Override
  @Nullable
  @Contract(
      pure = true
  )
  public PsiElement getContext() {
    return wrappedElement.getContext();
  }

  @Override
  @Contract(
      pure = true
  )
  public boolean isPhysical() {
    return wrappedElement.isPhysical();
  }

  @Override
  @NotNull
  @Contract(
      pure = true
  )
  public GlobalSearchScope getResolveScope() {
    return wrappedElement.getResolveScope();
  }

  @Override
  @NotNull
  @Contract(
      pure = true
  )
  public SearchScope getUseScope() {
    return wrappedElement.getUseScope();
  }

  @Override
  @Contract(
      pure = true
  )
  public ASTNode getNode() {
    return wrappedElement.getNode();
  }

  @Override
  @NonNls
  @Contract(
      pure = true
  )
  public String toString() {
    return wrappedElement.toString();
  }

  @Override
  @Contract(
      pure = true
  )
  public boolean isEquivalentTo(PsiElement another) {
    return wrappedElement.isEquivalentTo(another);
  }

  @Override
  @Nullable
  public <T> T getUserData(@NotNull Key<T> key) {
    return wrappedElement.getUserData(key);
  }

  @Override
  public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
    wrappedElement.putUserData(key, value);
  }

  @Override
  public Icon getIcon(int flags) {
    return wrappedElement.getIcon(flags);
  }
}
