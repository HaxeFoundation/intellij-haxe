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
import com.intellij.execution.process.BaseOSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.openapi.util.Key;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.plugins.haxe.haxelib.HaxelibCache;
import com.intellij.plugins.haxe.haxelib.HaxelibManager;
import com.intellij.plugins.haxe.hxml.HXMLLanguage;
import com.intellij.plugins.haxe.hxml.psi.HXMLTypes;
import com.intellij.plugins.haxe.util.HaxeHelpUtil;
import com.intellij.util.ProcessingContext;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by as3boyan on 10.08.14.
 */
public class HXMLCompilerArgumentsCompletionContributor extends CompletionContributor {

  public static List<String> COMPILER_ARGUMENTS = null;
  public static final Pattern PATTERN = Pattern.compile("--?([a-z-_0-9]+)");

  private List<String> getCompilerArguments() {
    List<String> compilerArguments = new ArrayList<String>();
    ArrayList<String> commandLine = new ArrayList<String>();
    commandLine.add(HaxeHelpUtil.getHaxePath(HaxelibCache.getHaxeModule()));
    commandLine.add("--help");

    List<String> strings = HaxelibManager.getProcessStderr(commandLine);
    if (strings.size() > 0) {
      strings.remove(0);
    }

    for (int i = 0; i < strings.size(); i++) {
      String text = strings.get(i);
      Matcher matcher = PATTERN.matcher(text);

      if (matcher.find()) {
        String group = matcher.group(1);

        if (!compilerArguments.contains(group)) {
          compilerArguments.add(group);
        }
      }
    }

    if (!compilerArguments.contains("D")) {
      compilerArguments.add("D");
    }

    return compilerArguments;
  }

  public HXMLCompilerArgumentsCompletionContributor() {
    if (COMPILER_ARGUMENTS == null) {
      COMPILER_ARGUMENTS = getCompilerArguments();
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

               if (COMPILER_ARGUMENTS != null) {
                 for (String argument : COMPILER_ARGUMENTS) {
                   set.addElement(LookupElementBuilder.create(argument));
                 }
               }
             }
           }
    );
  }
}
