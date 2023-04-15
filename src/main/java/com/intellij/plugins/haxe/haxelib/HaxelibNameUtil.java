/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017 Eric Bishton
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by as3boyan on 31.10.14.
 */
public final class HaxelibNameUtil {

  private final static String managedPrefix = "haxelib";
  private final static String joiner = "|";
  private final static String startsWith = managedPrefix + joiner;

  public static String stringifyHaxelib(HaxelibItem haxelibItem) {
    ArrayList<String> strings = new ArrayList<String>();
    strings.add(managedPrefix);
    strings.add(haxelibItem.getUrl());
    return Joiner.on(joiner).join(strings);
  }

  private static StringBuilder gSB = new StringBuilder(startsWith);
  public static String stringifyHaxelib(String path) {
    synchronized(gSB) {
      gSB.setLength(8); // re-initialize.
      gSB.append(path);
      return gSB.toString();
    }
  }

  @Nullable
  public static String parseHaxelib(@Nullable String data) {

    // XXX: ?? May be faster with if(isManagedLibraray(data)) return data.substr(startsWith.length, data.length); ??

    List<String> strings = StringUtil.split(data, joiner);

    if (strings.size() == 2 && strings.get(0).equals(managedPrefix)) {
      return strings.get(1);
    }

    return data;
  }

  /**
   * Determine whether the given name follows the 'managed' file name
   * convention.  See managedPrefix above for the current format.
   *
   * @param name to parse.
   * @return true if the given name follows the 'managed' file name convention,
   *         false otherwise.
   */
  public static boolean isManagedLibrary(String name) {
    return name.startsWith(startsWith); // null != parseHaxelib(data);
  }

  /**
   * Parse the URL to retrieve the class name.
   *
   * @param classpathUrl URL to parse.
   * @return the class name, or an empty string, if not found.
   */
  @Nullable
  public static String parseHaxelibNameFromPath(@NotNull String classpathUrl) {

    // TODO: Jettison this or fix it for the case where the path has extra directories attached to the end.

    String libName = null;
    String[] pieces = classpathUrl.split("/");
    if (pieces.length >= 2) {
      String versionString = pieces[pieces.length - 1];
      if (versionString.matches("[0-9],[0-9],[0-9]")) {
        libName = pieces[pieces.length - 2];
      }
    }
    return libName;
  }

}