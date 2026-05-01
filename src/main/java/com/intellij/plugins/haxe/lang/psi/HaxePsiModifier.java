/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
package com.intellij.plugins.haxe.lang.psi;

import com.intellij.psi.PsiModifier;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NonNls;

/**
 * @author: Srikanth.Ganapavarapu
 */
public interface HaxePsiModifier extends PsiModifier, HaxePsiCompositeElement {

  @NonNls String PUBLIC = "public";
  @NonNls String PRIVATE = "private";
  @NonNls String ABSTRACT = "abstract";
  @NonNls String EMPTY = "";

  @NonNls String INLINE = "inline";
  @NonNls String STATIC = "static";
  @NonNls String DYNAMIC = "dynamic";
  @NonNls String FINAL = "final";
  @NonNls String VAR = "var";
  @NonNls String OVERRIDE = "override";
  @NonNls String OVERLOAD = "overload";
  @NonNls String MACRO = "macro";
  @NonNls String EXTERN = "extern";

  @NonNls String FINAL_META = "@:final";
  @NonNls String INLINE_META = "@:inline";
  @NonNls String IS_VAR_META = "@:isVar";
  @NonNls String EXTERN_META = "@:extern";
  @NonNls String OVERLOAD_META = "@:overload";

  @NonNls String ENUM_META = "@:enum";
  @NonNls String ABSTRACT_META = "@:abstract";
  @NonNls String MACRO_META = "@:macro";
  @NonNls String NATIVE = "@:native";


  @MagicConstant(stringValues = {
    PUBLIC, PRIVATE, EMPTY, STATIC, FINAL, EXTERN, DYNAMIC, ABSTRACT, OVERRIDE, OVERLOAD, MACRO,
    FINAL_META,INLINE_META,  IS_VAR_META,   MACRO_META,  EXTERN_META, OVERLOAD_META,
    ENUM_META, ABSTRACT_META,  NATIVE, INLINE
  })
  @interface ModifierConstant {
  }

  @MagicConstant(stringValues = {PUBLIC, PRIVATE, ABSTRACT, EMPTY, INLINE, STATIC, DYNAMIC,
          FINAL, VAR, OVERRIDE, OVERLOAD, MACRO, EXTERN})
  public @interface KeywordConstant {

  }

  @MagicConstant(stringValues = {FINAL_META, INLINE_META, IS_VAR_META, EXTERN_META, OVERLOAD_META, ENUM_META, ABSTRACT_META, MACRO_META, NATIVE})
  public @interface MetaConstant {

  }


  static String getStringWithSpace(@ModifierConstant String modifier) {
    return (modifier.length() == 0) ? "" : (modifier + " ");
  }

  static int getVisibilityValue(@ModifierConstant String modifier) {
    switch (modifier) {
      case PUBLIC:
        return 1;
      case PRIVATE:
        return 0;
      case EMPTY:
        return 0;
    }
    return -1;
  }

  static boolean hasLowerVisibilityThan(@ModifierConstant String thisModifier, @ModifierConstant String thatModifier) {
    return getVisibilityValue(thisModifier) < getVisibilityValue(thatModifier);
  }
}
