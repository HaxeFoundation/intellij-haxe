/*
 * Copyright 2020 Eric Bishton
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
package com.intellij.plugins.haxe.metadata.psi.impl;

import com.intellij.plugins.haxe.lang.psi.HaxeMetadataType;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Hashtable;
import java.util.Objects;

public class HaxeMetadataTypeName {
  private static final Hashtable<String, HaxeMetadataTypeName> dictionary = new Hashtable<>();

  public final String name;

  public HaxeMetadataTypeName(@NotNull String name) {
    this.name = name;
    this.dictionary.put(name, this);
  }

  public static HaxeMetadataTypeName lookup(String name) {
    HaxeMetadataTypeName found = dictionary.get(name);
    return null != found ? found : HaxeMeta.CUSTOM;
  }

  public boolean matches(HaxeMetadataTypeName meta) {
    if (null == meta) return false;
    if (this == meta) return true;
    return matches(meta.name);
  }

  public boolean matches(HaxeMetadataType parsedType) {
    if (null == parsedType) return false;
    if (this == parsedType) return true; // optimize same object.
    return matches(parsedType.getText());
  }

  public boolean matches(String name) {
    return this.name.equals(name);
  }

  public static boolean matches(HaxeMetadataType leftType, HaxeMetadataType rightType) {
    if (null == leftType || null == rightType) return false;
    if (leftType == rightType) return true; // optimize same object.
    return matches(leftType.getText(), rightType.getText());
  }

  public static boolean matches(String typeLeft, String typeRight) {
    if (null == typeLeft || null == typeRight) return false;  // Both null do NOT match.
    if (typeLeft.isEmpty()) return false; // Empty also doesn't match.
    return typeLeft.equals(typeRight);
  }

  public String toString() {
    if (name.isEmpty()) {
      return "<un-named metadata type>";
    }
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HaxeMetadataTypeName name1 = (HaxeMetadataTypeName)o;
    return name.equals(name1.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }
}
