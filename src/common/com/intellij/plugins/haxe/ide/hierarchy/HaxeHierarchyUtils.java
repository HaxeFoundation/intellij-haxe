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
package com.intellij.plugins.haxe.ide.hierarchy;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.codeInsight.TargetElementUtilBase;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxePsiClass;
import com.intellij.plugins.haxe.lang.psi.impl.AnonymousHaxeTypeImpl;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.MethodSignatureUtil;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * Created by ebishton on 9/4/14.
 *
 * A set of utility functions that support the HierarchyProviders.
 */
public class HaxeHierarchyUtils {
  private static final Logger LOG = Logger.getInstance("#com.intellij.ide.hierarchy.HaxeHierarchyUtils");

  static
  {
    LOG.setLevel(Level.DEBUG);
  }

  private HaxeHierarchyUtils() {
    throw new NotImplementedException("Static use only.");
  }


  /**
   * Given a PSI id element, find out if it -- or one of its parents --
   * references a class, and, if so, returns the PSI element for the class.
   *
   * @param id A PSI element for an identifier (e.g. variable name).
   * @return A PSI class element, or null if not found.
   */
  @Nullable
  public static HaxeClass findReferencedClassForId(@NotNull LeafPsiElement id) {
    if (null == id) {
      return null;
    }

    PsiReference found = id.findReferenceAt(0);
    PsiElement resolved = null;
    if (found instanceof PsiMultiReference) {
      for (PsiReference ref : ((PsiMultiReference)found).getReferences()) {
        PsiElement target = ref.resolve();
        if (null != target && target instanceof PsiClass) {
          resolved = target;
          break;
        }
      }
    }
    else {
      resolved = found.resolve();
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("findReferencedClassForID found " + resolved);
    }

    return ((resolved instanceof HaxeClass) ? ((HaxeClass) resolved) : null);
  }

  /**
   * Retrieve the list of classes implemented in the given File.
   *
   * @param psiRoot - File to search.
   * @return An array of found classes, or an empty array if none.
   */
  public static HaxeClass[] getClassList(@NotNull HaxeFile psiRoot) {

    ArrayList<HaxeClass> classes = new ArrayList<HaxeClass>();
    for (PsiElement child : psiRoot.getChildren()) {
      if (child instanceof HaxeClass) {
        classes.add((HaxeClass)child);
      }
    }
    HaxeClass[] return_type = {};
    return (classes.toArray(return_type));
  }

