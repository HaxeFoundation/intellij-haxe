/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2015 Elias Ku
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

import com.intellij.ide.hierarchy.call.CallHierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.call.CallReferenceProcessor;
import com.intellij.ide.hierarchy.call.JavaCallHierarchyData;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.ide.hierarchy.HaxeHierarchyTimeoutHandler;
import com.intellij.plugins.haxe.lang.psi.HaxeNewExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeSuperExpression;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

/**
 * Created by ebishton on 1/9/15.  Lifted from JavaCallReferenceProcessor.
 */
public class HaxeCallReferenceProcessor implements CallReferenceProcessor {
  private static final Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.ide.hierarchy.call.HaxeCallReferenceProcessor");

  public static class CallData extends JavaCallHierarchyData {
    HaxeHierarchyTimeoutHandler myTimeoutHandler;

    public CallData(PsiClass originalClass,
                                 PsiMethod methodToFind,
                                 PsiClassType originalType,
                                 PsiMethod method,
                                 Set<PsiMethod> methodsToFind,
                                 NodeDescriptor nodeDescriptor,
                                 Map<PsiMember, NodeDescriptor> resultMap,
                                 Project project,
                                 HaxeHierarchyTimeoutHandler timeoutHandler) {
      super(originalClass, methodToFind, originalType, method, methodsToFind, nodeDescriptor, resultMap, project);
      myTimeoutHandler = timeoutHandler;
    }

    public HaxeHierarchyTimeoutHandler getTimeoutHandler() {
      return myTimeoutHandler;
    }
  }


  @Override
  public boolean process(@NotNull PsiReference reference, @NotNull JavaCallHierarchyData jchdata) {
    if (!(jchdata instanceof CallData)) {
      String msg = "Internal error, unexpected call data type passed in.";
      LOG.error(msg);
      throw new UnsupportedOperationException(msg);
    }
    CallData data = (CallData)jchdata;

    PsiClass originalClass = data.getOriginalClass();
    PsiMethod method = data.getMethod();
    Set<PsiMethod> methodsToFind = data.getMethodsToFind();
    PsiMethod methodToFind = data.getMethodToFind();
    PsiClassType originalType = data.getOriginalType();
    Map<PsiMember, NodeDescriptor> methodToDescriptorMap = data.getResultMap();
    Project myProject = data.getProject();
    HaxeHierarchyTimeoutHandler timeoutHandler = data.getTimeoutHandler();

    // All done, if we time out.
    if (timeoutHandler.checkAndCancelIfNecessary()) {
      return false;
    }

    if (reference instanceof HaxeReferenceExpression) {
      final PsiElement qualifierElement = ((HaxeReferenceExpression)reference).getQualifier();
      final HaxeReferenceExpression qualifier = (HaxeReferenceExpression) qualifierElement;
      if (qualifier instanceof HaxeSuperExpression) { // filter super.foo() call inside foo() and similar cases (bug 8411)
        final PsiClass superClass = PsiUtil.resolveClassInType(qualifier.getPsiType());
        if (superClass == null || originalClass.isInheritor(superClass, true)) {
          return true;
        }
      }
      if (qualifier != null && !methodToFind.hasModifierProperty(PsiModifier.STATIC)) {
        final PsiType qualifierType = qualifier.getPsiType();
        if (qualifierType instanceof PsiClassType &&
            !TypeConversionUtil.isAssignable(qualifierType, originalType) &&
            methodToFind != method) {
          final PsiClass psiClass = ((PsiClassType)qualifierType).resolve();
          if (psiClass != null) {
            final PsiMethod callee = psiClass.findMethodBySignature(methodToFind, true);
            if (callee != null && !methodsToFind.contains(callee)) {
              // skip sibling methods
              return true;
            }
          }
        }
      }
    }
    else {
      if (!(reference instanceof PsiElement)) {
        return true;
      }

      final PsiElement parent = ((PsiElement)reference).getParent();
      // If the parent is a 'new x' expression, but the reference isn't the primary
      // expression, then keep looking.
      if (parent instanceof HaxeNewExpression) {
        if (((HaxeNewExpression)parent).getType().getReferenceExpression() != reference) {
          return true;
        }
      }
      // If the reference isn't the primary expression of an anonymous class, then keep looking?
      else if (parent instanceof PsiAnonymousClass) {
        // XXX: This appears to be an optimization, knowing that an anonymous class can't be
        //      referenced from outside of itself, there's no need to load the class by
        //      calling for the base reference (the first child of the class).  The haxe
        //      PSI doesn't appear to be built in that fashion any way...
        // if (((PsiAnonymousClass)parent).getBaseClassReference() != reference) {
        //   return true;
        // }

        // let it be processed.
      }
      else {
        return true;
      }
    }

    final PsiElement element = reference.getElement();
    final PsiMember key = CallHierarchyNodeDescriptor.getEnclosingElement(element);

    synchronized (methodToDescriptorMap) {
      CallHierarchyNodeDescriptor d = (CallHierarchyNodeDescriptor)methodToDescriptorMap.get(key);
      if (d == null) {
        d = new CallHierarchyNodeDescriptor(myProject, (CallHierarchyNodeDescriptor)data.getNodeDescriptor(), element, false, true);
        methodToDescriptorMap.put(key, d);
      }
      else if (!d.hasReference(reference)) {
        d.incrementUsageCount();
      }
      d.addReference(reference);
    }
    return false;
  }

}
