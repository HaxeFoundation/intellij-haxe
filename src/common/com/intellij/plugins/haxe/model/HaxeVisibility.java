/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2016 AS3Boyan
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

public enum HaxeVisibility implements HaxeModifier {
  PUBLIC("public"), PRIVATE("private"), EMPTY("");

  public String name;

  HaxeVisibility(String name) {
    this.name = name;
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

  public boolean hasLowerVisibilityThan(HaxeVisibility that) {
    return this.getVisibilityValue() < that.getVisibilityValue();
  }

  public String getKeyword() {
    return this.name;
  }

  public String getKeywordWithSpace() {
    return (this.name.length() == 0) ? "" : (this.name + " ");
  }
}
