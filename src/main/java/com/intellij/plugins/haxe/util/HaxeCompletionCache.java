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
import java.util.Set;
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
  // executables whose listings a pooled thread is reading
  private static final Set<String> LOADING = ConcurrentHashMap.newKeySet();
  private static final HaxeCompletionCache EMPTY = new HaxeCompletionCache(List.of(), List.of(), List.of());

  private final List<HXMLCompletionItem> metaTags;
  private final List<HXMLCompletionItem> defines;
  private final List<String> compilerHelp;

  private HaxeCompletionCache(@NotNull List<HXMLCompletionItem> metaTags,
                              @NotNull List<HXMLCompletionItem> defines,
                              @NotNull List<String> compilerHelp) {
    this.metaTags = metaTags;
    this.defines = defines;
    this.compilerHelp = compilerHelp;
  }

  public List<HXMLCompletionItem> getMetaTags() {
    return metaTags;
  }

  public List<HXMLCompletionItem> getDefines() {
    return defines;
  }

  /** The lines of {@code haxe --help} after its banner; the hxml argument completion parses them. */
  public List<String> getCompilerHelp() {
    return compilerHelp;
  }

  /**
   * The listings of the module's compiler, or empty ones until they are read.
   * Completion runs under the read lock, where waiting for a process is not
   * allowed, so the first request reads them on a pooled thread and later
   * requests get them.
   */
  @NotNull
  public static HaxeCompletionCache getInstance(@NotNull Module module) {
    if (ApplicationManager.getApplication().isUnitTestMode()) return EMPTY;
    String haxePath = HaxeHelpUtil.getHaxePath(module);
    HaxeCompletionCache cached = INSTANCES.get(haxePath);
    if (cached != null) return cached;

    if (LOADING.add(haxePath)) {
      HaxeSdkAdditionalDataBase haxeSdkData = HaxeSdkUtilBase.getSdkData(module);
      ApplicationManager.getApplication().executeOnPooledThread(() -> loadInBackground(haxePath, haxeSdkData));
    }
    return EMPTY;
  }

  private static void loadInBackground(@NotNull String haxePath, @Nullable HaxeSdkAdditionalDataBase haxeSdkData) {
    try {
      INSTANCES.put(haxePath, load(haxePath, haxeSdkData));
    }
    finally {
      LOADING.remove(haxePath);
    }
  }

  @NotNull
  private static HaxeCompletionCache load(@NotNull String haxePath, @Nullable HaxeSdkAdditionalDataBase haxeSdkData) {
    List<HXMLCompletionItem> metaTags = probe(haxePath, "--help-metas", META_TAG_PATTERN, haxeSdkData);
    List<HXMLCompletionItem> defines = probe(haxePath, "--help-defines", DEFINE_PATTERN, haxeSdkData);
    List<String> help = helpListing(haxePath, "--help", haxeSdkData);
    List<String> compilerHelp = help.isEmpty() ? List.of() : List.copyOf(help.subList(1, help.size()));
    return new HaxeCompletionCache(metaTags, defines, compilerHelp);
  }

  /** The items of one help listing: each line the pattern matches, as (name, description). */
  @NotNull
  private static List<HXMLCompletionItem> probe(@NotNull String haxePath, @NotNull String helpFlag, @NotNull Pattern linePattern,
                                                @Nullable HaxeSdkAdditionalDataBase haxeSdkData) {
    List<String> lines = helpListing(haxePath, helpFlag, haxeSdkData);
    List<HXMLCompletionItem> items = new ArrayList<>();
    for (String line : lines) {
      Matcher matcher = linePattern.matcher(line);
      if (matcher.find()) {
        items.add(new HXMLCompletionItem(matcher.group(1).trim(), matcher.group(2).trim()));
      }
    }
    return List.copyOf(items);
  }

  /** The output of one help flag; haxe 3 prints {@code --help} on stderr, so both streams count. */
  @NotNull
  private static List<String> helpListing(@NotNull String haxePath, @NotNull String helpFlag,
                                          @Nullable HaxeSdkAdditionalDataBase haxeSdkData) {
    return HaxelibCommandUtils.getProcessStdout(new ArrayList<>(List.of(haxePath, helpFlag)), haxeSdkData);
  }
}
