/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
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
package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.lang.psi.HaxeImportStatementRegular;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import org.apache.commons.lang.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeImportModel {
  @NotNull
  public final HaxeImportStatementRegular regular;
  public String fqName;

  public HaxeImportModel(@NotNull HaxeImportStatementRegular regular) {
    this.regular = regular;
  }

  @Nullable
  public HaxeClassReferenceModel getImportedClassReference() {
    HaxeFileModel file = HaxeFileModel.fromElement(regular);

    HaxeClassReferenceModel reference = null;
    HaxeReferenceExpression expression = regular.getReferenceExpression();
    if (expression != null) {
      fqName = expression.getText();
      HaxeClassModel clazz = file.getProject().getClassFromFqName(fqName);
      if (clazz != null) {
        reference = new HaxeClassReferenceModel(expression, clazz.haxeClass);
      }
    }
    return reference;
  }

  @Nullable
  public HaxeClassModel getImportedClass() {
    HaxeClassReferenceModel reference = getImportedClassReference();
    return (reference != null) ? reference.getHaxeClass() : null;
  }
}
