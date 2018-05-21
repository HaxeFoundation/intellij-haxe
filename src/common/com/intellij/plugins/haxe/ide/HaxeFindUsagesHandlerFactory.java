/*
 * Copyright 2017 Eric Bishton
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

import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesHandlerFactory;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.lang.findUsages.LanguageFindUsages;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;

public class HaxeFindUsagesHandlerFactory extends FindUsagesHandlerFactory {

  static FindUsagesProvider haxeProvider;

  static FindUsagesProvider getHaxeProvider() {
    if (null == haxeProvider) {
      haxeProvider = LanguageFindUsages.INSTANCE.forLanguage(HaxeLanguage.INSTANCE); // Find and use the provider specified in plugin.xml.
    }
    return haxeProvider;
  }


  HaxeFindUsagesHandlerFactory(Project project) {
    super();
  }

  @Override
  public boolean canFindUsages(@NotNull final PsiElement element) {

    if (element instanceof PsiFileSystemItem) {
      if (((PsiFileSystemItem)element).getVirtualFile() == null)
        return false;
    }

    if (element.getLanguage() != HaxeLanguage.INSTANCE
    ||  !getHaxeProvider().canFindUsagesFor(element)) {
      return false;
    }

    return element.isValid();
  }

  @Override
  public FindUsagesHandler createFindUsagesHandler(@NotNull final PsiElement element, final boolean forHighlightUsages) {
    if (canFindUsages(element)) {
      PsiElement target = HaxeFindUsagesUtil.getTargetElement(element);

      if (!forHighlightUsages) {
        if (target instanceof PsiReference) {
          PsiElement resolved = ((PsiReference)target).resolve();
          if (resolved instanceof HaxeMethod) {
            target = resolved;
          }
        }
        if (target instanceof HaxeMethod) {
          PsiMethod[] supers = ((HaxeMethod)target).findSuperMethods();
          if (supers.length != 0) {
            String chosen = askWhetherToSearchForOverridingMethods(target);
            if (CURRENT_CLASS.equals(chosen))    { return new HaxeFindUsagesHandler(target); }
            else if (BASE_CLASS.equals(chosen))  { return new HaxeFindUsagesHandler(supers[supers.length - 1]); }
            else /* ANCESTOR_CLASSES */     { return new HaxeFindUsagesHandler(target, supers); }
          }
        }
      }
      return new HaxeFindUsagesHandler(target != null ? target : element);
    }
    return FindUsagesHandler.NULL_HANDLER;
  }

  //
  // TODO: Externalize the strings.
  private final static String BASE_CLASS = "Base Class Only";
  private final static String CURRENT_CLASS = "This Class Only";
  private final static String ANCESTOR_CLASSES = "This and All Parent Classes";

  /** Button text AND ordering -- see TestInterface*/
  final static String[] OVERRIDING_OPTIONS = {BASE_CLASS, CURRENT_CLASS, ANCESTOR_CLASSES};

  private String askWhetherToSearchForOverridingMethods(@NotNull PsiElement psiElement) {
    // TODO: Externalize the strings.
    int answer = Messages.showDialog(psiElement.getProject(),
                                          "Method is implemented in a base class or interface.  Would you like to find callers of the base class(es)?",
                                          "Find Method Callers",
                                          OVERRIDING_OPTIONS,
                                          0,
                                          Messages.getQuestionIcon());  // XXX - Add "Don't ask again?  Have to store that and allow a reset if we do.
    if (-1 == answer) {
      throw new ProcessCanceledException(new Throwable("FindUsages canceled by user."));
    }
    return OVERRIDING_OPTIONS[answer];
  }


  /**
   * Interface for tests to be able to respond to the dialog appropriately.
   */
  public static class TestInterface {
    public static int getOptionIndex(String option) {
      int i = OVERRIDING_OPTIONS.length;
      while (--i >= 0) {
        if (OVERRIDING_OPTIONS[i].equals(option)) {
          break;
        }
      }
      return i;
    }

    public static String getBaseClassOption() { return BASE_CLASS; }
    public static String getCurrentClassOption() { return CURRENT_CLASS; }
    public static String getAncestorClassesOption() { return ANCESTOR_CLASSES; }
  }
}
