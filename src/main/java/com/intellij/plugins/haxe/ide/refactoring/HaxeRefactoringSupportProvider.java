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
package com.intellij.plugins.haxe.ide.refactoring;

import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.plugins.haxe.ide.refactoring.extractInterface.ExtractInterfaceHandler;
import com.intellij.plugins.haxe.ide.refactoring.extractMethod.HaxeExtractMethodHandler;
import com.intellij.plugins.haxe.ide.refactoring.extractSuperclass.ExtractSuperclassHandler;
import com.intellij.plugins.haxe.ide.refactoring.introduceParameter.HaxeIntroduceParameterHandler;
import com.intellij.plugins.haxe.ide.refactoring.introduceVariable.HaxeIntroduceVariableHandler;
import com.intellij.plugins.haxe.ide.refactoring.introduceField.HaxeIntroduceConstantHandler;
import com.intellij.plugins.haxe.ide.refactoring.memberPullUp.HaxePullUpHandler;
import com.intellij.plugins.haxe.ide.refactoring.memberPushDown.HaxePushDownHandler;
import com.intellij.plugins.haxe.ide.refactoring.rename.HaxeConstructorCallInplaceRenameHandler;
import com.intellij.plugins.haxe.ide.refactoring.rename.HaxePropertyInplaceRenameHandler;
import com.intellij.plugins.haxe.ide.refactoring.rename.HaxeRenameProcessor;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedElement;
import com.intellij.plugins.haxe.model.HaxePropertyFamily;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.refactoring.RefactoringActionHandler;
import org.jetbrains.annotations.Nullable;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeRefactoringSupportProvider extends RefactoringSupportProvider {

  /**
   * Declarations visible only inside their block (locals, parameters, local
   * functions) are renamed in place as variables, since every usage is in
   * the current editor. {@code element} is the name under the caret when
   * rename starts on the declaration, and the declaring component when it
   * starts on a reference, since a reference resolves to the component.
   */
  @Override
  public boolean isInplaceRenameAvailable(PsiElement element, PsiElement context) {
    HaxeNamedElement name = nameOf(element);
    return name != null && isLocal(name);
  }

  /**
   * Members, module-level declarations and types are renamed in place as
   * members: the platform updates the current file while the user types and
   * the other files when the rename is committed; invoking rename a second
   * time opens the dialog. Two cases are left to handlers of their own. A
   * rename started on a constructor call goes to
   * {@link HaxeConstructorCallInplaceRenameHandler}, which renames the class.
   * A property that binds accessors, or one of those accessors, goes to
   * {@link HaxePropertyInplaceRenameHandler}, which first asks how far the
   * rename reaches.
   */
  @Override
  public boolean isMemberInplaceRenameAvailable(PsiElement element, PsiElement context) {
    HaxeNamedElement name = nameOf(element);
    return name != null
           && !isLocal(name)
           && HaxeRenameProcessor.canBeRenamed(element)
           && !HaxePropertyFamily.isPropertyOrAccessor(element);
  }

  /** The name element, whether the rename starts on it or on its component. */
  @Nullable
  private static HaxeNamedElement nameOf(PsiElement element) {
    if (element instanceof HaxeNamedElement name) return name;
    return element instanceof HaxeNamedComponent component ? component.getComponentName() : null;
  }

  private static boolean isLocal(HaxeNamedElement name) {
    return name.getUseScope() instanceof LocalSearchScope;
  }

  @Override
  public RefactoringActionHandler getIntroduceVariableHandler() {
    return new HaxeIntroduceVariableHandler();
  }

  @Override
  public @Nullable RefactoringActionHandler getIntroduceParameterHandler() {
    return new HaxeIntroduceParameterHandler();
  }

  @Nullable
  @Override
  public RefactoringActionHandler getExtractInterfaceHandler() {
    return new ExtractInterfaceHandler();
  }

  @Nullable
  @Override
  public RefactoringActionHandler getExtractSuperClassHandler() {
    return new ExtractSuperclassHandler();
  }

  @Nullable
  @Override
  public RefactoringActionHandler getExtractClassHandler() {
    return super.getExtractClassHandler();
  }

  @Nullable
  @Override
  public RefactoringActionHandler getExtractMethodHandler() {
    return new HaxeExtractMethodHandler();
  }

  @Nullable
  @Override
  public RefactoringActionHandler getIntroduceConstantHandler() {
    return new HaxeIntroduceConstantHandler();
  }

  @Nullable
  @Override
  public RefactoringActionHandler getPullUpHandler() {
    return new HaxePullUpHandler();
  }

  @Nullable
  @Override
  public RefactoringActionHandler getPushDownHandler() {
    return new HaxePushDownHandler();
  }
}