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
import com.intellij.plugins.haxe.hxml.psi.HXMLTypes;
import com.intellij.plugins.haxe.hxml.psi.HXMLValue;
import com.intellij.util.ProcessingContext;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

import static com.intellij.patterns.PlatformPatterns.psiElement;

/**
 * Created by as3boyan on 15.11.14.
 */
@CustomLog
public class HXMLHaxelibCompletionContributor extends CompletionContributor {

  protected static Set<String> availableHaxelibs = Collections.emptySet();
  protected static Set<String> localHaxelibs = Collections.emptySet();

  public HXMLHaxelibCompletionContributor() {

    // intelliJ 2018 and older
    extend(CompletionType.BASIC, psiElement(HXMLTypes.VALUE)
             .withParent(HXMLLib.class)
             .withLanguage(HXMLLanguage.INSTANCE),
           getProvider());

    // intelliJ 2019 and newer
    extend(CompletionType.BASIC,
           PlatformPatterns.psiElement()
             .withParent(HXMLValue.class)
             .withSuperParent(2, HXMLLib.class)
             .withLanguage(HXMLLanguage.INSTANCE),
           getProvider());
  }

  @NotNull
  private CompletionProvider<CompletionParameters> getProvider() {

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
        getLatestFromCache(module);

        for (String libName : availableHaxelibs) {
          result.addElement(LookupElementBuilder.create(libName).withTailText(" available at haxelib", true));
        }

        for (String libName : localHaxelibs) {
          result.addElement(LookupElementBuilder.create(libName).withTailText(" installed", true));
        }
      }

      private void getLatestFromCache(Module module) {
        availableHaxelibs = HaxelibCacheManager.getInstance(module).getAvailableLibraries().keySet();
        localHaxelibs = HaxelibCacheManager.getInstance(module).getInstalledLibraries().keySet();
      }
    };
  }
}
