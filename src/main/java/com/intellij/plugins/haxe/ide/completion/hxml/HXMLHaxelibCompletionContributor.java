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
package com.intellij.plugins.haxe.ide.completion.hxml;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupElementWeigher;
import com.intellij.codeInsight.lookup.WeighingContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.plugins.haxe.buildsystem.hxml.HXMLLanguage;
import com.intellij.plugins.haxe.haxelib.HaxelibCacheManager;
import com.intellij.plugins.haxe.hxml.psi.HXMLLib;
import com.intellij.plugins.haxe.hxml.psi.HXMLValue;
import com.intellij.util.ProcessingContext;
import lombok.CustomLog;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.patterns.StandardPatterns.not;
import static com.intellij.patterns.StandardPatterns.string;

/**
 * Created by as3boyan on 15.11.14.
 */
@CustomLog
public class HXMLHaxelibCompletionContributor extends CompletionContributor {


  public HXMLHaxelibCompletionContributor() {
    // completion without any text
    extend(CompletionType.BASIC,
           psiElement()
             .withSuperParent(2, HXMLValue.class)
             .withSuperParent(3, HXMLLib.class)
             .withLanguage(HXMLLanguage.INSTANCE),
           getNameProvider());
    // completion with
    extend(CompletionType.BASIC,
           PlatformPatterns.psiElement()
             .withParent(HXMLValue.class)
             .withSuperParent(2, HXMLLib.class)
             .withText(not(string().contains(":"))) // lib name and version separated by colon
             .withLanguage(HXMLLanguage.INSTANCE),
           getNameProvider());

    extend(CompletionType.BASIC,
           PlatformPatterns.psiElement()
             .withParent(HXMLValue.class)
             .withSuperParent(2, HXMLLib.class)
             .withText(string().contains(":")) // lib name and version separated by colon
             .withLanguage(HXMLLanguage.INSTANCE),
           getVersionProvider());
  }


  @NotNull
  private CompletionProvider<CompletionParameters> getNameProvider() {

    return new CompletionProvider<>() {
      @Override
      protected void addCompletions(@NotNull CompletionParameters parameters,
                                    ProcessingContext context,
                                    @NotNull CompletionResultSet result) {

        //VirtualFile file = parameters.getEditor().getVirtualFile();
        VirtualFile file = parameters.getOriginalFile().getVirtualFile();
        Project project = parameters.getEditor().getProject();
        if(project == null) {
          log.error("Unable to provide completion, Project is null");
          return;
        }
        Module module = ModuleUtil.findModuleForFile(file, project);
        HaxelibCacheManager cacheManager = HaxelibCacheManager.getInstance(module);

        Set<String>  available = cacheManager.getAvailableLibraries().keySet();
        Set<String>  installed = cacheManager.getInstalledLibraries().keySet();

        List<LookupElementBuilder> installedSuggestions = installed.stream()
          .map(libName -> LookupElementBuilder.create(libName).withTailText(" installed", true))
          .toList();

        List<LookupElementBuilder> availableSuggestions = available.stream()
          .map(libName -> LookupElementBuilder.create(libName).withTailText(" available at haxelib", true))
          .toList();

        result.addAllElements(installedSuggestions);
        result.addAllElements(availableSuggestions);

      }
    };
  }
  private CompletionProvider<CompletionParameters> getVersionProvider() {
    return new CompletionProvider<>() {
      @Override
      protected void addCompletions(@NotNull CompletionParameters parameters,
                                    ProcessingContext context,
                                    @NotNull CompletionResultSet result) {

        //VirtualFile file = parameters.getEditor().getVirtualFile();   // TODO use this when Android studio switches to 2023.x
        VirtualFile file = parameters.getOriginalFile().getVirtualFile();
        Project project = parameters.getEditor().getProject();
        if (project == null) {
          log.error("Unable to provide completion, Project is null");
          return;
        }
        String text = parameters.getOriginalPosition().getText();
        String libName = text.substring(0,text.indexOf(":")).trim();
        Module module = ModuleUtil.findModuleForFile(file, project);
        HaxelibCacheManager cacheManager = HaxelibCacheManager.getInstance(module);

        List<String> available = cacheManager.fetchAvailableVersions(libName).stream()
                .sorted(Comparator.reverseOrder())
                .toList();

        List<String> installed = cacheManager.getInstalledLibraries().getOrDefault(libName, Set.of()).stream()
                .sorted(Comparator.reverseOrder())
                .toList();

        List<LookupElementBuilder> installedSuggestions = installed.stream()
          .map(version -> LookupElementBuilder.create(libName + ":" + version).withTailText(" installed", true))
          .toList();

          List<LookupElementBuilder> availableSuggestions = available.stream()
            .map(version -> LookupElementBuilder.create(libName+":"+version).withTailText(" available at haxelib", true))
            .toList();

        // reverse sorting as the latest version is usually more relevant thant the earliest
        result = reverseSort(result);

        result.addAllElements(installedSuggestions);
        result.addAllElements(availableSuggestions);


      }
    };
  }

  private static @NonNull CompletionResultSet reverseSort(@NonNull CompletionResultSet result) {
    LookupElementWeigher lookupElementWeigher = new LookupElementWeigher("ReverseHaxelibVersionWeigher", true, false) {
      @NotNull
      @Override
      public Comparable weigh(@NotNull LookupElement element) {
        return element.getLookupString();
      }
    };
    return result.withRelevanceSorter(CompletionSorter.emptySorter().weigh(lookupElementWeigher));
  }
}
