/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2020 Eric Bishton
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
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.util.HaxeCompletionCache;
import com.intellij.util.ProcessingContext;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by as3boyan on 15.11.14.
 */
@CustomLog
public class HaxeMetaTagsCompletionContributor extends CompletionContributor {
  public HaxeMetaTagsCompletionContributor() {

    extend(CompletionType.BASIC, PlatformPatterns.psiElement(HaxeTokenTypes.META_ID), new CompletionProvider<>() {
      @Override
      protected void addCompletions(@NotNull CompletionParameters parameters,
                                    ProcessingContext context,
                                    @NotNull CompletionResultSet result) {

        //VirtualFile file = parameters.getEditor().getVirtualFile();   // TODO use this when Android studio switches to 2023.x
        VirtualFile file = parameters.getOriginalFile().getVirtualFile();
        Project project = parameters.getEditor().getProject();
        if(project == null) {
          log.error("Unable to provide completion, Project is nuull");
          return;
        }
        Module module = ModuleUtil.findModuleForFile(file, project);

        final List<HXMLCompletionItem> metaTags = HaxeCompletionCache.getInstance(module).getMetaTags();

        for (HXMLCompletionItem completionItem : metaTags) {
          result.addElement(LookupElementBuilder.create(completionItem.name).withTailText(" " + completionItem.description, true));
        }
      }
    });
  }
}
