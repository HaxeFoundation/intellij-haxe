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
package com.intellij.plugins.haxe.ide.completion.hxml;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.plugins.haxe.buildsystem.hxml.HXMLLanguage;
import com.intellij.plugins.haxe.hxml.psi.HXMLTypes;
import com.intellij.plugins.haxe.ide.HXMLCompletionItem;
import com.intellij.plugins.haxe.util.HaxeCompletionCache;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.intellij.patterns.PlatformPatterns.psiElement;

/**
 * Created by as3boyan on 10.08.14.
 */
public class HXMLCompilerArgumentsCompletionContributor extends CompletionContributor {

  // a haxe 3 single-dash help line: "-name <param> : description" (name, optional <param>, description)
  public static final Pattern HAXE3_PATTERN = Pattern.compile("-([a-z-_0-9]+)[\\s](<[^>]+>)?[^:]+:[\\t\\s]+([^\\r\\n]+)");
  // a haxe 3 double-dash help line: "--name ... : description" (name, description)
  public static final Pattern HAXE3_PATTERN2 = Pattern.compile("--([a-z-_0-9]+)[^:]+:[\\t\\s]+([^\\r\\n]+)");
  // a haxe 4+ help line: optional "-short, ", then "--long", its <param> and [param2], then the description
  public static final Pattern HAXE4_PATTERN = Pattern.compile("(-((?<short>([a-z_0-9]+)),\\s))?--(?<long>[a-z-_0-9]+)[\\s](?<param><[^>]+>)?\\s*(?<param2>\\[.*\\])?\\s+[\\t\\s]+(?<description>[^\\r\\n]+)", Pattern.CASE_INSENSITIVE);

  /** The arguments one compiler's {@code --help} lists, in their single-dash ({@code -cp}) and double-dash ({@code --macro}) forms. */
  private record CompilerArguments(@NotNull List<HXMLCompletionItem> singleDash, @NotNull List<HXMLCompletionItem> doubleDash) {
  }

  /** The arguments the compiler of the file's module lists; none until its help listing is read. */
  @NotNull
  private static CompilerArguments compilerArguments(@NotNull CompletionParameters parameters) {
    VirtualFile file = parameters.getOriginalFile().getVirtualFile();
    Project project = parameters.getEditor().getProject();
    Module module = ModuleUtil.findModuleForFile(file, project);
    List<String> helpLines = HaxeCompletionCache.getInstance(module).getCompilerHelp();

    CompilerArguments arguments = new CompilerArguments(new ArrayList<>(), new ArrayList<>());
    addHaxe4CompilerArguments(helpLines, arguments);
    addHaxe3CompilerArguments(helpLines, arguments);
    return arguments;
  }

