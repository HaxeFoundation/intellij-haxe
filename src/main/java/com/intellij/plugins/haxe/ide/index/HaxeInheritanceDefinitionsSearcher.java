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
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.stubs.index.specialized.HaxeSuperClassStubIndex;
import com.intellij.plugins.haxe.util.HaxeNamedSubComponentUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.DefinitionsScopedSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.intellij.plugins.haxe.util.HaxeResolveUtil.getSimpleName;

public class HaxeInheritanceDefinitionsSearcher extends QueryExecutorBase<PsiElement, DefinitionsScopedSearch.SearchParameters> {

  public HaxeInheritanceDefinitionsSearcher() {
    super(true);
  }




  /** Package access.  This should only be called from HaxeInheritanceDefinitionsSearchExecutor. */
  @Override
  public void processQuery(@NotNull DefinitionsScopedSearch.SearchParameters queryParameters, @NotNull Processor<? super PsiElement> consumer) {
    final PsiElement queryParameterElement = queryParameters.getElement();
    final PsiElement queryParametersParentElement = queryParameterElement.getParent();

    HaxeNamedComponent haxeNamedComponent;
    if (queryParameterElement instanceof HaxeClass) {
      haxeNamedComponent = (HaxeClass)queryParameterElement;
    }
    else if (queryParametersParentElement instanceof HaxeNamedComponent && queryParameterElement instanceof HaxeComponentName) {
      haxeNamedComponent = (HaxeNamedComponent)queryParametersParentElement;
    }
    else {
      return;
    }
    if (haxeNamedComponent instanceof HaxeClass) {
      processInheritors(((HaxeClass)haxeNamedComponent).getQualifiedName(), queryParameterElement, consumer);
    }
    else {
      HaxeComponentType componentType = haxeNamedComponent.getComponentType();
      if (componentType == HaxeComponentType.METHOD || componentType == HaxeComponentType.FIELD) {
        final String nameToFind = haxeNamedComponent.getName();
        if (nameToFind == null) return;

        HaxeClass haxeClass = PsiTreeUtil.getStubOrPsiParentOfType(haxeNamedComponent, HaxeClass.class);
        assert haxeClass != null;

        processInheritors(haxeClass.getQualifiedName(), queryParameterElement, element -> {
          for (HaxeNamedComponent subHaxeNamedComponent : HaxeNamedSubComponentUtil.getNamedSubComponentsFromClassType((HaxeClass)element)) {
            if (nameToFind.equals(subHaxeNamedComponent.getName())) {
              consumer.process(subHaxeNamedComponent);
            }
          }
          return true;
        });
      }
    }
  }

  static private void processInheritors(final String qName, final PsiElement context, final Processor<? super PsiElement> consumer) {
    final Set<String> namesSet = new HashSet<>();
    final LinkedList<String> namesQueue = new LinkedList<>();
    namesQueue.add(qName);
    final Project project = context.getProject();
    final GlobalSearchScope scope = GlobalSearchScope.allScope(project);

    while (!namesQueue.isEmpty()) {
      final String name = namesQueue.pollFirst();
      if (!namesSet.add(name)) continue;

      // The stub index keys are the unresolved reference text from source (simple names in most cases).
      // Look up by both the simple name (covers "extends Foo") and the full name if it contains dots
      // (covers "extends com.example.Foo" written literally in source).
      // MLO: AFAIK the extends expressions must be either fully qualified or simple names
      // stuff like "Module.ClassName" will not compile so we dont need to handle these
      boolean isFQN = name.contains(".");
      final String simpleName = getSimpleName(name);
      final Set<HaxeClass> candidates = new LinkedHashSet<>();

      candidates.addAll(HaxeSuperClassStubIndex.getBySuper(simpleName, project, scope));

      for (HaxeClass subClass : candidates) {
        if (subClass == null) continue;
        // Post-filter: confirm this class actually directly extends/implements the type we queried.
        // Necessary because the stub index uses simple names as keys, which can produce false positives
        // when multiple types in different packages share the same simple name.
        if (!directlyInheritsFrom(subClass, name, simpleName)) continue;
        if (!consumer.process(subClass)) return;
        final String subQName = subClass.getQualifiedName();
        if (subQName != null) namesQueue.add(subQName);
      }

      if (isFQN) {
        // Also look up by fully-qualified name for source that uses qualified extends references
        // should not be any need to resolve / verify with directlyInheritsFrom as these are Fully qualified
        Collection<HaxeClass> fqnSupers = HaxeSuperClassStubIndex.getBySuper(name, project, scope);
        for (HaxeClass subClass : fqnSupers) {
          if (!consumer.process(subClass)) return;
          final String subQName = subClass.getQualifiedName();
          if (subQName != null) namesQueue.add(subQName);
        }
      }
    }
  }


  private static boolean directlyInheritsFrom(@NotNull HaxeClass subClass, @NotNull String targetQName, String targetSimpleName) {

    for (HaxeType type : subClass.getHaxeExtendsList()) {
      HaxeReferenceExpression referenceExpression = type.getReferenceExpression();
      if(referenceExpression.textMatches(targetQName)) return true;
      if(referenceExpression.textMatches(targetSimpleName)) {
        PsiElement resolve = referenceExpression.resolve();
        String resolvedQname = resolve instanceof HaxeClass haxeClass ? haxeClass.getQualifiedName() : null;
        if (targetQName.equals(resolvedQname)) return true;
      }
    }
    for (HaxeType type : subClass.getHaxeImplementsList()) {
      HaxeReferenceExpression referenceExpression = type.getReferenceExpression();
      if(referenceExpression.textMatches(targetQName)) return true;
      if(referenceExpression.textMatches(targetSimpleName)) {
        PsiElement resolve = referenceExpression.resolve();
        String resolvedQname = resolve instanceof HaxeClass haxeClass ? haxeClass.getQualifiedName() : null;
        if (targetQName.equals(resolvedQname)) return true;
      }
    }
    return false;
  }

  /**
   * Returns true if {@code storedRef} (the raw reference text from source, e.g. {@code "Foo"} or
   * {@code "com.example.Foo"}) refers to the type identified by {@code targetQName}.
   */
  private static boolean refMatchesQName(@NotNull String storedRef, @NotNull String targetQName) {
    // Exact FQN match: stored "com.example.Foo", target "com.example.Foo"
    if (targetQName.equals(storedRef)) return true;
    // Simple-name match: stored "Foo", target "com.example.Foo"
    if (targetQName.endsWith("." + storedRef)) return true;
    return false;
  }
}
