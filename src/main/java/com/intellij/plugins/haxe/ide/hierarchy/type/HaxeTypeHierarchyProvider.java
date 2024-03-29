/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Eric Bishton
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

import com.intellij.ide.hierarchy.HierarchyBrowser;
import com.intellij.ide.hierarchy.HierarchyProvider;
import com.intellij.ide.hierarchy.TypeHierarchyBrowserBase;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.LogLevel;
import com.intellij.plugins.haxe.ide.hierarchy.HaxeHierarchyUtils;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import lombok.CustomLog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Created by ebishton on 9/3/14.
 */
@CustomLog
public class HaxeTypeHierarchyProvider implements HierarchyProvider {

  static {
    log.info("Loaded HaxeTypeHierarchyProvider");
    log.setLevel(LogLevel.DEBUG);
  }

  @Nullable
  @Override
  public PsiElement getTarget(@NotNull DataContext context) {
    HaxeClass clazz = HaxeHierarchyUtils.getContainingClass(
                                  context, false /* No anonymous classes */ );
    if (null == clazz) {
      HaxeFile f = HaxeHierarchyUtils.getContainingFile(context);
      if (f != null) {
        HaxeClass[] classes = HaxeHierarchyUtils.getClassArray(f);
        return classes.length > 0 ? classes[0] : null;
      }
    }

    return clazz;
  }

  @NotNull
  @Override
  public HierarchyBrowser createHierarchyBrowser(PsiElement element) {
    return new HaxeTypeHierarchyBrowser(element.getProject(), (PsiClass) element);
  }

  @Override
  public void browserActivated(@NotNull HierarchyBrowser hierarchyBrowser) {
    final HaxeTypeHierarchyBrowser browser = (HaxeTypeHierarchyBrowser) hierarchyBrowser;
    final String typeName =
      browser.isInterface() ? TypeHierarchyBrowserBase.getSubtypesHierarchyType() : TypeHierarchyBrowserBase.getTypeHierarchyType();
    browser.changeView(typeName);
  }
}
