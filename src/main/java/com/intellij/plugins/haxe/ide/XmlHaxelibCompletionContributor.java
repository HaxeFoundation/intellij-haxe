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
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.XmlPatterns;
import com.intellij.plugins.haxe.haxelib.HaxelibCacheManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ProcessingContext;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Created by as3boyan on 15.11.14.
 */
@CustomLog
public class XmlHaxelibCompletionContributor extends CompletionContributor {

  public XmlHaxelibCompletionContributor() {

    extend(CompletionType.BASIC, PlatformPatterns.psiElement().inside(
             XmlPatterns.xmlAttributeValue().withParent(XmlPatterns.xmlAttribute("name"))
               .withSuperParent(2, XmlPatterns.xmlTag().withName("haxelib")).withLanguage(XMLLanguage.INSTANCE)),
           createNameCompletionProvider());
    extend(CompletionType.BASIC, PlatformPatterns.psiElement().inside(
             XmlPatterns.xmlAttributeValue().withParent(XmlPatterns.xmlAttribute("version"))
               .withSuperParent(2, XmlPatterns.xmlTag().withName("haxelib")).withLanguage(XMLLanguage.INSTANCE)),
           createVersionCompletionProvider());
  }

  private CompletionProvider<CompletionParameters> createVersionCompletionProvider() {
    return new CompletionProvider<>() {
      @Override
      protected void addCompletions(@NotNull CompletionParameters parameters,
                                    ProcessingContext context,
                                    @NotNull CompletionResultSet result) {
        VirtualFile file = parameters.getEditor().getVirtualFile();
        Project project = parameters.getEditor().getProject();
        if(project == null) {
          log.error("Unable to provide completion, Project is null");
          return;
        }

        String libName = extractLibName(parameters);
        if(libName == null) return; // if lib name not found do nothing

        Module moduleForFile = ModuleUtil.findModuleForFile(file, project);
        HaxelibCacheManager instance = HaxelibCacheManager.getInstance(moduleForFile);

        Set<String> availableVersions = instance.fetchAvailableVersions(libName);
        Set<String> installedVersions = instance.getInstalledLibraries().getOrDefault(libName, Set.of());
        availableVersions.removeAll(installedVersions); // avoid duplicates

        for (String libVersion : installedVersions) {
          result.addElement(LookupElementBuilder.create(libVersion).withTailText(" installed", true));
        }
        for (String libVersion : availableVersions) {
          result.addElement(LookupElementBuilder.create(libVersion).withTailText(" available at haxelib", true));
        }
      }
    };
  }

  @Nullable
  private static String extractLibName(@NotNull CompletionParameters parameters) {
    PsiElement position = parameters.getOriginalPosition();
    while(!(position instanceof XmlTag tag)) {
      position = position.getParent();
      if(position == null) return null;
    }

    return tag.getAttribute("name").getValue();
  }

  @NotNull
  private static CompletionProvider<CompletionParameters> createNameCompletionProvider() {
    return new CompletionProvider<>() {
      @Override
      protected void addCompletions(@NotNull CompletionParameters parameters,
                                    ProcessingContext context,
                                    @NotNull CompletionResultSet result) {
        VirtualFile file = parameters.getEditor().getVirtualFile();
        Project project = parameters.getEditor().getProject();
        if(project == null) {
          log.error("Unable to provide completion, Project is null");
          return;
        }
        Module moduleForFile = ModuleUtil.findModuleForFile(file, project);
        HaxelibCacheManager instance = HaxelibCacheManager.getInstance(moduleForFile);

        Set<String> available = instance.getAvailableLibraries().keySet();
        Set<String> installed = instance.getInstalledLibraries().keySet();

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
}
