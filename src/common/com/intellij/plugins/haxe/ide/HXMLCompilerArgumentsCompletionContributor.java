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
package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.plugins.haxe.haxelib.HaxelibCache;
import com.intellij.plugins.haxe.haxelib.HaxelibCommandUtils;
import com.intellij.plugins.haxe.hxml.HXMLLanguage;
import com.intellij.plugins.haxe.hxml.psi.HXMLTypes;
import com.intellij.plugins.haxe.util.HaxeHelpUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by as3boyan on 10.08.14.
 */
public class HXMLCompilerArgumentsCompletionContributor extends CompletionContributor {

  public static List<HXMLCompletionItem> COMPILER_ARGUMENTS = null;
  public static List<HXMLCompletionItem> COMPILER_ARGUMENTS2 = null;
  public static final Pattern PATTERN = Pattern.compile("-([a-z-_0-9]+)[\\s](<[^>]+>)?[^:]+:[\\t\\s]+([^\\r\\n]+)");
  public static final Pattern PATTERN2 = Pattern.compile("--([a-z-_0-9]+)[^:]+:[\\t\\s]+([^\\r\\n]+)");

  private void getCompilerArguments() {
    List<HXMLCompletionItem> compilerArguments = new ArrayList<HXMLCompletionItem>();
    List<HXMLCompletionItem> compilerArguments2 = new ArrayList<HXMLCompletionItem>();
    ArrayList<String> commandLine = new ArrayList<String>();
    List<String> strings = getStrings(commandLine);

    Matcher matcher;
    for (int i = 0; i < strings.size(); i++) {
      String text = strings.get(i);
      matcher = PATTERN2.matcher(text);

      if (matcher.find()) {
        String group = matcher.group(1);

        if (!compilerArguments2.contains(group)) {
          compilerArguments2.add(new HXMLCompletionItem(group, matcher.group(2)));
        }
      }
      else
      {
        matcher = PATTERN.matcher(text);

        if (matcher.find()) {
          String group = matcher.group(1);

          if (!compilerArguments.contains(group)) {
            String description = matcher.group(3);
            String group2 = matcher.group(2);
            if (group2 != null) {
              group2 = group + " " + group2;
            }
            compilerArguments.add(new HXMLCompletionItem(group, description, group2));
          }
        }
      }
    }

    if (!compilerArguments.contains("D")) {
      compilerArguments.add(new HXMLCompletionItem("D"));
    }

    COMPILER_ARGUMENTS = compilerArguments;
    COMPILER_ARGUMENTS2 = compilerArguments2;
  }

  private List<String> getStrings(ArrayList<String> commandLine) {
    commandLine.add(HaxeHelpUtil.getHaxePath(HaxelibCache.getHaxeModule()));
    commandLine.add("--help");

    List<String> strings = HaxelibCommandUtils.getProcessStderr(commandLine);
    if (strings.size() > 0) {
      strings.remove(0);
    }
    return strings;
  }

  public HXMLCompilerArgumentsCompletionContributor() {
    if (COMPILER_ARGUMENTS == null) {
      getCompilerArguments();
    }
    extend(CompletionType.BASIC, PlatformPatterns.psiElement(HXMLTypes.KEY).withLanguage(HXMLLanguage.INSTANCE),
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
