package com.intellij.plugins.haxe.ide.completion.weigher;

import com.intellij.codeInsight.completion.CompletionLocation;
import com.intellij.codeInsight.completion.CompletionWeigher;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.plugins.haxe.ide.completion.HaxeCompletionPriorityUtil;
import com.intellij.plugins.haxe.ide.lookup.HaxeLookupElement;
import org.jetbrains.annotations.NotNull;

public class HaxeLookupCompletionWeigher extends CompletionWeigher {
  @Override
  public Comparable<HaxeLookupElement> weigh(@NotNull LookupElement element, @NotNull CompletionLocation location) {
    if (element instanceof HaxeLookupElement haxeLookupElement) {
      HaxeCompletionPriorityUtil.calculatePriority(haxeLookupElement, location);
      return haxeLookupElement;
    }
    return null;
  }
}
