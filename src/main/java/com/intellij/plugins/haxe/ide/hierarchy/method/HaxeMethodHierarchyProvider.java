/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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
import com.intellij.openapi.diagnostic.LogLevel;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;

import com.intellij.plugins.haxe.ide.hierarchy.HaxeHierarchyUtils;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import lombok.CustomLog;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by ebishton on 9/3/14.
 */
@CustomLog
public class HaxeMethodHierarchyProvider implements HierarchyProvider {

  {
    log.info("Loaded HaxeMethodHierarchyProvider");
    log.setLevel(LogLevel.DEBUG);
  }

  @Nullable
  @Override
  public PsiElement getTarget(@NotNull DataContext context) {
    if ( log.isDebugEnabled() ) {
      log.debug("getTarget " + context );
    }
    HaxeMethod foundMethod = HaxeHierarchyUtils.getTargetMethod(context);
    return foundMethod;
  }

  @NotNull
  @Override
  public HierarchyBrowser createHierarchyBrowser(PsiElement element) {
    if ( log.isDebugEnabled() ) {
      log.debug("createHierarchyBrowser " + element );
    }
    return new HaxeMethodHierarchyBrowser(element.getProject(), (PsiMethod) element);
  }

  @Override
  public void browserActivated(@NotNull HierarchyBrowser browser) {
    if ( log.isDebugEnabled() ) {
      log.debug("browserActivated " + browser );
    }
    ((MethodHierarchyBrowser) browser).changeView(MethodHierarchyBrowserBase.getMethodType());
  }

}
