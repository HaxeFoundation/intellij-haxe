package com.intellij.plugins.haxe.ide.lookup;

import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import org.jspecify.annotations.NonNull;

public interface HaxeLookupElement extends Comparable<HaxeLookupElement> {
  HaxeCompletionPriorityData getPriority();

  @Override
  default int compareTo(@NonNull HaxeLookupElement other) {
    double otherValue = other.getPriority().calculate();
    double thisValue = this.getPriority().calculate();
    return Double.compare(thisValue, otherValue);
  }
}
