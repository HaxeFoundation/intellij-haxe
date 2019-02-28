/*
 * Copyright 2019 Eric Bishton
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
package com.intellij.plugins.haxe.util;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class HaxeRangeUtil {

  static class RangeBuilder {
    private boolean first = true;
    private int minStartOffset = 0;
    private int maxEndOffset = 0;

    public RangeBuilder() { }

    public void addRange(@Nullable TextRange range) {
      if (null == range) return;

      if (first) {
        first = false;
        minStartOffset = range.getStartOffset();
        maxEndOffset = range.getEndOffset();
      } else {
        minStartOffset = Math.min(minStartOffset, range.getStartOffset());
        maxEndOffset = Math.max(maxEndOffset, range.getEndOffset());
      }
    }

    public TextRange toRange() {
      if (first) {
        return TextRange.EMPTY_RANGE;
      }
      return new TextRange(minStartOffset, maxEndOffset);
    }
  }

  private HaxeRangeUtil() {}

  @NotNull
  public static TextRange getCombinedRange(@Nullable PsiElement... elements) {
    if (null == elements)
      return TextRange.EMPTY_RANGE;
    return getCombinedRange(Arrays.asList(elements));
  }

  @NotNull
  public static TextRange getCombinedRange(@NotNull Iterable<? extends PsiElement> iterable) {
    if (null == iterable)
      return TextRange.EMPTY_RANGE;

    RangeBuilder builder = new RangeBuilder();
    for (PsiElement element : iterable) {
      if (null != element) {
        builder.addRange(element.getTextRange());
      }
    }
    return builder.toRange();
  }
}
