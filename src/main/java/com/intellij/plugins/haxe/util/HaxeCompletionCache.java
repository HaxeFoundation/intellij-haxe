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
import com.intellij.plugins.haxe.haxelib.HaxelibCommandUtils;
import com.intellij.plugins.haxe.ide.HXMLCompletionItem;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by as3boyan on 15.11.14.
 */
public class HaxeCompletionCache {
  static HaxeCompletionCache instance = null;
  public static final Pattern META_TAG_PATTERN = Pattern.compile("@:([^\\r\\n\\t\\s]+)[^:]+:[\\t\\s]+([^\\r\\n]+)");
  public static final Pattern DEFINE_PATTERN = Pattern.compile("([^\\r\\n\\t\\s]+)[^:]+:[\\t\\s]([^\\r\\n]+)");

  private static final List<HXMLCompletionItem> metaTags = new ArrayList<>();
  private static final List<HXMLCompletionItem> defines = new ArrayList<>();

  public List<HXMLCompletionItem> getMetaTags() {
    return metaTags;
  }
  public List<HXMLCompletionItem> getDefines() {
    return defines;
  }


  public static HaxeCompletionCache getInstance(Module module) {
    if (instance == null) {
      instance = new HaxeCompletionCache();
      if (!ApplicationManager.getApplication().isUnitTestMode()) {
        instance.load(module);
      }
    }

    return instance;
  }

  private void load(Module module) {
    ArrayList<String> commandLineArguments = new ArrayList<String>();
    String haxePath = HaxeHelpUtil.getHaxePath(module);
    HaxeSdkAdditionalDataBase haxeSdkData = HaxeSdkUtilBase.getSdkData(module);

    getMetasFromCompiler(commandLineArguments, haxePath, haxeSdkData);
    getDefinesFromCompiler(commandLineArguments, haxePath, haxeSdkData);
  }

  private static void getMetasFromCompiler(ArrayList<String> commandLineArguments, String haxePath, HaxeSdkAdditionalDataBase haxeSdkData) {
    commandLineArguments.add(haxePath);
    commandLineArguments.add("--help-metas");

    List<String> metaTagsFromCompiler = HaxelibCommandUtils.getProcessStdout(commandLineArguments, haxeSdkData);

    metaTags.clear();

    for (int i = 0, size = metaTagsFromCompiler.size(); i < size; i++) {
      String string = metaTagsFromCompiler.get(i);
      Matcher matcher = META_TAG_PATTERN.matcher(string);

      if (matcher.find()) {
        metaTags.add(new HXMLCompletionItem(matcher.group(1), matcher.group(2)));
      }
    }
  }

  private static void getDefinesFromCompiler(ArrayList<String> commandLineArguments, String haxePath, HaxeSdkAdditionalDataBase haxeSdkData) {
    commandLineArguments.clear();
    commandLineArguments.add(haxePath);
    commandLineArguments.add("--help-defines");

    List<String> definesFromCompiler = HaxelibCommandUtils.getProcessStdout(commandLineArguments, haxeSdkData);

    defines.clear();

    for (int i = 0; i < definesFromCompiler.size(); i++) {
      String string = definesFromCompiler.get(i);
      Matcher matcher = DEFINE_PATTERN.matcher(string);

      if (matcher.find()) {
        defines.add(new HXMLCompletionItem(matcher.group(1), matcher.group(2)));
      }
    }
  }
}
