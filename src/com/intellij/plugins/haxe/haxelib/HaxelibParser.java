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

import com.google.common.base.Joiner;
import com.intellij.openapi.util.text.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by as3boyan on 31.10.14.
 */
public class HaxelibParser {
  public static String stringifyHaxelib(HaxelibItem haxelibItem) {
    ArrayList<String> strings = new ArrayList<String>();
    strings.add("haxelib");
    strings.add(haxelibItem.classpathUrl);
    return Joiner.on("|").join(strings);
  }

  public static StringBuilder gSB = new StringBuilder("haxelib|");
  public static String stringifyHaxelib(String path) {
    synchronized(gSB) {
      gSB.setLength(8); // re-initialize.
      gSB.append(path);
      return gSB.toString();
    }
  }

  public static HaxelibItem parseHaxelib(String data) {
    List<String> strings = StringUtil.split(data, "|");

    if (strings.size() == 2 && strings.get(0).equals("haxelib")) {
      return new HaxelibItem(strings.get(1));
    }

    return null;
  }
}