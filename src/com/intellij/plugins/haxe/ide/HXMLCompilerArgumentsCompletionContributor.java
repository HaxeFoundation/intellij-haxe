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
import com.intellij.plugins.haxe.hxml.HXMLLanguage;
import com.intellij.plugins.haxe.hxml.psi.HXMLTypes;
import com.intellij.util.ProcessingContext;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by as3boyan on 10.08.14.
 */
public class HXMLCompilerArgumentsCompletionContributor extends CompletionContributor {
  private static final Set<String> compilerArguments = new THashSet<String>() {
    {
      //for (IElementType elementType : HaxeTokenTypeSets.KEYWORDS.getTypes()) {
      //  add(elementType.toString());
      //}
      //add(HaxeTokenTypes.ONEW.toString());

      //final ArrayList<String> compilerArguments;
      //compilerArguments = new ArrayList<String>();

      // TODO: get path to Haxe instead of using global alias

      try {
        ArrayList<String> commandLine = new ArrayList<String>();
        //final String sdkExePath = HaxeSdkUtilBase.getCompilerPathByFolderPath(context.getSdkHomePath());
        commandLine.add("haxe");
        commandLine.add("--help");

        BaseOSProcessHandler processHandler =
          new BaseOSProcessHandler(new ProcessBuilder(commandLine).start(), null, Charset.defaultCharset());

        processHandler.addProcessListener(new ProcessAdapter() {
          @Override
          public void onTextAvailable(ProcessEvent event, Key outputType) {
            String text = event.getText();
            Pattern pattern = Pattern.compile("-([a-z-_0-9]+)");
            Matcher matcher = pattern.matcher(text);

            while (matcher.find()) {
              String group = matcher.group();

              if (!contains(group)) {
                add(group);
              }
            }
          }

          @Override
          public void processTerminated(ProcessEvent event) {
            super.processTerminated(event);
          }
        });

        processHandler.startNotify();
        processHandler.waitFor();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }
  };

  public HXMLCompilerArgumentsCompletionContributor() {
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

               if (compilerArguments != null) {
                 for (String argument : compilerArguments) {
                   set.addElement(LookupElementBuilder.create(argument));
                 }
               }
             }
           }
    );
  }
}
