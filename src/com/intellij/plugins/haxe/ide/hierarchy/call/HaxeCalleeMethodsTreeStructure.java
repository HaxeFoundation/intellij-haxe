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

import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.ide.hierarchy.call.CallHierarchyNodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.ide.hierarchy.HaxeHierarchyTimeoutHandler;
import com.intellij.plugins.haxe.lang.psi.HaxeCallExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxeNewExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeReference;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Processor;
import com.intellij.util.Query;
import com.intellij.util.containers.HashMap;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Created by ebishton on 1/12/15.
 */
public class HaxeCalleeMethodsTreeStructure extends HierarchyTreeStructure {
  private final String myScopeType;

  /**
   * Should be called in read action
   */
  public HaxeCalleeMethodsTreeStructure(final Project project, final PsiMethod method, final String scopeType) {
    super(project, new CallHierarchyNodeDescriptor(project, null, method, true, false));
    myScopeType = scopeType;
  }

  @NotNull
  protected final Object[] buildChildren(@NotNull final HierarchyNodeDescriptor descriptor) {
    final HaxeHierarchyTimeoutHandler timeoutHandler = new HaxeHierarchyTimeoutHandler();

    try {
      final PsiMember enclosingElement = ((CallHierarchyNodeDescriptor)descriptor).getEnclosingElement();
      if (!(enclosingElement instanceof PsiMethod)) {
        return ArrayUtil.EMPTY_OBJECT_ARRAY;
      }
      final PsiMethod method = (PsiMethod)enclosingElement;

      final ArrayList<PsiMethod> methods = new ArrayList<PsiMethod>();

      final PsiCodeBlock body = method.getBody();
      if (body != null) {
        visitor(body, methods);
      }

      final PsiMethod baseMethod = (PsiMethod)((CallHierarchyNodeDescriptor)getBaseDescriptor()).getTargetElement();
      final PsiClass baseClass = baseMethod.getContainingClass();

      final HashMap<PsiMethod, CallHierarchyNodeDescriptor> methodToDescriptorMap = new HashMap<PsiMethod, CallHierarchyNodeDescriptor>();

      final ArrayList<CallHierarchyNodeDescriptor> result = new ArrayList<CallHierarchyNodeDescriptor>();

      for (final PsiMethod calledMethod : methods) {
        if (timeoutHandler.checkAndCancelIfNecessary()) {
          break;
        }

        if (!isInScope(baseClass, calledMethod, myScopeType)) continue;

        CallHierarchyNodeDescriptor d = methodToDescriptorMap.get(calledMethod);
        if (d == null) {
          d = new CallHierarchyNodeDescriptor(myProject, descriptor, calledMethod, false, false);
          methodToDescriptorMap.put(calledMethod, d);
          result.add(d);
        }
        else {
          d.incrementUsageCount();
        }
      }

      // also add overriding methods as children
      if (!timeoutHandler.isCanceled()) {
        Query<PsiMethod> query = HaxeMethodsSearch.search(method, timeoutHandler);
        query.forEach(new Processor<PsiMethod>() {
          @Override
          public boolean process(PsiMethod overridingMethod) {
            if (isInScope(baseClass, overridingMethod, myScopeType)) {
              final CallHierarchyNodeDescriptor node =
                new CallHierarchyNodeDescriptor(myProject, descriptor, overridingMethod, false, false);
              if (!result.contains(node)) result.add(node);
            }
            return timeoutHandler.checkAndCancelIfNecessary();
          }
        });
      }

      return ArrayUtil.toObjectArray(result);

    } finally {
      // This is in a finally clause because a cancellation would otherwise throw
      // right past us.

      timeoutHandler.stop(); // Clean up.
      if (timeoutHandler.isCanceled()) {
        timeoutHandler.postCanceledDialog(myProject);
      }
    }
  }

  private static void visitor(final PsiElement element, final ArrayList<PsiMethod> methods) {
    final PsiElement[] children = element.getChildren();
    for (final PsiElement child : children) {
      visitor(child, methods);
      if (child instanceof HaxeCallExpression) {
        //final PsiReference ref = ((HaxeCallExpression)child).getReference();
        // TODO: Fix. This code shouldn't reach down into the expression this way.
        // Expressions should NOT be derived from references, though they are.  This is implemented via the BNF.
        final PsiReference ref = PsiTreeUtil.findChildOfType(child, HaxeReference.class);
        final PsiElement resolved = ref.resolve();
        if (null != resolved && resolved instanceof HaxeMethod) {
          methods.add((HaxeMethod)resolved);
        }
      }
      else if (child instanceof HaxeNewExpression) {
        final HaxeReference ref = (HaxeNewExpression)child.getReference();
        final PsiElement resolved = ref.resolve();
        if (null != resolved && resolved instanceof HaxeMethod) {
          methods.add((HaxeMethod)resolved);
        }
      }
    }
  }
}
