package com.intellij.plugins.haxe.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author: Fedor.Korotkov
 */
public class LazyPsiElement implements PsiElement {
  private final Function<Void, PsiElement> myCreator;
  private PsiElement element = null;

  public LazyPsiElement(Function<Void, PsiElement> creator) {
    myCreator = creator;
  }

  @NotNull
  public PsiElement getElement() {
    if (element == null) {
      element = myCreator.fun(null);
    }
    return element;
  }

  @NotNull
  @Override
  public Project getProject() throws PsiInvalidElementAccessException {
    return getElement().getProject();
  }

  @NotNull
  @Override
  public Language getLanguage() {
    return getElement().getLanguage();
  }

  @Override
  public PsiManager getManager() {
    return getElement().getManager();
  }

  @NotNull
  @Override
  public PsiElement[] getChildren() {
    return getElement().getChildren();
  }

  @Override
  public PsiElement getParent() {
    return getElement().getParent();
  }

  @Override
  public PsiElement getFirstChild() {
    return getElement().getFirstChild();
  }

  @Override
  public PsiElement getLastChild() {
    return getElement().getLastChild();
  }

  @Override
  public PsiElement getNextSibling() {
    return getElement().getNextSibling();
  }

  @Override
  public PsiElement getPrevSibling() {
    return getElement().getPrevSibling();
  }

  @Override
  public PsiFile getContainingFile() throws PsiInvalidElementAccessException {
    return getElement().getContainingFile();
  }

  @Override
  public TextRange getTextRange() {
    return getElement().getTextRange();
  }

  @Override
  public int getStartOffsetInParent() {
    return getElement().getStartOffsetInParent();
  }

  @Override
  public int getTextLength() {
    return getElement().getTextLength();
  }

  @Override
  public PsiElement findElementAt(int offset) {
    return getElement().findElementAt(offset);
  }

  @Override
  public PsiReference findReferenceAt(int offset) {
    return getElement().findReferenceAt(offset);
  }

  @Override
  public int getTextOffset() {
    return getElement().getTextOffset();
  }

  @Override
  public String getText() {
    return getElement().getText();
  }

  @NotNull
  @Override
  public char[] textToCharArray() {
    return getElement().textToCharArray();
  }

  @Override
  public PsiElement getNavigationElement() {
    return getElement().getNavigationElement();
  }

  @Override
  public PsiElement getOriginalElement() {
    return getElement().getOriginalElement();
  }

  @Override
  public boolean textMatches(@NotNull @NonNls CharSequence text) {
    return getElement().textMatches(text);
  }

  @Override
  public boolean textMatches(@NotNull PsiElement element) {
    return getElement().textMatches(element);
  }

  @Override
  public boolean textContains(char c) {
    return getElement().textContains(c);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    getElement().accept(visitor);
  }

  @Override
  public void acceptChildren(@NotNull PsiElementVisitor visitor) {
    getElement().acceptChildren(visitor);
  }

  @Override
  public PsiElement copy() {
    return getElement().copy();
  }

  @Override
  public PsiElement add(@NotNull PsiElement element) throws IncorrectOperationException {
    return getElement().add(element);
  }

  @Override
  public PsiElement addBefore(@NotNull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    return getElement().addBefore(element, anchor);
  }

  @Override
  public PsiElement addAfter(@NotNull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    return getElement().addAfter(element, anchor);
  }

  @Override
  public void checkAdd(@NotNull PsiElement element) throws IncorrectOperationException {
    getElement().checkAdd(element);
  }

  @Override
  public PsiElement addRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
    return getElement().addRange(first, last);
  }

  @Override
  public PsiElement addRangeBefore(@NotNull PsiElement first, @NotNull PsiElement last, PsiElement anchor)
    throws IncorrectOperationException {
    return getElement().addRangeBefore(first, last, anchor);
  }

  @Override
  public PsiElement addRangeAfter(PsiElement first, PsiElement last, PsiElement anchor) throws IncorrectOperationException {
    return getElement().addRangeAfter(first, last, anchor);
  }

  @Override
  public void delete() throws IncorrectOperationException {
    getElement().delete();
  }

  @Override
  public void checkDelete() throws IncorrectOperationException {
    getElement().checkDelete();
  }

  @Override
  public void deleteChildRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
    getElement().deleteChildRange(first, last);
  }

  @Override
  public PsiElement replace(@NotNull PsiElement newElement) throws IncorrectOperationException {
    return getElement().replace(newElement);
  }

  @Override
  public boolean isValid() {
    return getElement().isValid();
  }

  @Override
  public boolean isWritable() {
    return getElement().isWritable();
  }

  @Override
  public PsiReference getReference() {
    return getElement().getReference();
  }

  @NotNull
  @Override
  public PsiReference[] getReferences() {
    return getElement().getReferences();
  }

  @Override
  public <T> T getCopyableUserData(Key<T> key) {
    return getElement().getCopyableUserData(key);
  }

  @Override
  public <T> void putCopyableUserData(Key<T> key, @Nullable T value) {
    getElement().putCopyableUserData(key, value);
  }

  @Override
  public boolean processDeclarations(@NotNull PsiScopeProcessor processor,
                                     @NotNull ResolveState state,
                                     @Nullable PsiElement lastParent,
                                     @NotNull PsiElement place) {
    return getElement().processDeclarations(processor, state, lastParent, place);
  }

  @Override
  public PsiElement getContext() {
    return getElement().getContext();
  }

  @Override
  public boolean isPhysical() {
    return getElement().isPhysical();
  }

  @NotNull
  @Override
  public GlobalSearchScope getResolveScope() {
    return getElement().getResolveScope();
  }

  @NotNull
  @Override
  public SearchScope getUseScope() {
    return getElement().getUseScope();
  }

  @Override
  public ASTNode getNode() {
    return getElement().getNode();
  }

  @Override
  public boolean isEquivalentTo(PsiElement another) {
    return getElement().isEquivalentTo(another);
  }

  @Override
  public Icon getIcon(@IconFlags int flags) {
    return getElement().getIcon(flags);
  }

  @Override
  public <T> T getUserData(@NotNull Key<T> key) {
    return getElement().getUserData(key);
  }

  @Override
  public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
    getElement().putUserData(key, value);
  }
}
