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
package com.intellij.plugins.haxe.util;

import com.intellij.plugins.haxe.haxelib.HaxelibCache;
import com.intellij.plugins.haxe.haxelib.HaxelibClasspathUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by as3boyan on 15.11.14.
 */
public class HaxeHelpCache {
  static HaxeHelpCache instance = null;
  public static final Pattern PATTERN = Pattern.compile("@:([^\\r\\n\\t\\s]+)");

  public List<String> getMetaTags() {
    return metaTags;
  }

  private static List<String> metaTags;

  public HaxeHelpCache() {
    load();
  }

  public static HaxeHelpCache getInstance() {
    if (instance == null) {
      instance = new HaxeHelpCache();
    }

    return instance;
  }

  private void load() {
    ArrayList<String> commandLineArguments = new ArrayList<String>();
    String haxePath = HaxeHelpUtil.getHaxePath(HaxelibCache.getHaxeModule());
    commandLineArguments.add(haxePath);
    commandLineArguments.add("--help-metas");

    List<String> strings = HaxelibClasspathUtils.getProcessStdout(commandLineArguments);

    metaTags = new ArrayList<String>();

    for (int i = 0, size = strings.size(); i < size; i++) {
      String string = strings.get(i);
      Matcher matcher = PATTERN.matcher(string);

      if (matcher.find()) {
        metaTags.add(matcher.group(1));
      }
    }
  }
}
