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

import com.intellij.codeInsight.TargetElementUtilBase;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxePsiClass;
import com.intellij.plugins.haxe.lang.psi.impl.AnonymousHaxeTypeImpl;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Created by ebishton on 9/4/14.
 */
public class HaxeHierarchyUtils {
  private static final Logger LOG = Logger.getInstance("#com.intellij.ide.hierarchy.HaxeHierarchyUtils");

  // EMB: Don't check this line in...
  {
    LOG.setLevel(Level.DEBUG);
  }

  private HaxeHierarchyUtils() {
    throw new NotImplementedException("Static use only.");
  }

  public static AbstractHaxePsiClass[] getClassList(@NotNull HaxeFile psiRoot) {

    ArrayList<AbstractHaxePsiClass> classes = new ArrayList<AbstractHaxePsiClass>();
    for (PsiElement child : psiRoot.getChildren()) {
      if (child instanceof AbstractHaxePsiClass) {
        classes.add((AbstractHaxePsiClass)child);
      }
    }
    return ((AbstractHaxePsiClass[])classes.toArray());
  }

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

  public static HaxeFile getContainingFile(@NotNull DataContext context) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("getContainingFile " + context);
    }

    final Project project = CommonDataKeys.PROJECT.getData(context);
    if (project == null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("No project");
      }
      return null;
    }

    final Editor editor = CommonDataKeys.EDITOR.getData(context);
    if (editor != null) {
      final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (file == null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("No file found.");
        }
        return null;
      }

      // Walk the PSI tree toward the root.
      final int offset = editor.getCaretModel().getOffset();
      // XXX: EMB: Can we just ask for the node at offset 0??
      PsiElement element = file.findElementAt(offset);
      while (element != null) {
        if (element instanceof HaxeFile) {
          return (HaxeFile)element;
        }
        element = element.getParent();
      }
    }
    return null;
  }

} // END class HaxeHierarchyUtils
