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
package com.intellij.plugins.haxe.ide.hierarchy.method;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.JavaHierarchyUtil;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.util.CompositeAppearance;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Iconable;
import com.intellij.plugins.haxe.ide.hierarchy.HaxeHierarchyUtils;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFunctionalExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.presentation.java.ClassPresentationUtil;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.RowIcon;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Created by ebishton on 11/3/14.
 */
public final class HaxeMethodHierarchyNodeDescriptor extends HierarchyNodeDescriptor {

  //
  // Lifted, Lock, Stock, and Barrel from MethodHierarchyNodeDescriptor.  Then
  // hacked on for Haxe purposes.  The original MethodHierarchyNodeDescriptor,
  // while not final, had methods that were, and we needed to override them.
  //

  private Icon myRawIcon;
  private Icon myStateIcon;
  private HaxeMethodHierarchyTreeStructure myTreeStructure;

  public HaxeMethodHierarchyNodeDescriptor(final Project project,
                                       final HierarchyNodeDescriptor parentDescriptor,
                                       final PsiElement aClass,
                                       final boolean isBase,
                                       final HaxeMethodHierarchyTreeStructure treeStructure
  ) {
    super(project, parentDescriptor, aClass, isBase);
    myTreeStructure = treeStructure;
  }

  public final void setTreeStructure(final HaxeMethodHierarchyTreeStructure treeStructure) {
    myTreeStructure = treeStructure;
  }

  PsiMethod getMethod(final PsiClass aClass, final boolean checkBases) {
    return HaxeHierarchyUtils.findBaseMethodInClass(myTreeStructure.getBaseMethod(), aClass, checkBases);
  }

  @Nullable
  public final HaxeClass getHaxeClass() {
    return (myElement instanceof HaxeClass) ? (HaxeClass) myElement : null;
  }

  /**
  * Element for OpenFileDescriptor
  */
  public final PsiElement getTargetElement() {
    final HaxeClass theHaxeClass = getHaxeClass();
    if ((null == theHaxeClass) || (! theHaxeClass.isValid())) return null;
    final PsiMethod method = getMethod(theHaxeClass, false);
    if (method != null) return method;
    return theHaxeClass;
  }

  public final boolean isValid() {
    final HaxeClass haxePsiClass = getHaxeClass();
    return haxePsiClass != null && haxePsiClass.isValid();
  }

  public final boolean update() {
    int flags = Iconable.ICON_FLAG_VISIBILITY;
    if (isMarkReadOnly()){
      flags |= Iconable.ICON_FLAG_READ_STATUS;
    }

    boolean changes = super.update();

    final PsiElement psiClass = getHaxeClass();

    if (psiClass == null){
      final String invalidPrefix = IdeBundle.message("node.hierarchy.invalid");
      if (!myHighlightedText.getText().startsWith(invalidPrefix)) {
        myHighlightedText.getBeginning().addText(invalidPrefix, HierarchyNodeDescriptor.getInvalidPrefixAttributes());
      }
      return true;
    }

    final Icon newRawIcon = psiClass.getIcon(flags);
    final Icon newStateIcon = psiClass instanceof PsiClass ? calculateState((PsiClass)psiClass) : AllIcons.Hierarchy.MethodDefined;

    if (changes || newRawIcon != myRawIcon || newStateIcon != myStateIcon) {
      changes = true;

      myRawIcon = newRawIcon;
      myStateIcon = newStateIcon;

      Icon newIcon = myRawIcon;

      if (myIsBase) {
        final LayeredIcon icon = new LayeredIcon(2);
        icon.setIcon(newIcon, 0);
        icon.setIcon(AllIcons.Hierarchy.Base, 1, -AllIcons.Hierarchy.Base.getIconWidth() / 2, 0);
        newIcon = icon;
      }

      if (myStateIcon != null) {
        final RowIcon icon = new RowIcon(2);
        icon.setIcon(myStateIcon, 0);
        icon.setIcon(newIcon, 1);
        newIcon = icon;
      }

      setIcon(newIcon);
    }

    final CompositeAppearance oldText = myHighlightedText;

    myHighlightedText = new CompositeAppearance();
    TextAttributes classNameAttributes = null;
    if (myColor != null) {
      classNameAttributes = new TextAttributes(myColor, null, null, null, Font.PLAIN);
    }
    if (psiClass instanceof PsiClass) {
      myHighlightedText.getEnding().addText(ClassPresentationUtil.getNameForClass((PsiClass)psiClass, false), classNameAttributes);
      myHighlightedText.getEnding().addText("  (" + JavaHierarchyUtil.getPackageName((PsiClass)psiClass) + ")", HierarchyNodeDescriptor.getPackageNameAttributes());
    } else if (psiClass instanceof PsiFunctionalExpression) {
      myHighlightedText.getEnding().addText(ClassPresentationUtil.getFunctionalExpressionPresentation((PsiFunctionalExpression)psiClass, false));
    }
    myName = myHighlightedText.getText();

    if (!Comparing.equal(myHighlightedText, oldText)) {
      changes = true;
    }
    return changes;
  }

  private Icon calculateState(final PsiClass psiClass) {
    final PsiMethod method = getMethod(psiClass, false);

    if (method != null) {
      return AllIcons.Hierarchy.MethodDefined;
    }

    if (myTreeStructure.isSuperClassForBaseClass(psiClass)) {
      return AllIcons.Hierarchy.MethodNotDefined;
    }

    // Was it implemented in any superclasses?
    final PsiMethod baseClassMethod = getMethod(psiClass, true);

    final boolean hasBaseImplementation = baseClassMethod != null;

    if (hasBaseImplementation ) {
      return AllIcons.Hierarchy.MethodNotDefined;
    }
    //else {
    //  Can't "return AllIcons.Hierarchy.ShouldDefineMethod;"
    //  because Haxe doesn't have Java's idea of abstract methods.
    //  (In Haxe, abstract refers to /adding/ to an existing class or type.)
    //}

    return null;
  }

}
