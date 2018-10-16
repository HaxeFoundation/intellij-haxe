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
package com.intellij.plugins.haxe.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkAdditionalDataBase;
import com.intellij.plugins.haxe.haxelib.HaxelibCache;
import com.intellij.plugins.haxe.haxelib.HaxelibCommandUtils;
import com.intellij.plugins.haxe.ide.HaxeCompletionDefine;
import com.intellij.plugins.haxe.ide.HaxeCompletionMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by as3boyan on 15.11.14.
 */
public class HaxeHelpCache {
  static HaxeHelpCache instance = null;
  public static final Pattern META_TAG_PATTERN = Pattern.compile("@:([^\\r\\n\\t\\s]+)[^:]+:[\\t\\s]+([^\\r\\n]+)");
  public static final Pattern DEFINE_PATTERN = Pattern.compile("([^\\r\\n\\t\\s]+)[^:]+:[\\t\\s]([^\\r\\n]+)");

  public List<HaxeCompletionMeta> getMetaTags() {
    return metaTags;
  }
  public List<HaxeCompletionDefine> getDefines() {
    return defines;
  }

  private static List<HaxeCompletionMeta> metaTags;
  private static List<HaxeCompletionDefine> defines;

  public HaxeHelpCache() {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      metaTags = Collections.emptyList();
      defines = Collections.emptyList();
      return;
    }

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
    Module module = HaxelibCache.getHaxeModule();
    String haxePath = HaxeHelpUtil.getHaxePath(module);
    HaxeSdkAdditionalDataBase haxeSdkData = HaxeSdkUtilBase.getSdkData(module);

    commandLineArguments.add(haxePath);
    commandLineArguments.add("--help-metas");

    List<String> strings = HaxelibCommandUtils.getProcessOutput(commandLineArguments, haxeSdkData);

    metaTags = new ArrayList<>();

    for (int i = 0, size = strings.size(); i < size; i++) {
      String string = strings.get(i);
      Matcher matcher = META_TAG_PATTERN.matcher(string);

      if (matcher.find()) {
        metaTags.add(new HaxeCompletionMeta(matcher.group(1), matcher.group(2)));
      }
    }

    commandLineArguments.clear();
    commandLineArguments.add(haxePath);
    commandLineArguments.add("--help-defines");

    strings = HaxelibCommandUtils.getProcessOutput(commandLineArguments, haxeSdkData);

    defines = new ArrayList<>();

    for (int i = 0; i < strings.size(); i++) {
      String string = strings.get(i);
      Matcher matcher = DEFINE_PATTERN.matcher(string);

      if (matcher.find()) {
        defines.add(new HaxeCompletionDefine(matcher.group(1), matcher.group(2)));
      }
    }
  }
}
