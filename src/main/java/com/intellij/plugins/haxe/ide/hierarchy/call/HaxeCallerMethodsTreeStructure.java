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

import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.ide.hierarchy.call.CallHierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.call.CallReferenceProcessor;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.LogLevel;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.ide.hierarchy.HaxeHierarchyTimeoutHandler;
import com.intellij.psi.*;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;

import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.HashMap;

/**
 * Created by ebishton on 11/5/14.
 */
@CustomLog
public class HaxeCallerMethodsTreeStructure extends HierarchyTreeStructure {

  private static final boolean DEBUG = false;
  static {
    if (DEBUG) {
      log.setLevel(LogLevel.DEBUG);
    }
  }

  private final String myScopeType;

  /**
   * Should be called in read action
   */
  public HaxeCallerMethodsTreeStructure(@NotNull Project project, @NotNull PsiMethod method, final String scopeType) {
    super(project, new CallHierarchyNodeDescriptor(project, null, method, true, false));
    myScopeType = scopeType;
  }

  private String getSearchTargetName(@NotNull final HierarchyNodeDescriptor descriptor) {
    final PsiMember enclosingElement = ((CallHierarchyNodeDescriptor)descriptor).getEnclosingElement();
    HierarchyNodeDescriptor nodeDescriptor = getBaseDescriptor();
    if (!(enclosingElement instanceof PsiMethod) || nodeDescriptor == null) {
      return "";
    }
    final PsiMethod method = (PsiMethod)enclosingElement;
    return method.getName();
  }


  @NotNull
  @Override
  protected final Object[] buildChildren(@NotNull final HierarchyNodeDescriptor descriptor) {
    if (false) {

      // XXX: (ebishton)
      //
      // This first block was an attempt to build the children with a cancelable background
      // progress indicator.  It doesn't work right (asserts that it needs to run on the UI
      // thread), but I'd like to come back to it at some
      // point.  If you click on the little 'round' indicator while running, then the process
      // will cancel, but that is a very subtle UI feature.  I'd prefer to have the progress
      // showed on the status bar with a red (X) like most background processes.


      final ArrayList<Object> children = new ArrayList<Object>();

      ApplicationManager.getApplication().runReadAction(new Runnable() {
        @Override
        public void run() {
          ProgressManager.getInstance().run(
            // TODO: Put this string in a resource bundle.
            new Task.Backgroundable(myProject, "Searching for callers of " + getSearchTargetName(descriptor),
                                    false, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
              @Override
              public void run(@NotNull ProgressIndicator indicator) {
                children.addAll(Arrays.asList(buildChildrenInternal(descriptor)));
              }
            });
        }
      });
      // XXX: If this mechanism works, make buildChildrenInternal() return a list instead of an array.
      //       So we don't have all of this useless conversion back and forth.
      return children.toArray(new Object[0]);

    } else {

      return buildChildrenInternal(descriptor);

    }
  }


  @NotNull
  protected final Object[] buildChildrenInternal(@NotNull final HierarchyNodeDescriptor descriptor) {
    final HaxeHierarchyTimeoutHandler timeoutHandler = new HaxeHierarchyTimeoutHandler();
    // final HaxeDebugTimeLog timeLog = HaxeDebugTimeLog.startNew("buildChildren", HaxeDebugTimeLog.Since.StartAndPrevious);

    try {
      final PsiMember enclosingElement = ((CallHierarchyNodeDescriptor)descriptor).getEnclosingElement();
      HierarchyNodeDescriptor nodeDescriptor = getBaseDescriptor();
      if (!(enclosingElement instanceof PsiMethod) || nodeDescriptor == null) {
        return ArrayUtil.EMPTY_OBJECT_ARRAY;
      }
      final PsiMethod method = (PsiMethod)enclosingElement;
      final PsiMethod baseMethod = (PsiMethod)((CallHierarchyNodeDescriptor)nodeDescriptor).getTargetElement();
      final SearchScope searchScope = getSearchScope(myScopeType, baseMethod.getContainingClass());

      final PsiClass originalClass = method.getContainingClass();
      assert originalClass != null;
      final PsiClassType originalType = JavaPsiFacade.getElementFactory(myProject).createType(originalClass);
      final Set<PsiMethod> methodsToFind = new HashSet<PsiMethod>();
      methodsToFind.add(method);

      // timeLog.stampAndEcho("calling method.findDeepestSuperMethods()");
      ContainerUtil.addAll(methodsToFind, method.findDeepestSuperMethods());
      // timeLog.stampAndEcho("beginning to walk " + methodsToFind.size() + " methods");

      final Map<PsiMember, NodeDescriptor<?>> methodToDescriptorMap = new HashMap<PsiMember, NodeDescriptor<?>>();
      for (final PsiMethod methodToFind : methodsToFind) {
        final HaxeCallReferenceProcessor.CallData data =
          new HaxeCallReferenceProcessor.CallData(originalClass, methodToFind, originalType, method, methodsToFind,
                                                  descriptor, methodToDescriptorMap, myProject, timeoutHandler);

        // timeLog.stampAndEcho("Looking for references in method: " + methodToFind.getName());
        MethodReferencesSearch.search(methodToFind, searchScope, true).forEach(new Processor<PsiReference>() {
          @Override
          public boolean process(final PsiReference reference) {
            for (CallReferenceProcessor processor : CallReferenceProcessor.EP_NAME.getExtensions()) {
              if (!processor.process(reference, data)) break;
              // timeLog.stampAndEcho("processing entry in forEach()");
              if (timeoutHandler.checkAndCancelIfNecessary()) break;
            }
            return !timeoutHandler.isCanceled();
          }
        });

        // timeLog.stampAndEcho("finished with " + methodToFind.getName());
        if (timeoutHandler.checkAndCancelIfNecessary()) break;
      }

      return methodToDescriptorMap.values().toArray(new Object[0]);

    } catch (ProcessCanceledException e) {

      // timeLog.stampAndEcho("ProcessCanceledException was thrown.");
      throw e;

    } finally {

      timeoutHandler.stop(); // Clean up.
      if (timeoutHandler.isCanceled()) {
        // timeLog.stampAndEcho("posting dialog with canceled message");
        timeoutHandler.postCanceledDialog(myProject);
      }
    }

  }

  @Override
  public boolean isAlwaysShowPlus() {
    return true;
  }
}
