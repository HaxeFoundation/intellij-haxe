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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.Computable;
import com.intellij.plugins.haxe.ide.hierarchy.HaxeHierarchyTimeoutHandler;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ExtensibleQueryFactory;
import com.intellij.psi.search.searches.OverridingMethodsSearch;
import com.intellij.util.EmptyQuery;
import com.intellij.util.Query;
import com.intellij.util.QueryExecutor;
import org.jetbrains.annotations.NonNls;

/*
 * Created by ebishton on 1/21/15.  Lifted from OverridingMethodsSearch and hacked
 * because (private static) cannotBeOverridden() was incorrect for Haxe.  (Haxe private == Java protected).
 *
 * And then, we needed to add timeout checks to the processing, so we added that here.
 */
public class HaxeMethodsSearch extends ExtensibleQueryFactory<PsiMethod, HaxeMethodsSearch.SearchParameters> {

  // We're going to keep using the OverridingMethodSearch executor.
  public static ExtensionPointName<QueryExecutor> EP_NAME = ExtensionPointName.create("com.intellij.plugins.haxe.haxeMethodsSearch");
  public static final HaxeMethodsSearch INSTANCE = new HaxeMethodsSearch("com.intellij.plugins.haxe");

  // The Java searcher that we're using (defined in src/META-INF/plugin.xml:<plugins.haxe.haxeMethodsSearch>)
  // expects to get an OverridingMethodsSearch.SearchParameter as an argument.  We can't just use that class
  // directly, because createUniqueResultsQuery() wants a HaxeMethodsSearch.SearchParameters.  So, we just
  // make one in terms of the other.
  public static class SearchParameters extends OverridingMethodsSearch.SearchParameters {
    public SearchParameters(final PsiMethod aClass, SearchScope scope, final boolean checkDeep) {
      super(aClass, scope, checkDeep);
    }
  }

  private HaxeMethodsSearch(@NonNls final String epNameSpace) {
    super(epNameSpace);
  }

  public static Query<PsiMethod> search(final PsiMethod method, SearchScope scope, final boolean checkDeep, HaxeHierarchyTimeoutHandler timeoutHandler) {
    if (ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {
      @Override
      public Boolean compute() {
        return cannotBeOverriden(method);
      }
    })) return EmptyQuery.getEmptyQuery(); // Optimization
    return INSTANCE.createUniqueResultsQuery(new SearchParameters(method, scope, checkDeep));
  }

  private static boolean cannotBeOverriden(final PsiMethod method) {
    // In Haxe, private really means what protected means in Java.
    // There is no final keyword, either.
    final PsiClass parentClass = method.getContainingClass();
    return parentClass == null
           || method.isConstructor()
           || method.hasModifierProperty(PsiModifier.STATIC)
           || parentClass instanceof PsiAnonymousClass;
  }

  public static Query<PsiMethod> search(final PsiMethod method, final boolean checkDeep, HaxeHierarchyTimeoutHandler timeoutHandler) {
    return search(method, ApplicationManager.getApplication().runReadAction(new Computable<SearchScope>() {
      @Override
      public SearchScope compute() {
        return method.getUseScope();
      }
    }), checkDeep, timeoutHandler);
  }

  public static Query<PsiMethod> search(final PsiMethod method, HaxeHierarchyTimeoutHandler timeoutHandler) {
    return search(method, true, timeoutHandler);
  }

}
