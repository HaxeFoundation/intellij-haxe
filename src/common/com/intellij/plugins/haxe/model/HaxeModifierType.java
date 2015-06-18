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

public enum HaxeModifierType {
  OVERRIDE("override"),
  STATIC("static"),
  PUBLIC("public"),
  PRIVATE("private"),
  EMPTY(""),
  INLINE("inline"),
  FINAL("@:final"),
  IS_VAR("@:isVar"),
  DEPRECATED("@:deprecated");


  public String s;

  HaxeModifierType(String s) {
    this.s = s;
  }

  public String getStringWithSpace() {
    return (this.s.length() == 0) ? "" : (this.s + " ");
  }

  public int getVisibilityValue() {
    switch (this) {
      case PUBLIC:
        return 1;
      case PRIVATE:
        return 0;
      case EMPTY:
        return 0;
    }
    return -1;
  }

  public boolean hasLowerVisibilityThan(HaxeModifierType that) {
    return this.getVisibilityValue() < that.getVisibilityValue();
  }
}
