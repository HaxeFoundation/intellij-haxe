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
import com.intellij.patterns.PlatformPatterns;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.util.HaxeHelpCache;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by as3boyan on 15.11.14.
 */
public class HaxeMetaTagsCompletionContributor extends CompletionContributor {
  public HaxeMetaTagsCompletionContributor() {
    final List<HXMLCompletionItem> metaTags = HaxeHelpCache.getInstance().getMetaTags();
    extend(CompletionType.BASIC, PlatformPatterns.psiElement(HaxeTokenTypes.MACRO_ID), new CompletionProvider<CompletionParameters>() {
      @Override
      protected void addCompletions(@NotNull CompletionParameters parameters,
                                    ProcessingContext context,
                                    @NotNull CompletionResultSet result) {
        for (int i = 0; i < metaTags.size(); i++) {
          HXMLCompletionItem completionItem = metaTags.get(i);
          result.addElement(LookupElementBuilder.create(completionItem.name).withTailText(" " + completionItem.description, true));
        }
      }
    });
  }
}
