/*
 * Copyright 2019 Eric Bishton
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
package com.intellij.plugins.haxe.ide.refactoring.rename;

import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.SearchScope;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * Handle renaming of elements.  This class accomplishes two things: it ensures that the default
 * Java handling isn't used (which magically makes things work); and it deals with the vagaries
 * of the Haxe language.
 *
 * The trouble is that the Java handling can't be completely circumvented.  So, we still have to
 * deal with issues around renaming constructors.  There are comments interspersed where we are
 * dealing with it.
 */
public class HaxeRenameProcessor extends RenamePsiElementProcessor {

  /**
   * Specifies whether a specific instance of an element can be
   * renamed.
   *
   * @param element Renaming candidate element instance.
   * @return true if the element can be renamed, false if not.
   */
  public static boolean canBeRenamed(PsiElement element) {
    if (element instanceof HaxeMethodPsiMixin) {
      return !((HaxeMethodPsiMixin)element).isConstructor();
    }
    if (element instanceof HaxeComponentName) {
      PsiElement parent = element.getParent();
      if (parent instanceof HaxeMethodPsiMixin) {
        return !((HaxeMethodPsiMixin)parent).isConstructor();
      }
    }

    return true;
  }


  public HaxeRenameProcessor() {
  }

  @Override
  public boolean canProcessElement(@NotNull PsiElement element) {
    // NOTE: We should *NOT* use canBeRenamed() as part of this call.  Doing so
    //       will cause other (Java, Pom) renaming processors to be used instead.
    //       That causes renaming to appear to work, but with errors that get
    //       blamed on this plugin.

    return element.getLanguage().isKindOf(HaxeLanguage.INSTANCE);
  }

  @NotNull
  @Override
  public Collection<PsiReference> findReferences(@NotNull PsiElement element, boolean searchInCommentsAndStrings) {
    // To stop constructors from being renamed, this is the place where we would, ultimately, like to
    // prevent the inclusion of the constructor itself.  However, the RenameJavaClassProcessor *also*
    // tries to handle the rename and we can't preempt the constructor being included via that code path.

    Collection<PsiReference> references = super.findReferences(element, searchInCommentsAndStrings);
    return references;
  }


  @Nullable
  @Override
  public PsiElement substituteElementToRename(@NotNull PsiElement element, @Nullable Editor editor) {

    // If the element selected is a "new" statement, then we really want to rename the class, not the constructor.
    if (element instanceof HaxeMethodDeclaration) {
      HaxeMethodDeclaration method = (HaxeMethodDeclaration) element;
      if (method.isConstructor()) {
        return ((HaxeMethodDeclaration)element).getContainingClass();
      }
    }

    // If the element cannot be renamed, then stop the process here. (Returning null does that.)
    //
    // Note: This will pre-empt the dialogs, which may be confusing to the user.  We could put up a
    // dialog or a message when we refuse to run.  However, *nothing* else does that when rename is
    // inappropriate.  It just quietly ignores the request.  The difference in most of those places
    // is that the Refactor->Rename menu item is greyed out, which we can't accomplish without
    // replacing the system-wide rename processor.  And we don't want to do that because we will
    // conflict with any other plugin that may want to do so.  (A chaining protocol would have been
    // nice here...)
    //
    if (!canBeRenamed(element)) {
      return null;
    }

    PsiElement substitute = super.substituteElementToRename(element, editor);
    return canBeRenamed(substitute) ? substitute : null;
  }

  @Override
  public void renameElement(@NotNull PsiElement element,
                            @NotNull String newName,
                            @NotNull UsageInfo[] usages,
                            @Nullable RefactoringElementListener listener) throws IncorrectOperationException {
    // Here, the element is actually being renamed.  If it shouldn't be renamed, then we'll just skip it.
    // We have to do so, because the element to be renamed is still on the list of elements due to
    // the PomRenameProcessor stating that it could handle the rename after we've already substituted the
    // proper elements.  But since we are handling it anyway the PomRenameProcessor is cut out of the loop.
    if (canBeRenamed(element)) {
      super.renameElement(element, newName, usages, listener);
    }
  }

  @Override
  public void prepareRenaming(@NotNull PsiElement element, @NotNull String newName, @NotNull Map<PsiElement, String> allRenames) {
    super.prepareRenaming(element, newName, allRenames);
  }

  @Override
  public void prepareRenaming(@NotNull PsiElement element,
                              @NotNull String newName,
                              @NotNull Map<PsiElement, String> allRenames,
                              @NotNull SearchScope scope) {
    super.prepareRenaming(element, newName, allRenames, scope);
  }
}