  /**
   * Get the PSI element for the class containing the currently focused
   * element.  Anonymous classes can be excluded if desired.
   *
   * @param context - editing context
   * @param allowAnonymous - flag to allow anonymous classes or not.
   * @return The PSI element representing the containing class.
   */
  @Nullable
  public static AbstractHaxePsiClass getContainingClass(@NotNull DataContext context, boolean allowAnonymous) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("getContainingClass " + context);
    }

    final Project project = CommonDataKeys.PROJECT.getData(context);
    if (project == null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("No project");
      }
      return null;
    }

    final Editor editor = CommonDataKeys.EDITOR.getData(context);
    if (LOG.isDebugEnabled()) {
      LOG.debug("editor " + editor);
    }
    if (editor != null) {
      final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (file == null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("No file found.");
        }
        return null;
      }

      final PsiElement targetElement = TargetElementUtilBase.findTargetElement(editor, TargetElementUtilBase.ELEMENT_NAME_ACCEPTED |
                                                                                       TargetElementUtilBase.REFERENCED_ELEMENT_ACCEPTED |
                                                                                       TargetElementUtilBase.LOOKUP_ITEM_ACCEPTED);
      if (LOG.isDebugEnabled()) {
        LOG.debug("target element " + targetElement);
      }
      if (targetElement instanceof AbstractHaxePsiClass) {
        return (AbstractHaxePsiClass)targetElement;
      }

      // Haven't found it yet, walk the PSI tree toward the root.
      final int offset = editor.getCaretModel().getOffset();
      PsiElement element = file.findElementAt(offset);
      while (element != null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("context element " + element);
        }
        if (element instanceof HaxeFile) {
          // If we get to the file node, then we're outside of a class definition.
          // No need to look further.
          return null;
        }
        if (element instanceof AbstractHaxePsiClass) {
          // Keep looking if we don't allow anonymous classes.
          if (allowAnonymous || !(element instanceof AnonymousHaxeTypeImpl)) {
            return (AbstractHaxePsiClass)element;
          }
        }
        element = element.getParent();
      }

      return null;
    }
    else {
      final PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(context);
      return element instanceof AbstractHaxePsiClass ? (AbstractHaxePsiClass)element : null;
    }
  }

  /**
   * Retrieve the PSI element for the file containing the given
   * context (focus element).
   *
   * @param context - editing context
   * @return The PSI node representing the file element.
   */
  @Nullable
  public static HaxeFile getContainingFile(@NotNull DataContext context) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("getContainingFile " + context);
    }

    // XXX: EMB: Can we just ask for the node at offset 0??
    PsiElement element = getPsiElement(context);
    while (element != null) {
      if (element instanceof HaxeFile) {
        return (HaxeFile)element;
      }
      element = element.getParent();
    }
    return null;
  }

  /**
   * Retrieve the PSI element for the given context (focal point).
   * Returns the leaf-node element at the exact position in the PSI.
   * This does NOT attempt to locate a higher-order PSI element as
   * {@link TargetElementUtilBase#findTargetElement} would.
   *
   * @param context - editing context
   * @return The PSI element at the caret position.
   */
  @Nullable
  public static PsiElement getPsiElement(@NotNull DataContext context) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("getPsiElement " + context);
    }

    PsiElement element = null;

    final Project project = CommonDataKeys.PROJECT.getData(context);
    if (project == null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("No project");
      }
      return null;
    }

    final Editor editor = CommonDataKeys.EDITOR.getData(context);
    if (LOG.isDebugEnabled()) {
      LOG.debug("editor " + editor);
    }
    if (editor != null) {
      final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (file == null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("No file found.");
        }
        return null;
      }

      final int offset = editor.getCaretModel().getOffset();
      element = file.findElementAt(offset);
    }
    else {
      element = CommonDataKeys.PSI_ELEMENT.getData(context);
    }
    return element;
  }

  @Nullable
  public static PsiElement getReferencedElement(@NotNull DataContext context) {
    PsiElement element = null;

    final Editor editor = CommonDataKeys.EDITOR.getData(context);
    if (editor != null) {
      element = TargetElementUtil.findTargetElement(editor,
                                    TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED |
                                    TargetElementUtil.ELEMENT_NAME_ACCEPTED);
    }

    return element;
  }

  /**
   * Determine if there is a method that is the target of the current
   * action, and, if so, return it.
   *
   * @param context Editor context.
   * @return The PSI method if the current context points at a method,
   *         null otherwise.
   */
  @Nullable
  public static HaxeMethod getTargetMethod(@NotNull DataContext context) {

    if (LOG.isDebugEnabled()) {
      LOG.debug("getTargetMethod " + context);
    }

    final Project project = CommonDataKeys.PROJECT.getData(context);
    if (null == project) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("No project");
      }
      return null;
    }

    final Editor editor = CommonDataKeys.EDITOR.getData(context);

    if (null == editor) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("No editor");
      }

      final PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(context);
      return element instanceof HaxeMethod ? (HaxeMethod) element : null;
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("editor " + editor);
    }

    final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("No file found.");
      }
      return null;
    }

    final PsiElement targetElement = TargetElementUtilBase.findTargetElement(editor,
                                                                             TargetElementUtilBase.ELEMENT_NAME_ACCEPTED |
                                                                             TargetElementUtilBase.REFERENCED_ELEMENT_ACCEPTED |
                                                                             TargetElementUtilBase.LOOKUP_ITEM_ACCEPTED);
    if (LOG.isDebugEnabled()) {
      LOG.debug("target element " + targetElement);
    }

    if (targetElement instanceof HaxeMethod) {
      LOG.debug("target element " + targetElement);
      return ((HaxeMethod) targetElement);
    }

    // Haven't found it yet, walk the PSI tree toward the root.
    final int offset = editor.getCaretModel().getOffset();
    PsiElement element = file.findElementAt(offset);
    while (element != null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("context element " + element);
      }
      if (element instanceof HaxeFile) {
        // If we get to the file node, then we're outside of a class definition.
        // No need to look further.
        return null;
      }
      if (element instanceof HaxeMethod) {
          return ((HaxeMethod)element);
      }
      element = element.getParent();
    }

    return null;
  }


  /**
   * Determine the class (PSI element), if any, that is referenced by the
   * given reference expression.
   *
   * @param element A PSI reference expression.
   * @return The associated class, if any.  null if not found.
   */
  @Nullable
  public static HaxeClass resolveClassReference(@NotNull HaxeReference element) {
    HaxeClassResolveResult result = element.resolveHaxeClass();
    HaxeClass pclass = result == null ? null : result.getHaxeClass();
    return pclass;
  }



  // Lifted from MethodHierarchyUtil, which needed the cannotBeOverriding helper
  // to be overridden.
  /**
   * Locates a potentially overridden method in a sub-class or an
   * intermediate parent -- up to the class containing the base method.
   *
   * Note: This CANNOT be used to find a method in a super-class of
   * baseMethod's class. Only classes that are sub-classes of baseMethod are
   * considered.
   *
   * @param baseMethod The method that may be overridden
   * @param aClass The sub-class to start inspecting at.
   * @param checkBases Whether to continue to further base classes.
   * @return The PsiMethod in the given class or the closest superclass.
   */
  public static PsiMethod findBaseMethodInClass(final PsiMethod baseMethod, final PsiClass aClass, final boolean checkBases) {
    if (baseMethod == null) return null; // base method is invalid
    if (cannotBeOverriding(baseMethod)) return null;
    return MethodSignatureUtil.findMethodBySuperMethod(aClass, baseMethod, checkBases);
  }

  /**
   * Figure out if a method can override a lower one.
   * @param method The method to test.
   * @return true if the method can override one in a superclass, false if not.
   */
  public static boolean cannotBeOverriding(final PsiMethod method) {
    // Note that in Haxe, a private method can override another private
    // method, and so can a public method (but private can't override public).
    final PsiClass parentClass = method.getContainingClass();
    return parentClass == null
           || method.isConstructor()
           || method.hasModifierProperty(PsiModifier.STATIC);
  }





} // END class HaxeHierarchyUtils
