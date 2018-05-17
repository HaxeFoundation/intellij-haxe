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
package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.search.searches.DeepestSuperMethodsSearch;
import com.intellij.psi.search.searches.SuperMethodsSearch;
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod;
import com.intellij.psi.util.MethodSignatureUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created by ebishton on 11/5/14.
 */
public class HaxeMethodUtils {

  //
  // These functions were mostly lifted from PsiSuperMethodImplUtil.
  // And then we hacked them.
  //
  // We're checking for null instead of using @NotNull on the arguments because
  // we're adapting to an existing API that's called from the Java implementation.
  //

  private static boolean canHaveSuperMethod(PsiMethod method, boolean allowStaticMethod) {
    // Private really means protected in Haxe, so this version skips the private test.
    if (null == method || method.isConstructor()) return false;
    if (!allowStaticMethod && method.hasModifierProperty(PsiModifier.STATIC)) return false;
    PsiClass parentClass = method.getContainingClass();
    return parentClass != null;
  }

  @NotNull
  public static PsiMethod[] findSuperMethods(PsiMethod method) {
    PsiClass myclass = null == method ? null : method.getContainingClass();
    return findSuperMethods(method, myclass);
  }

  @NotNull
  public static PsiMethod[] findSuperMethods(PsiMethod method, PsiClass parentClass) {
    if (!canHaveSuperMethod(method, false)) return PsiMethod.EMPTY_ARRAY;
    return findSuperMethodsInternal(method, parentClass);
  }

  //  XXX: The old version of findSuperMethodsInternal called FindSuperMethodSignatures,
  //       which may be a better place for us to put the revised code.
  @NotNull
  private static PsiMethod[] findSuperMethodsInternal(PsiMethod method, PsiClass parentClass) {
    if (null == parentClass || null == method) return PsiMethod.EMPTY_ARRAY;
    List<PsiMethod> sooperMethods = new ArrayList<PsiMethod>();
    LinkedList<PsiClass> soopers = new LinkedList<PsiClass>(Arrays.asList(parentClass.getSupers()));
    while (!soopers.isEmpty()) {
      PsiClass sooper = soopers.pollFirst();
      // Get the super-method on the closest superclass that contains the method.
      PsiMethod sooperMethod = MethodSignatureUtil.findMethodBySignature(sooper, method, true);
      if (null != sooperMethod) {
        sooperMethods.add(sooperMethod);
        soopers.addAll(Arrays.asList(sooperMethod.getContainingClass().getSupers()));
      }
    }
    return sooperMethods.toArray(PsiMethod.EMPTY_ARRAY);
  }

  @NotNull
  public static List<MethodSignatureBackedByPsiMethod> findSuperMethodSignaturesIncludingStatic(PsiMethod method) {
    if (!canHaveSuperMethod(method, true)) return Collections.emptyList();
    return findSuperMethodSignatures(method, null, true);
  }

  @NotNull
  private static List<MethodSignatureBackedByPsiMethod> findSuperMethodSignatures(PsiMethod method,
                                                                                  PsiClass parentClass,
                                                                                  boolean allowStaticMethod) {

    return new ArrayList<MethodSignatureBackedByPsiMethod>(SuperMethodsSearch.search(method, parentClass, true, allowStaticMethod).findAll());
  }


  @Nullable
  public static PsiMethod findDeepestSuperMethod(PsiMethod method) {
    if (!canHaveSuperMethod(method, false)) return null;
    return DeepestSuperMethodsSearch.search(method).findFirst();
  }

  public static PsiMethod[] findDeepestSuperMethods(PsiMethod method) {
    if (!canHaveSuperMethod(method, false)) return PsiMethod.EMPTY_ARRAY;
    Collection<PsiMethod> collection = DeepestSuperMethodsSearch.search(method).findAll();
    return collection.toArray(new PsiMethod[collection.size()]);
  }

}
