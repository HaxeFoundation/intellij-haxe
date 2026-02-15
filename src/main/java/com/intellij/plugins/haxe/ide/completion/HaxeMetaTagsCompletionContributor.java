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
package com.intellij.plugins.haxe.ide.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.plugins.haxe.ide.HXMLCompletionItem;
import com.intellij.plugins.haxe.ide.documentation.providers.HaxeMetadataDocumentations;
import com.intellij.plugins.haxe.ide.lookup.HaxeMetadataLookupElement;
import com.intellij.plugins.haxe.metadata.lexer.HaxeMetadataTokenTypes;
import com.intellij.plugins.haxe.util.HaxeCompletionCache;
import com.intellij.util.ProcessingContext;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Created by as3boyan on 15.11.14.
 */
@CustomLog
public class HaxeMetaTagsCompletionContributor extends CompletionContributor {
  public HaxeMetaTagsCompletionContributor() {
    extend(CompletionType.BASIC, PlatformPatterns.psiElement(HaxeMetadataTokenTypes.META_TYPE), new CompletionProvider<>() {
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

        final List<HXMLCompletionItem> metaTags = HaxeCompletionCache.getInstance(module).getMetaTags();

        if(!metaTags.isEmpty()) {
          for (HXMLCompletionItem completionItem : metaTags) {
            //check if we got complementary docs and use those if available, otherwise use compiler results.
            HaxeMetadataDocumentations.MetadataInfo docs = HaxeMetadataDocumentations.getDocsFor(completionItem.name);
            if (docs != null) {
              result.addElement(new HaxeMetadataLookupElement(docs));
            } else {
              String presentation = ":" + completionItem.name;
              String description = completionItem.description;
              result.addElement(new HaxeMetadataLookupElement(presentation, description, ""));
            }
          }
        } else {
          for (HaxeMetadataDocumentations.MetadataInfo docs : HaxeMetadataDocumentations.getDocs()) {
            result.addElement(new HaxeMetadataLookupElement(docs));
          }
        }
      }
    });
  }
}
