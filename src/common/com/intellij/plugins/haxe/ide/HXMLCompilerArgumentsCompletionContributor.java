/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017 Eric Bishton
 * Copyright 2018 Ilya Malanin
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
package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.completion.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.plugins.haxe.HaxeLanguageLevel;
import com.intellij.plugins.haxe.haxelib.HaxelibCache;
import com.intellij.plugins.haxe.haxelib.HaxelibCommandUtils;
import com.intellij.plugins.haxe.haxelib.HaxelibSdkUtils;
import com.intellij.plugins.haxe.hxml.HXMLLanguage;
import com.intellij.plugins.haxe.hxml.psi.HXMLTypes;
import com.intellij.plugins.haxe.util.HaxeHelpUtil;
import com.intellij.plugins.haxe.util.HaxeSdkUtilBase;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.intellij.plugins.haxe.HaxeLanguageLevel.HAXE_4;

/**
 * Created by as3boyan on 10.08.14.
 */
public class HXMLCompilerArgumentsCompletionContributor extends CompletionContributor {

  private static List<HXMLCompletionArgument> arguments = null;
  private static HaxeLanguageLevel level = null;

  private static final Pattern PATTERN_HAXE3 = Pattern.compile("^\\s*(?<keys>-+([a-z\\-A-Z_0-9]+))\\s*(?<options>(\\[[^]]+]|(<[^>]+>))*)\\s*:\\s*(?<description>[^\\r\\n]+)");
  private static final Pattern PATTERN_HAXE4 = Pattern.compile("^\\s*(?<keys>((-{1,2})([a-z\\-A-Z_0-9]+)(, )?)+)(?<options>(\\s*(\\[.+]|<[^>]+>))*)\\s+(?<description>[^\\r\\n]+)");

  private List<HXMLCompletionArgument> getCompilerArguments() {
    HaxeLanguageLevel sdkLevel = HaxelibSdkUtils.getLanguageLevel(HaxelibCache.getHaxeModule());

    if (level == null || !level.equals(sdkLevel)) {
      level = sdkLevel;
      arguments = null;
    }

    if (arguments != null) return arguments;

    final List<String> strings = getCompilerHelpLines();
    arguments = new ArrayList<>();

    Pattern pattern = PATTERN_HAXE3;

    if (level == HAXE_4) pattern = PATTERN_HAXE4;

    for (String text : strings) {
      Matcher matcher = pattern.matcher(text);
      if (matcher.find()) {
        String keys = matcher.group("keys");
        String options = StringUtil.trim(matcher.group("options"));
        String description = StringUtil.trim(matcher.group("description"));

        for (String key : keys.split(", ")) {
          boolean isDoubleDash = key.startsWith("--");
          arguments.add(new HXMLCompletionArgument(isDoubleDash, key.substring(isDoubleDash ? 2 : 1).trim(), description, options));
        }
      }
    }
    return arguments;
  }

  private List<String> getCompilerHelpLines() {
    Module module = HaxelibCache.getHaxeModule();

    List<String> commandLine = new ArrayList<>();
    commandLine.add(HaxeHelpUtil.getHaxePath(module));
    commandLine.add("--help");

    List<String> result = HaxelibCommandUtils.getProcessOutput(commandLine, HaxeSdkUtilBase.getSdkData(module));
    if (result.size() > 0) {
      result.remove(0);
    }
    return result;
  }

  public HXMLCompilerArgumentsCompletionContributor() {
    extend(CompletionType.BASIC, PlatformPatterns.psiElement(HXMLTypes.KEY_TOKEN).withLanguage(HXMLLanguage.INSTANCE),
           new CompletionProvider<CompletionParameters>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet set) {
               String text = parameters.getPosition().getText();
               boolean doubleDash = text.startsWith("--");
               for (HXMLCompletionArgument argument : getCompilerArguments()) {
                 if (argument.doubleDash == doubleDash) {
                   set.addElement(argument.getLookupElement());
                 }
               }
             }
           }
    );
  }
}

