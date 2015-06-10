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
package com.intellij.plugins.haxe.haxelib;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A single item in a class path, originating from an invocation of the
 * 'haxelib' command.
 *
 * Possible extended functionality: Be able to create an item from a name,
 *      especially a managed library.
 */
public class HaxelibItem extends HaxeClasspathEntry {

  Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.haxelib.HaxelibItem");

  public static final List<HaxelibItem> EMPTY_LIST = new ArrayList<HaxelibItem>(0);

  public HaxelibItem(@NotNull String name, @NotNull String classpathUrl) {
    super(name, classpathUrl);
  }

  public HaxelibItem(@NotNull String classpathUrl) {
    super(null, classpathUrl);

    // XXX: Can we just steal the last part of the url path as the name?
    myName = HaxelibParser.parseHaxelibNameFromPath(classpathUrl);
  }
}