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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxePsiCompositeElement;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.source.PsiReferenceListImpl;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ArrayFactory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Srikanth.Ganapavarapu
 */
public class HaxePsiReferenceList extends PsiReferenceListImpl implements PsiReferenceList {

  private Role mRole;
  private AbstractHaxePsiClass mContainingClass;
  private List<PsiElement> mChildren;

  public HaxePsiReferenceList(ASTNode node) {
    super(node);
    mRole = null;
    mChildren = new ArrayList<PsiElement>();
  }

  public HaxePsiReferenceList(AbstractHaxePsiClass containingClass, ASTNode node, Role inRole) {
    super(node);
    mRole = inRole;
    mContainingClass = containingClass;
    mChildren = new ArrayList<PsiElement>();
  }

  @Override
  public Role getRole() {
    return mRole;
  }

  @NotNull
  public void addReferenceElements(PsiElement[] psiElements) {
    if (mRole.equals(PsiReferenceList.Role.EXTENDS_LIST) ||
        mRole.equals(PsiReferenceList.Role.IMPLEMENTS_LIST)) {
      for (PsiElement element : psiElements) {
        mChildren.add(element);
      }
    }
    //else {
    //  // TODO: [TiVo]: should we throw exception?
    //}
  }

  public int countChildren(IElementType type, PsiElement psiElement) {
    // no lock is needed because all chameleons are expanded already
    int count = 0;
    for (ASTNode child = psiElement.getFirstChild().getNode(); child != null; child = child.getTreeNext()) {
      if (type == child.getElementType()) {
        count++;
      }
    }

    return count;
  }

  @NotNull
  public <T extends PsiElement> T[] getChildrenAsPsiElements(@NotNull IElementType type, ArrayFactory<T> constructor, PsiElement psiElement) {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    int count = countChildren(type, psiElement);
    T[] result = constructor.create(count);
    if (count == 0) {
      return result;
    }
    int idx = 0;
    for (ASTNode child = psiElement.getFirstChild().getNode(); child != null && idx < count; child = child.getTreeNext()) {
      if (type == child.getElementType()) {
        @SuppressWarnings("unchecked") T element = (T)child.getPsi();
        // LOG.assertTrue(element != null, child);
        result[idx++] = element;
      }
    }
    return result;
  }

  @NotNull
  @Override
  public PsiJavaCodeReferenceElement[] getReferenceElements() {
    PsiJavaCodeReferenceElement[] result = {};
    //if (mRole.equals(PsiReferenceList.Role.EXTENDS_LIST) || mRole.equals(PsiReferenceList.Role.IMPLEMENTS_LIST)) {
    //  PsiElement[] array = new PsiElement[mChildren.size()];
    //  //array = mChildren.toArray(array);
    //  //return getChildrenAsPsiElements(JavaElementType.JAVA_CODE_REFERENCE, PsiJavaCodeReferenceElement.ARRAY_FACTORY, array[0]);
    //  // TODO: [TiVo]: Fix ClassCastException here
    //  try {
    //    result = ((PsiJavaCodeReferenceElement[])mChildren.toArray(array));
    //  }
    //  catch (Throwable t) {
    //    t.printStackTrace(); // XXX: don't swallow
    //  }
    //}
    //else {
    //  try {
    //    result = ((PsiJavaCodeReferenceElement[])((HaxePsiCompositeElement) getNode()).getChildren());
    //  }
    //  catch (Throwable t) {
    //    t.printStackTrace(); // XXX: don't swallow
    //  }
    //}
    return result;
  }

  @NotNull
  @Override
  public PsiClassType[] getReferencedTypes() {
    final PsiElement[] referenceElements = getReferenceElements();
    final PsiClassType[] psiClassTypes = new PsiClassType[referenceElements.length];
    final PsiElementFactory psiElementFactory = JavaPsiFacade.getElementFactory(mContainingClass.getProject());
    for (int index = 0; index < psiClassTypes.length; index++) {
      psiClassTypes[index] = psiElementFactory.createType((PsiClass)referenceElements[index]);
    }
    return psiClassTypes;
  }

  @Override
  public boolean isWritable() {
    return true;
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    super.accept(visitor);
  }

  @Override
  public PsiElement getParent() {
    return mContainingClass;
  }

  @Override
  public PsiManagerEx getManager() {
    return mContainingClass.getManager();
  }

  @NotNull
  @Override
  public Project getProject() {
    return mContainingClass.getProject();
  }

  @NotNull
  @Override
  public PsiFile getContainingFile() {
    return mContainingClass.getContainingFile();
  }
}
