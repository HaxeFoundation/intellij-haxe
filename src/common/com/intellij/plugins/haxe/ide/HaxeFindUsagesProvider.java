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
package com.intellij.plugins.haxe.ide;

import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeFindUsagesProvider implements FindUsagesProvider {

  final static boolean LogQueries = true; // Turn off when done debugging.
  final static Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.ide.HaxeFindUsagesProvider");
  static {
    if (LogQueries) {
      LOG.setLevel(Level.DEBUG);
    }
  }


  @Override
  public WordsScanner getWordsScanner() {
    return null;
  }

  @Override
  public boolean canFindUsagesFor(@NotNull PsiElement psiElement) {
    boolean ret = false;
    PsiElement parent = getTargetElement(psiElement);
    if (null != parent) {
      ret = true;
    }
    LOG.debug("canFindUsagesFor("+psiElement.toString()+")->"+(ret?"true":"false"));
    return ret;
  }

  @Override
  public String getHelpId(@NotNull PsiElement psiElement) {
    return null;
  }

  @NotNull
  public String getType(@NotNull final PsiElement element) {
    String result = HaxeComponentType.getName(getTargetElement(element));
    if (null == result) {
      result = "reference";
    }
    LOG.debug("getType("+element.toString()+")->"+result);
    return result;
  }

  @NotNull
  public String getDescriptiveName(@NotNull final PsiElement element) {
    String result = HaxeComponentType.getPresentableName(getTargetElement(element));
    if (null == result) {
      result = "";
    }
    return result;
  }

  @NotNull
  public String getNodeText(@NotNull final PsiElement element, final boolean useFullName) {
    PsiElement parent = getTargetElement(element);
    if (null != parent) {
      if (useFullName) {
        if (parent instanceof HaxeClass) {
          return ((HaxeClass)parent).getQualifiedName();
        }
      }
    }
    return element.getText();
  }

  /**
   * Selected element is usually a COMPONENT_NAME, not a named element that we can get
   * typing, etc, from.  And it's no longer always the parent.  So go find the appropriate
   * element that can be searched for.
   *
   * @param psiElement - lowest level element found at a cursor position -- usually a COMPONENT_NAME.
   * @return a higher level PsiElement that has context and typing information.
   */
  @Nullable
  private PsiElement getTargetElement(@NotNull final PsiElement psiElement) {
    PsiElement parent = PsiTreeUtil.getParentOfType(psiElement, HaxeNamedComponent.class, false);
    return parent;
  }
}
