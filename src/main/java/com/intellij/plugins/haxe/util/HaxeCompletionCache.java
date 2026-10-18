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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by as3boyan on 15.11.14.
 */
public final class HaxeCompletionCache {

  // a "@:name : description" line of haxe --help-metas
  public static final Pattern META_TAG_PATTERN = Pattern.compile("@:([^\\r\\n\\t\\s]+[^:]+):[\\t\\s]+([^\\r\\n]+)");
  // a "name : description" line of haxe --help-defines
  public static final Pattern DEFINE_PATTERN = Pattern.compile("([^\\r\\n\\t\\s]+[^:]+):[\\t\\s]([^\\r\\n]+)");

  /** One cache per compiler executable, complete before it is published; modules on different SDKs see their own lists. */
  private static final Map<String, HaxeCompletionCache> INSTANCES = new ConcurrentHashMap<>();
  private static final HaxeCompletionCache EMPTY = new HaxeCompletionCache(List.of(), List.of());

  private final List<HXMLCompletionItem> metaTags;
  private final List<HXMLCompletionItem> defines;

  private HaxeCompletionCache(@NotNull List<HXMLCompletionItem> metaTags, @NotNull List<HXMLCompletionItem> defines) {
    this.metaTags = metaTags;
    this.defines = defines;
  }

  public List<HXMLCompletionItem> getMetaTags() {
    return metaTags;
  }

  public List<HXMLCompletionItem> getDefines() {
    return defines;
  }

  @NotNull
  public static HaxeCompletionCache getInstance(@NotNull Module module) {
    if (ApplicationManager.getApplication().isUnitTestMode()) return EMPTY;
    String haxePath = HaxeHelpUtil.getHaxePath(module);
    HaxeSdkAdditionalDataBase haxeSdkData = HaxeSdkUtilBase.getSdkData(module);
    return INSTANCES.computeIfAbsent(haxePath, path -> load(path, haxeSdkData));
  }

  @NotNull
  private static HaxeCompletionCache load(@NotNull String haxePath, @Nullable HaxeSdkAdditionalDataBase haxeSdkData) {
    List<HXMLCompletionItem> metaTags = probe(haxePath, "--help-metas", META_TAG_PATTERN, haxeSdkData);
    List<HXMLCompletionItem> defines = probe(haxePath, "--help-defines", DEFINE_PATTERN, haxeSdkData);
    return new HaxeCompletionCache(metaTags, defines);
  }

  /** The items of one help listing: each line the pattern matches, as (name, description). */
  @NotNull
  private static List<HXMLCompletionItem> probe(@NotNull String haxePath, @NotNull String helpFlag, @NotNull Pattern linePattern,
                                                @Nullable HaxeSdkAdditionalDataBase haxeSdkData) {
    List<String> lines = HaxelibCommandUtils.getProcessStdout(new ArrayList<>(List.of(haxePath, helpFlag)), haxeSdkData);
    List<HXMLCompletionItem> items = new ArrayList<>();
    for (String line : lines) {
      Matcher matcher = linePattern.matcher(line);
      if (matcher.find()) {
        items.add(new HXMLCompletionItem(matcher.group(1).trim(), matcher.group(2).trim()));
      }
    }
    return List.copyOf(items);
  }
}
