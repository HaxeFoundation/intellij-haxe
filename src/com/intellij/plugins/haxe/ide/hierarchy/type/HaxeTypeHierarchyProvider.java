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
package com.intellij.plugins.haxe.ide.hierarchy.type;

import com.intellij.codeInsight.TargetElementUtilBase;
import com.intellij.ide.hierarchy.HierarchyBrowser;
import com.intellij.ide.hierarchy.HierarchyProvider;
import com.intellij.ide.hierarchy.TypeHierarchyBrowserBase;
import com.intellij.ide.hierarchy.type.TypeHierarchyBrowser;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.ide.hierarchy.HaxeHierarchyUtils;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxePsiClass;
import com.intellij.plugins.haxe.lang.psi.impl.AnonymousHaxeTypeImpl;
import com.intellij.psi.*;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by ebishton on 9/3/14.
 */
public class HaxeTypeHierarchyProvider implements HierarchyProvider {
  private static final Logger LOG = Logger.getInstance("#com.intellij.ide.hierarchy.type.HaxeTypeHierarchyProvider");

  {
    LOG.info("Loaded HaxeTypeHierarchyProvider");
    LOG.setLevel(Level.DEBUG);
  }

  @Nullable
  @Override
  public PsiElement getTarget(@NotNull DataContext context) {
    if ( LOG.isDebugEnabled() ) {
      LOG.debug( "getTarget " + context );
    }

    AbstractHaxePsiClass clazz = HaxeHierarchyUtils.getContainingClass(
                                          context, false /* No anonymous classes */ );
    if (null == clazz) {
      HaxeFile f = HaxeHierarchyUtils.getContainingFile(context);
      if (f != null) {
        AbstractHaxePsiClass[] classes = HaxeHierarchyUtils.getClassList(f);
        return classes.length > 0 ? classes[0] : null;
      }
    }
    return clazz;


    //
    //
    //// TODO: EMB: Verify that this function is correct for Haxe.  It was taken wholesale from the Java implementation.
    //final Project project = CommonDataKeys.PROJECT.getData(context);
    //if (project == null) {
    //  if ( LOG.isDebugEnabled() ) {
    //    LOG.debug("No project");
    //  }
    //  return null;
    //}
    //
    //final Editor editor = CommonDataKeys.EDITOR.getData(context);
    //if (LOG.isDebugEnabled()) {
    //  LOG.debug("editor " + editor);
    //}
    //if (editor != null) {
    //  final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    //  if (file == null) {
    //    if (LOG.isDebugEnabled()) {
    //      LOG.debug("No file found.");
    //    }
    //    return null;
    //  }
    //
    //  final PsiElement targetElement = TargetElementUtilBase.findTargetElement(editor, TargetElementUtilBase.ELEMENT_NAME_ACCEPTED |
    //                                                                                   TargetElementUtilBase.REFERENCED_ELEMENT_ACCEPTED |
    //                                                                                   TargetElementUtilBase.LOOKUP_ITEM_ACCEPTED);
    //  if (LOG.isDebugEnabled()) {
    //    LOG.debug("target element " + targetElement);
    //  }
    //  if (targetElement instanceof AbstractHaxePsiClass) {
    //    return targetElement;
    //  }
    //
    //  final int offset = editor.getCaretModel().getOffset();
    //  PsiElement element = file.findElementAt(offset);
    //  while (element != null) {
    //    if (LOG.isDebugEnabled()) {
    //      LOG.debug("context element " + element);
    //    }
    //    if (element instanceof HaxeFile) {
    //      // If we're outside of a class, or just pointing at a file, we
    //      // return the first class in the file.  If the user wants a specific
    //      // type, then they'll have to set the cursor at that type (or a
    //      // component of it).
    //      if (LOG.isDebugEnabled()) {
    //        LOG.debug("Tree lookup walked to top of file.");
    //      }
    //      final AbstractHaxePsiClass[] classes = HaxeHierarchyUtils.getClassList((HaxeFile)element);
    //      return classes.length > 0 ? classes[0] : null;
    //    }
    //    if (element instanceof AbstractHaxePsiClass && !(element instanceof AnonymousHaxeTypeImpl)) {
    //      return element;
    //    }
    //    element = element.getParent();
    //  }
    //
    //  return null;
    //}
    //else {
    //  final PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(context);
    //  return element instanceof AbstractHaxePsiClass ? (AbstractHaxePsiClass)element : null;
    //}

  }

  @NotNull
  @Override
  public HierarchyBrowser createHierarchyBrowser(PsiElement element) {
    if ( LOG.isDebugEnabled() ) {
      LOG.debug( "createHierarchyBrowser " + element );
    }
    // return new TypeHierarchyBrowser(element.getProject(), (PsiClass) element);
    return null;
  }

  @Override
  public void browserActivated(@NotNull HierarchyBrowser hierarchyBrowser) {
    if ( LOG.isDebugEnabled() ) {
      LOG.debug( "browserActivated " + hierarchyBrowser );
    }
    // TODO: EMB: need to verify that this is correct for Haxe.  It was copied from Java.
    final TypeHierarchyBrowser browser = (TypeHierarchyBrowser)hierarchyBrowser;
    final String typeName =
      browser.isInterface() ? TypeHierarchyBrowserBase.SUBTYPES_HIERARCHY_TYPE : TypeHierarchyBrowserBase.TYPE_HIERARCHY_TYPE;
    browser.changeView(typeName);
  }
}
