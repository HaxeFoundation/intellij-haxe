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
package com.intellij.plugins.haxe.ide.index;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.psi.PsiElement;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

/**
 * Wrapper class around HaxeInheritanceDefinitionsSearcher that conforms to the
 * abstract function interface for QueryExecutorBase, which changed between
 * IDEA versions 18.1 and 18.2. (This file is for versions 18.2 and greater.)
 */
@SuppressWarnings({"Duplicates"})
public class HaxeInheritanceDefinitionsSearchExecutor extends QueryExecutorBase<PsiElement, PsiElement> {

  public HaxeInheritanceDefinitionsSearchExecutor() {
    super(true); // Wrap in read action.
  }

  //@Override
  public void processQuery(@NotNull PsiElement queryParameters, @NotNull Processor<? super PsiElement> consumer) {
    //noinspection unchecked
    HaxeInheritanceDefinitionsSearcher.processQueryInternal(queryParameters, (Processor<PsiElement>)consumer);
  }
}
