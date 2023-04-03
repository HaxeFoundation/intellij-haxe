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
package com.intellij.plugins.haxe.model.fixer;

import com.intellij.plugins.haxe.lang.psi.HaxeTypeTag;
import com.intellij.plugins.haxe.model.HaxeDocumentModel;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeTypeTagChangeFixer extends HaxeFixer {
  private HaxeTypeTag typeTag;
  private SpecificTypeReference result;

  public HaxeTypeTagChangeFixer(@Nullable String text,
                                @NotNull HaxeTypeTag typeTag,
                                @NotNull SpecificTypeReference result) {
    super(text == null ? "HaxeTypeTagChangeFixer" : text);
    this.typeTag = typeTag;
    this.result = result;
  }

  public HaxeTypeTagChangeFixer(@NotNull HaxeTypeTag typeTag, @NotNull SpecificTypeReference result) {
    this(null, typeTag, result);
  }

  @Override
  public void run() {
    HaxeDocumentModel.fromElement(typeTag).replaceElementText(
      typeTag,
      ":" + result.toStringWithoutConstant()
    );
  }
}
