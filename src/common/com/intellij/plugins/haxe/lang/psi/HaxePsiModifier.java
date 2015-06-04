/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
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
package com.intellij.plugins.haxe.lang.psi;

import com.intellij.psi.PsiModifier;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NonNls;

/**
 * @author: Srikanth.Ganapavarapu
 */
public interface HaxePsiModifier extends PsiModifier {

  @NonNls String PUBLIC = "public";
  @NonNls String PRIVATE = "private";
  @NonNls String ABSTRACT = "abstract";

  @NonNls String INLINE = "inline";

  @NonNls String FINAL = "@:final";
  @NonNls String KEEP = "@:keep";
  @NonNls String COREAPI = "@:coreApi";
  @NonNls String BIND = "@:bind";
  @NonNls String HACK = "@:hack";

  @NonNls String MACRO = "@:macro";
  @NonNls String UNREFLECTIVE = "@:unreflective";

  @NonNls String NATIVE = "@:native";
  @NonNls String JSREQUIRE = "@:jsRequire";

  @NonNls String REQUIRE = "@:require";

  @NonNls String NS = "@:ns";
  @NonNls String META = "@:meta";
  @NonNls String BITMAP = "@:bitmap";
  @NonNls String FAKEENUM = "@:fakeEnum";

  @NonNls String BUILD = "@:build";
  @NonNls String AUTOBUILD = "@:autoBuild";

  @NonNls String[] MODIFIERS = {
    PUBLIC, PRIVATE, ABSTRACT, FINAL, KEEP, COREAPI, BIND, MACRO, HACK,
    REQUIRE, FAKEENUM, NATIVE, JSREQUIRE, BITMAP, NS, META, BUILD,
    AUTOBUILD, UNREFLECTIVE
  };

  @MagicConstant(stringValues = {
    PUBLIC, PRIVATE, ABSTRACT, FINAL, KEEP, COREAPI, BIND, MACRO, HACK,
    REQUIRE, FAKEENUM, NATIVE, JSREQUIRE, BITMAP, NS, META, BUILD,
    AUTOBUILD, UNREFLECTIVE
  })
  @interface ModifierConstant { }
}
