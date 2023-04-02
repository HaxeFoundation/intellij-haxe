/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 TiVo Inc.
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

import com.intellij.ide.hierarchy.HierarchyBrowser;
import com.intellij.ide.hierarchy.HierarchyProvider;
import com.intellij.ide.hierarchy.MethodHierarchyBrowserBase;
import com.intellij.ide.hierarchy.method.MethodHierarchyBrowser;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.plugins.haxe.ide.hierarchy.HaxeHierarchyUtils;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.util.HaxeDebugLogger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by ebishton on 9/3/14.
 */
public class HaxeMethodHierarchyProvider implements HierarchyProvider {
  private static final HaxeDebugLogger LOG = HaxeDebugLogger.getInstance("#com.intellij.ide.hierarchy.type.HaxeMethodHierarchyProvider");

  {
    LOG.info("Loaded HaxeMethodHierarchyProvider");
    LOG.setLevel(Level.DEBUG);
  }

  @Nullable
  @Override
  public PsiElement getTarget(@NotNull DataContext context) {
    if ( LOG.isDebugEnabled() ) {
      LOG.debug( "getTarget " + context );
    }
    HaxeMethod foundMethod = HaxeHierarchyUtils.getTargetMethod(context);
    return foundMethod;
  }

  @NotNull
  @Override
  public HierarchyBrowser createHierarchyBrowser(PsiElement element) {
    if ( LOG.isDebugEnabled() ) {
      LOG.debug( "createHierarchyBrowser " + element );
    }
    return new HaxeMethodHierarchyBrowser(element.getProject(), (PsiMethod) element);
  }

  @Override
  public void browserActivated(@NotNull HierarchyBrowser browser) {
    if ( LOG.isDebugEnabled() ) {
      LOG.debug( "browserActivated " + browser );
    }
    ((MethodHierarchyBrowser) browser).changeView(MethodHierarchyBrowserBase.getMethodType());
  }

}
