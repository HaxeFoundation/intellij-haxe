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

import com.intellij.plugins.haxe.lang.psi.HaxePropertyAccessor;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

public enum HaxeAccessorType {
  DEFAULT("default"),
  NULL("null"),
  GET("get"),
  SET("set"),
  PRIVATE_GET("private get"),
  PRIVATE_SET("private set"),
  DYNAMIC("dynamic"),
  NEVER("never"),
  INVALID(""),
  ;

  public final String text;

  HaxeAccessorType(String text) {
    this.text = text;
  }

  /** Creates an {@code HaxeAccessorType} from a stub-stored text value. Returns {@link #INVALID} for {@code null}. */
  public static HaxeAccessorType from(@Nullable String text) {
    if (text == null) return INVALID;
    return fromString(text);
  }

  private static HaxeAccessorType fromString(String text) {
    for (HaxeAccessorType type : HaxeAccessorType.values()) {
      if (type.text.equals(text)) {
        return type;
      }
    }
    return HaxeAccessorType.INVALID;
  }

  public boolean isAllowedFromInside() {
    return (this != NEVER);
  }

  public boolean isAllowedFromOutside() {
    return (this != NEVER) && (this != NULL) && notPrivate();
  }

  private boolean notPrivate() {
    return (this != PRIVATE_GET) && (this != PRIVATE_SET);
  }

  public static HaxeAccessorType fromPsi(PsiElement psi) {
    if(psi instanceof HaxePropertyAccessor propertyAccessor) {
      String rawText = propertyAccessor.getText();
      String maxOneWhitespace = rawText.replaceAll("\\s+", " ");
      return fromString(maxOneWhitespace);
    }
    return INVALID;
  }

  public boolean isValidGetOrSetAccessor() {
    return this == DEFAULT || this == NULL || this == DYNAMIC || this == NEVER;
  }

  public boolean isValidGetAccessor() {
    return isValidGetOrSetAccessor() || this == GET || this == PRIVATE_GET;
  }
  public boolean isGetter() {
    return  this == GET || this == PRIVATE_GET;
  }
  public boolean isSetter() {
    return  this == SET || this == PRIVATE_SET;
  }

  public boolean isValidSetAccessor() {
    return isValidGetOrSetAccessor() || this == SET || this == PRIVATE_SET;
  }
}
