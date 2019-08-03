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
package com.intellij.plugins.haxe.ide.annotator;

import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.psi.PsiElement;

/**
 * A library of annotations that can be re-used.  Place annotations that are used more than
 * once (or should be) in this class.
 */
public class HaxeStandardAnnotation {

  private HaxeStandardAnnotation() {}

  public static HaxeAnnotation typeMismatch(PsiElement incompatibleElement, String incompatibleType, String correctType) {
    String message = HaxeBundle.message("haxe.semantic.incompatible.type.0.should.be.1", incompatibleType, correctType);

    return new HaxeAnnotation(HighlightSeverity.ERROR, incompatibleElement.getTextRange(), message, null);
  }

  public static HaxeAnnotation returnTypeMismatch(PsiElement incompatibleElement, String incompatibleType, String correctType) {
    String message = HaxeBundle.message("haxe.semantic.incompatible.return.type.0.should.be.1", incompatibleType, correctType);
    return new HaxeAnnotation(HighlightSeverity.ERROR, incompatibleElement.getTextRange(), message, null);
  }

}
