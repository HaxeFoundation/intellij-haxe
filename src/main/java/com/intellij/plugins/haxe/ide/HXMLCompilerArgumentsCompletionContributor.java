/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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
package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.plugins.haxe.haxelib.HaxelibCache;
import com.intellij.plugins.haxe.haxelib.HaxelibCommandUtils;
import com.intellij.plugins.haxe.hxml.HXMLLanguage;
import com.intellij.plugins.haxe.util.HaxeHelpUtil;
import com.intellij.plugins.haxe.hxml.psi.HXMLTypes;
import com.intellij.plugins.haxe.util.HaxeSdkUtilBase;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by as3boyan on 10.08.14.
 */
public class HXMLCompilerArgumentsCompletionContributor extends CompletionContributor {

  public static List<HXMLCompletionItem> COMPILER_ARGUMENTS =  new ArrayList<>();;
  public static List<HXMLCompletionItem> COMPILER_ARGUMENTS2 =  new ArrayList<>();;
  public static final Pattern HAXE3_PATTERN = Pattern.compile("-([a-z-_0-9]+)[\\s](<[^>]+>)?[^:]+:[\\t\\s]+([^\\r\\n]+)");
  public static final Pattern HAXE3_PATTERN2 = Pattern.compile("--([a-z-_0-9]+)[^:]+:[\\t\\s]+([^\\r\\n]+)");

  public static final Pattern HAXE4_PATTERN = Pattern.compile("(-((?<short>([a-z_0-9]+)),\\s))?--(?<long>[a-z-_0-9]+)[\\s](?<param><[^>]+>)?\\s*(?<param2>\\[.*\\])?\\s+[\\t\\s]+(?<description>[^\\r\\n]+)", Pattern.CASE_INSENSITIVE);

  private void getCompilerArguments() {
    ArrayList<String> commandLine = new ArrayList<>();
    List<String> strings = getStrings(commandLine);

    addHaxe4CompilerArguments(strings);
    addHaxe3CompilerArguments(strings);
  }

  private void addHaxe3CompilerArguments(List<String> strings) {
    for (int i = 0; i < strings.size(); i++) {
      String text = strings.get(i);
      Matcher matcher = HAXE3_PATTERN2.matcher(text);

      if (matcher.find()) {
        String group = matcher.group(1);

        if (!COMPILER_ARGUMENTS2.contains(group)) {
          COMPILER_ARGUMENTS2.add(new HXMLCompletionItem(group, matcher.group(2)));
        }
      }
      else
      {
        matcher = HAXE3_PATTERN.matcher(text);

        if (matcher.find()) {
          String group = matcher.group(1);

          if (!COMPILER_ARGUMENTS.contains(group)) {
            String description = matcher.group(3);
            String group2 = matcher.group(2);
            if (group2 != null) {
              group2 = group + " " + group2;
            }
            COMPILER_ARGUMENTS.add(new HXMLCompletionItem(group, description, group2));
          }
        }
      }
    }

    if (!COMPILER_ARGUMENTS.contains("D")) {
      COMPILER_ARGUMENTS.add(new HXMLCompletionItem("D"));
    }
  }
  private void addHaxe4CompilerArguments(List<String> strings) {
    for (int i = 0; i < strings.size(); i++) {
      String text = strings.get(i);

      Matcher matcher = HAXE4_PATTERN.matcher(text);

        if (matcher.find()) {
          String shortCmd = getTextFromGroup(matcher, "short");
          String longCmd = getTextFromGroup(matcher, "long");

          if (shortCmd != null && !COMPILER_ARGUMENTS.contains(shortCmd)) {
            String description = getTextFromGroup(matcher, "description");
            StringJoiner presentation = new StringJoiner(" ");
            String params = getTextFromGroup(matcher, "params");
            String params2 = getTextFromGroup(matcher, "params2");
            presentation.add(shortCmd);
            if (params != null)  presentation.add(params);
            if (params2 != null)  presentation.add(params2);
            COMPILER_ARGUMENTS.add(new HXMLCompletionItem(shortCmd, description,  presentation.toString()));
          }

          if (longCmd != null && !COMPILER_ARGUMENTS2.contains(longCmd)) {
            String description = getTextFromGroup(matcher, "description");
            StringJoiner presentation = new StringJoiner(" ");
            String params = getTextFromGroup(matcher, "params");
            String params2 = getTextFromGroup(matcher, "params2");
            presentation.add(longCmd);
            if (params != null)  presentation.add(params);
            if (params2 != null)  presentation.add(params2);

            COMPILER_ARGUMENTS2.add(new HXMLCompletionItem(longCmd, description, presentation.toString()));
          }
        }
      }
  }

  private String getTextFromGroup(Matcher matcher, String groupName) {
    try {
      return matcher.group(groupName);
    }catch (IllegalArgumentException e) {
      // group not found
      return null;
    }
  }

  private List<String> getStrings(ArrayList<String> commandLine) {
    Module module = HaxelibCache.getHaxeModule();
    commandLine.add(HaxeHelpUtil.getHaxePath(module));
    commandLine.add("--help");

    List<String> strings = HaxelibCommandUtils.getProcessStdout(commandLine, HaxeSdkUtilBase.getSdkData(module));
    if (strings.size() > 0) {
      strings.remove(0);
    }
    return strings;
  }

  public HXMLCompilerArgumentsCompletionContributor() {
    if (COMPILER_ARGUMENTS.isEmpty()) {
      getCompilerArguments();
    }
    extend(CompletionType.BASIC, PlatformPatterns.psiElement(HXMLTypes.KEY_TOKEN).withLanguage(HXMLLanguage.INSTANCE),
           new CompletionProvider<CompletionParameters>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet set) {


               //String[] compilerArguments;


               //compilerArguments = new String[]{
               //  "lib",
               //  "D",
               //  "cp",
               //  "main",
               //  "dce
               //};

               String text = parameters.getPosition().getText();

               if (text.startsWith("--")) {
                 for (HXMLCompletionItem argument : COMPILER_ARGUMENTS2) {
                   set.addElement(LookupElementBuilder.create(argument.name).withTailText(" " + argument.description, true));
                 }
               }
               else {
                 for (HXMLCompletionItem argument : COMPILER_ARGUMENTS) {
                   LookupElementBuilder lookupElementBuilder = LookupElementBuilder.create(argument.name).withTailText(" " + argument.description, true);
                   if (argument.presentableText != null) {
                     lookupElementBuilder = lookupElementBuilder.withPresentableText(argument.presentableText);
                   }
                   set.addElement(lookupElementBuilder);
                 }
               }
             }
           }
    );
  }
}
