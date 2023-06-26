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
import com.intellij.plugins.haxe.hxml.psi.HXMLDefine;
import com.intellij.plugins.haxe.hxml.psi.HXMLTypes;
import com.intellij.plugins.haxe.hxml.psi.HXMLValue;
import com.intellij.plugins.haxe.util.HaxeCompletionCache;
import com.intellij.util.ProcessingContext;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

import java.util.List;


/**
 * Created by as3boyan on 19.11.14.
 */
@CustomLog
public class HXMLDefineCompletionContributor extends CompletionContributor {
  public HXMLDefineCompletionContributor() {


    // intelliJ 2018 and older
    extend(CompletionType.BASIC, PlatformPatterns.psiElement(HXMLTypes.VALUE).withParent(HXMLDefine.class), createProvider());
    // intelliJ 2019 and newer
    extend(CompletionType.BASIC, PlatformPatterns.psiElement().withParent(HXMLValue.class).withSuperParent(2, HXMLDefine.class), createProvider());
  }

  @NotNull
  private CompletionProvider<CompletionParameters> createProvider() {
    return new CompletionProvider<>() {
      @Override
      protected void addCompletions(@NotNull CompletionParameters parameters,
                                    ProcessingContext context,
                                    @NotNull CompletionResultSet result) {

        //VirtualFile file = parameters.getEditor().getVirtualFile();   // TODO use this when Android studio switches to 2023.x
        VirtualFile file = parameters.getOriginalFile().getVirtualFile();
        Project project = parameters.getEditor().getProject();
        if(project == null) {
          log.error("Unable to provide completion, Project is null");
          return;
        }
        Module module = ModuleUtil.findModuleForFile(file, project);

        final List<HXMLCompletionItem> defines = HaxeCompletionCache.getInstance(module).getDefines();

        for (int i = 0; i < defines.size(); i++) {
          HXMLCompletionItem completionItem = defines.get(i);
          result.addElement(LookupElementBuilder.create(completionItem.name).withTailText(" " + completionItem.description, true));
        }
      }
    };
  }
}
