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
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

import static com.intellij.patterns.StandardPatterns.not;
import static com.intellij.patterns.StandardPatterns.string;

/**
 * Created by as3boyan on 15.11.14.
 */
@CustomLog
public class HXMLHaxelibCompletionContributor extends CompletionContributor {


  public HXMLHaxelibCompletionContributor() {

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

        VirtualFile file = parameters.getEditor().getVirtualFile();
        Project project = parameters.getEditor().getProject();
        if(project == null) {
          log.error("Unable to provide completion, Project is null");
          return;
        }
        Module module = ModuleUtil.findModuleForFile(file, project);
        HaxelibCacheManager cacheManager = HaxelibCacheManager.getInstance(module);

        Set<String>  availableHaxelibs = cacheManager.getAvailableLibraries().keySet();
        Set<String>  localHaxelibs = cacheManager.getInstalledLibraries().keySet();


          for (String libName : localHaxelibs) {
            result.addElement(LookupElementBuilder.create(libName).withTailText(" installed", true));
          }

          for (String libName : availableHaxelibs) {
            result.addElement(LookupElementBuilder.create(libName).withTailText(" available at haxelib", true));
          }
      }
    };
  }
  private CompletionProvider<CompletionParameters> getVersionProvider() {
    return new CompletionProvider<>() {
      @Override
      protected void addCompletions(@NotNull CompletionParameters parameters,
                                    ProcessingContext context,
                                    @NotNull CompletionResultSet result) {

        VirtualFile file = parameters.getEditor().getVirtualFile();
        Project project = parameters.getEditor().getProject();
        if (project == null) {
          log.error("Unable to provide completion, Project is null");
          return;
        }
        String text = parameters.getOriginalPosition().getText();
        String libName = text.substring(0,text.indexOf(":")).trim();
        Module module = ModuleUtil.findModuleForFile(file, project);
        HaxelibCacheManager cacheManager = HaxelibCacheManager.getInstance(module);

        List<String> availableVersions = cacheManager.fetchAvailableVersions(libName);
        List<String> installedVersions = cacheManager.getInstalledLibraries().getOrDefault(libName, List.of());

        for (String version : installedVersions) {
          result.addElement(LookupElementBuilder.create(libName+":"+version).withTailText(" installed", true));
        }

        for (String version : availableVersions) {
          result.addElement(LookupElementBuilder.create(libName+":"+version).withTailText(" available at haxelib", true));
        }
      }
    };
  }

}
