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
package com.intellij.plugins.haxe.ide.hierarchy.call;

import com.intellij.codeInsight.TargetElementUtilBase;
import com.intellij.ide.hierarchy.CallHierarchyBrowserBase;
import com.intellij.ide.hierarchy.HierarchyBrowser;
import com.intellij.ide.hierarchy.HierarchyProvider;
import com.intellij.ide.hierarchy.call.CallHierarchyBrowser;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.ide.hierarchy.HaxeHierarchyUtils;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxePsiClass;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeClassReferenceImpl;
import com.intellij.plugins.haxe.lang.psi.impl.HaxePsiMethod;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeReferenceExpressionImpl;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by ebishton on 9/3/14.
 */
public class HaxeCallHierarchyProvider implements HierarchyProvider {
  private static final Logger LOG = Logger.getInstance("#com.intellij.ide.hierarchy.type.HaxeCallHierarchyProvider");

  {
    LOG.info("Loaded HaxeCallHierarchyProvider");
    LOG.setLevel(Level.DEBUG);
  }

  /**
   * Returns the element for which the hierarchy should be displayed.
   *
   * @param context the data context for the action invocation.
   * @return the target element, or null if the action is not applicable in this context.
   */
  @Nullable
  @Override
  public PsiElement getTarget(@NotNull DataContext context) {
    if ( LOG.isDebugEnabled() ) {
      LOG.debug( "getTarget " + context );
    }

/*
    final PsiElement logicalElement = HaxeHierarchyUtils.getReferencedElement(context);
    if (logicalElement == null) {
      return null;
    }

    // Apparently, the tree that referenced element is part of is NOT
    // the PSI tree from the parsed file, but rather a PSI tree that
    // matches the logical structure of the language.  The parent of a
    // referenced component is always the element we want the type of.
    HaxeComponentType ctype = HaxeComponentType.typeOf(logicalElement.getParent());
    if (ctype == HaxeComponentType.METHOD) {
      // What we need to return is not the element we checked the type of,
      // nor the corresponding parsed file element.
      // Instead, we need to return the composite HaxePsiMethod class.
      HaxeNamedComponent psiElement = (HaxeNamedComponent) logicalElement.getParent();
      HaxePsiMethod psiMethod = new HaxePsiMethod(psiElement);
      return psiMethod;
    }

    if (false) {
      // test code -- do not check in
      PsiElement testel = logicalElement;
      while (!(testel instanceof HaxeFile)) {
        final String result = HaxeComponentType.getName(testel);
        String type = result == null ? testel.toString() : result;
        LOG.debug( "Component type is " + type);
        testel = testel.getParent();
      }

      {
        PsiElement target = TargetElementUtilBase.findTargetElement(CommonDataKeys.EDITOR.getData(context),
                                                                    TargetElementUtilBase.ELEMENT_NAME_ACCEPTED |
                                                                    TargetElementUtilBase.REFERENCED_ELEMENT_ACCEPTED |
                                                                    TargetElementUtilBase.LOOKUP_ITEM_ACCEPTED);
        String result = HaxeComponentType.getName(target);
        String type = result == null ? target.toString() : result;
        LOG.debug("Found Component type is " + type);
      }

      {
        final String result = HaxeComponentType.getName(logicalElement.getParent());
        String type = result == null ? "reference" : result;
        LOG.debug("Component type is " + type);
      }
    }
    return null;
*/
    HaxePsiMethod foundMethod = HaxeHierarchyUtils.getTargetMethod(context);
    return foundMethod;
  }

  /**
   * Creates a browser for viewing the hierarchy of the specified element.
   *
   * @param element the element to view the hierarchy for.
   * @return the browser instance.
   */
  @NotNull
  @Override
  public HierarchyBrowser createHierarchyBrowser(PsiElement element) {
    if ( LOG.isDebugEnabled() ) {
      LOG.debug( "createHierarchyBrowser " + element );
    }
    return new CallHierarchyBrowser(element.getProject(), (PsiMethod) element);
  }

  /**
   * Notifies that the toolwindow has been shown and the specified browser is currently being displayed.
   *
   * @param hierarchyBrowser the browser instance created by {@link #createHierarchyBrowser(com.intellij.psi.PsiElement)}.
   */
  @Override
  public void browserActivated(@NotNull HierarchyBrowser hierarchyBrowser) {
    if ( LOG.isDebugEnabled() ) {
      LOG.debug( "browserActivated " + hierarchyBrowser );
    }

    ((CallHierarchyBrowser) hierarchyBrowser).changeView(CallHierarchyBrowserBase.CALLER_TYPE);
  }
}
