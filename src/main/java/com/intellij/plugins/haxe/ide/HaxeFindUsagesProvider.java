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
package com.intellij.plugins.haxe.ide;

import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;

import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.psi.PsiElement;
import lombok.CustomLog;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
@CustomLog
public class HaxeFindUsagesProvider implements FindUsagesProvider {

  @Override
  public WordsScanner getWordsScanner() {
    return null;
  }

  @Override
  public boolean canFindUsagesFor(@NotNull PsiElement psiElement) {
    boolean ret = false;
    PsiElement parent = HaxeFindUsagesUtil.getTargetElement(psiElement);
    if (null != parent) {
      ret = true;
    }
    if (log.isDebugEnabled()) {
      log.debug("canFindUsagesFor(" + debugId(psiElement) + ")->" + (ret ? "true" : "false"));
    }
    return ret;
  }

  @Override
  public String getHelpId(@NotNull PsiElement psiElement) {
    return null;
  }

  @NotNull
  public String getType(@NotNull final PsiElement element) {
    String result = HaxeComponentType.getName(HaxeFindUsagesUtil.getTargetElement(element));
    if (null == result) {
      result = "reference";
    }
    if (log.isDebugEnabled()) {
      log.debug("getType(" + debugId(element) + ")->" + result);
    }
    return result;
  }

  @NotNull
  public String getDescriptiveName(@NotNull final PsiElement element) {
    String result = HaxeComponentType.getPresentableName(HaxeFindUsagesUtil.getTargetElement(element));
    if (null == result) {
      result = "";
    }
    return result;
  }

  @NotNull
  public String getNodeText(@NotNull final PsiElement element, final boolean useFullName) {
    PsiElement parent = HaxeFindUsagesUtil.getTargetElement(element);
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
   * Get a useful debug value from an element.
   * XXX: Should check whether element is a HaxePsiElement type and call a specific debug method there?
   *
   * @param psiElement
   * @return ID for debug logging.
   */
  private static String debugId(final PsiElement psiElement) {
    String s = psiElement.toString();
    if (s.length() > DEBUG_ID_LEN) {
      s = s.substring(0, DEBUG_ID_LEN);
    }
    return s.replaceAll("\n", " ");
  }
  private static final int DEBUG_ID_LEN = 80;
}
