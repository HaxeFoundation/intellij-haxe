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
package com.intellij.plugins.haxe.lang.psi;

import com.intellij.psi.PsiCodeFragment;
import com.intellij.psi.impl.PsiFileEx;

import java.util.Set;

/**
 * @author: Fedor.Korotkov
 */
public interface HaxeExpressionCodeFragment extends PsiFileEx, PsiCodeFragment {

  /**
   * Records an import (a fully-qualified type name) on the fragment itself,
   * separate from its text — so a class the context file does not import can
   * still resolve in the evaluate window without altering the expression that
   * gets evaluated. The Haxe analogue of {@link com.intellij.psi.PsiImportHolder#importClass}.
   *
   * @return {@code true} if the import was newly added.
   */
  boolean importClass(String qualifiedName);

  /** The fully-qualified type names imported into this fragment (see {@link #importClass}). */
  Set<String> getImportedTypeNames();
}