  private static void addHaxe3CompilerArguments(List<String> strings, CompilerArguments arguments) {
    for (int i = 0; i < strings.size(); i++) {
      String text = strings.get(i);
      Matcher matcher = HAXE3_PATTERN2.matcher(text);

      if (matcher.find()) {
        String group = matcher.group(1);

        if (!arguments.doubleDash().contains(group)) {
          arguments.doubleDash().add(new HXMLCompletionItem(group, matcher.group(2)));
        }
      }
      else
      {
        matcher = HAXE3_PATTERN.matcher(text);

        if (matcher.find()) {
          String group = matcher.group(1);

          if (!arguments.singleDash().contains(group)) {
            String description = matcher.group(3);
            String group2 = matcher.group(2);
            if (group2 != null) {
              group2 = group + " " + group2;
            }
            arguments.singleDash().add(new HXMLCompletionItem(group, description, group2));
          }
        }
      }
    }

    if (!arguments.singleDash().contains("D")) {
      arguments.singleDash().add(new HXMLCompletionItem("D"));
    }
  }
  private static void addHaxe4CompilerArguments(List<String> strings, CompilerArguments arguments) {
    for (int i = 0; i < strings.size(); i++) {
      String text = strings.get(i);

      Matcher matcher = HAXE4_PATTERN.matcher(text);

        if (matcher.find()) {
          String shortCmd = getTextFromGroup(matcher, "short");
          String longCmd = getTextFromGroup(matcher, "long");

          if (shortCmd != null && !arguments.singleDash().contains(shortCmd)) {
            String description = getTextFromGroup(matcher, "description");
            StringJoiner presentation = new StringJoiner(" ");
            String params = getTextFromGroup(matcher, "params");
            String params2 = getTextFromGroup(matcher, "params2");
            presentation.add(shortCmd);
            if (params != null)  presentation.add(params);
            if (params2 != null)  presentation.add(params2);
            arguments.singleDash().add(new HXMLCompletionItem(shortCmd, description,  presentation.toString()));
          }

          if (longCmd != null && !arguments.doubleDash().contains(longCmd)) {
            String description = getTextFromGroup(matcher, "description");
            StringJoiner presentation = new StringJoiner(" ");
            String params = getTextFromGroup(matcher, "params");
            String params2 = getTextFromGroup(matcher, "params2");
            presentation.add(longCmd);
            if (params != null)  presentation.add(params);
            if (params2 != null)  presentation.add(params2);

            arguments.doubleDash().add(new HXMLCompletionItem(longCmd, description, presentation.toString()));
          }
        }
      }
  }

  private static String getTextFromGroup(Matcher matcher, String groupName) {
    try {
      return matcher.group(groupName);
    }catch (IllegalArgumentException e) {
      // group not found
      return null;
    }
  }

  public static PsiElementPattern.Capture<PsiElement> emptyLinePattern() {
    return psiElement().with(new PatternCondition<PsiElement>("emptyLine") {
      @Override
      public boolean accepts(@NotNull PsiElement element, ProcessingContext context) {
        PsiDocumentManager docMgr = PsiDocumentManager.getInstance(element.getProject());
        Document document = docMgr.getDocument(element.getContainingFile().getOriginalFile());

        int offset = element.getTextOffset();
        int lineNumber = document.getLineNumber(offset);
        int lineStart = document.getLineStartOffset(lineNumber);
        int lineEnd = document.getLineEndOffset(lineNumber);

        String lineText = document.getText(new TextRange(lineStart, lineEnd));
        return lineText.trim().isEmpty();
      }
    });
  }

  public HXMLCompilerArgumentsCompletionContributor() {
    extend(CompletionType.BASIC, psiElement()
                    .withLanguage(HXMLLanguage.INSTANCE)
                    .and(emptyLinePattern()),
            new CompletionProvider<>() {
              @Override
              protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet set) {
                CompilerArguments arguments = compilerArguments(parameters);

                for (HXMLCompletionItem argument : arguments.doubleDash()) {
                  LookupElementBuilder element = LookupElementBuilder.create("--" +argument.name)
                          .withTailText(" " + argument.description, true);
                  set.addElement(element);
                }

                for (HXMLCompletionItem argument : arguments.singleDash()) {
                  LookupElementBuilder lookupElementBuilder = LookupElementBuilder.create("-"+argument.name)
                          .withTailText(" " + argument.description, true);
                  if (argument.presentableText != null) {
                    lookupElementBuilder = lookupElementBuilder.withPresentableText("-"+argument.presentableText);
                  }
                  set.addElement(lookupElementBuilder);
                }

              }
            }
    );

    extend(CompletionType.BASIC, psiElement(HXMLTypes.KEY_TOKEN).withLanguage(HXMLLanguage.INSTANCE),
           new CompletionProvider<>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet set)
             {
               CompilerArguments arguments = compilerArguments(parameters);

               String text = parameters.getPosition().getText();

               if (text.startsWith("--")) {
                 for (HXMLCompletionItem argument : arguments.doubleDash()) {
                   set.addElement(LookupElementBuilder.create(argument.name).withTailText(" " + argument.description, true));
                 }
               }
               else {
                 for (HXMLCompletionItem argument : arguments.singleDash()) {
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
