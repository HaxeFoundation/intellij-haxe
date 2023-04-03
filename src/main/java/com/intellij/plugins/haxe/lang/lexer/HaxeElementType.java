/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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
package com.intellij.plugins.haxe.lang.lexer;

import com.intellij.plugins.haxe.metadata.HaxeMetadataLanguage;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class HaxeElementType extends IElementType {

  public HaxeElementType(@NotNull @NonNls String debugName) {
    super(debugName, HaxeLanguage.INSTANCE);
  }

  public static IElementType createToken(String name) {
    if ("EMBEDDED_META".equals(name)) {
      return new HaxeEmbeddedElementType(name, HaxeMetadataLanguage.INSTANCE);
    }
    return new HaxeElementType(name);
  }

  public String asCode() {
    // Because the debug string is pretty much what we want anyway,
    // we'll just use that for now.
    return super.toString();
  }
}
