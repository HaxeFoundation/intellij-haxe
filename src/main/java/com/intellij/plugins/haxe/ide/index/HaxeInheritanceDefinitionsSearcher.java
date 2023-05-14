/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2019 Eric Bishton
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
package com.intellij.plugins.haxe.ide.index;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.DefinitionsScopedSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndex;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Workhorse class for HaxeInheritanceDefnitionsSearchExecutor.  This class, by
 * all rights, should be (and used to be!) that class.  However, we now have wrappers
 * to deal with the changed signature of {@link QueryExecutorBase#processQuery(Object, Processor)}.
 */
public class HaxeInheritanceDefinitionsSearcher  {

  public static List<HaxeClass> getItemsByQName(final HaxeClass haxeClass) {
    final List<HaxeClass> result = new ArrayList<HaxeClass>();
    DefinitionsScopedSearch.search(haxeClass).forEach(element -> {
      if (element instanceof HaxeClass) {
        result.add((HaxeClass)element);
      }
      return true;
    });
    return result;
  }

  /** Package access.  This should only be called from HaxeInheritanceDefinitionsSearchExecutor. */
  static boolean processQueryInternal(@NotNull final PsiElement queryParameters, @NotNull final Processor<PsiElement> consumer) {
    final PsiElement queryParametersParent = queryParameters.getParent();
    HaxeNamedComponent haxeNamedComponent;
    if (queryParameters instanceof HaxeClass) {
      haxeNamedComponent = (HaxeClass)queryParameters;
    }
    else if (queryParametersParent instanceof HaxeNamedComponent && queryParameters instanceof HaxeComponentName) {
      haxeNamedComponent = (HaxeNamedComponent)queryParametersParent;
    }
    else {
      return true;
    }
    if (haxeNamedComponent instanceof HaxeClass) {
      processInheritors(((HaxeClass)haxeNamedComponent).getQualifiedName(), queryParameters, consumer);
    }
    else if (HaxeComponentType.typeOf(haxeNamedComponent) == HaxeComponentType.METHOD ||
             HaxeComponentType.typeOf(haxeNamedComponent) == HaxeComponentType.FIELD) {
      final String nameToFind = haxeNamedComponent.getName();
      if (nameToFind == null) return true;

      HaxeClass haxeClass = PsiTreeUtil.getParentOfType(haxeNamedComponent, HaxeClass.class);
      assert haxeClass != null;

      processInheritors(haxeClass.getQualifiedName(), queryParameters, element -> {
        for (HaxeNamedComponent subHaxeNamedComponent : HaxeResolveUtil.getNamedSubComponents((HaxeClass)element)) {
          if (nameToFind.equals(subHaxeNamedComponent.getName())) {
            consumer.process(subHaxeNamedComponent);
          }
        }
        return true;
      });
    }
    return true;

  }

  static private boolean processInheritors(final String qName, final PsiElement context, final Processor<? super PsiElement> consumer) {
    final Set<String> namesSet = new HashSet<String>();
    final LinkedList<String> namesQueue = new LinkedList<String>();
    namesQueue.add(qName);
    final Project project = context.getProject();
    final GlobalSearchScope scope = GlobalSearchScope.allScope(project);
    while (!namesQueue.isEmpty()) {
      final String name = namesQueue.pollFirst();
      if (!namesSet.add(name)) {
        continue;
      }
      List<List<HaxeClassInfo>> files = FileBasedIndex.getInstance().getValues(HaxeInheritanceIndex.HAXE_INHERITANCE_INDEX, name, scope);
      files.addAll(FileBasedIndex.getInstance().getValues(HaxeTypeDefInheritanceIndex.HAXE_TYPEDEF_INHERITANCE_INDEX, name, scope));
      for (List<HaxeClassInfo> subClassInfoList : files) {
        for (HaxeClassInfo subClassInfo : subClassInfoList) {
          final HaxeClass subClass = HaxeResolveUtil.findClassByQName(subClassInfo.getValue(), context.getManager(), scope);
          if (subClass != null) {
            if (!consumer.process(subClass)) {
              return true;
            }
            namesQueue.add(subClass.getQualifiedName());
          }
        }
      }
    }
    return true;
  }
}
