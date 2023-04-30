/*
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
package com.intellij.plugins.haxe.lang.lexer;

import com.intellij.lexer.Lexer;
import com.intellij.lexer.MergeFunction;
import com.intellij.lexer.MergingLexerAdapterBase;
import com.intellij.psi.tree.IElementType;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

public class HaxeMetaCoalescingLexerAdapter extends MergingLexerAdapterBase {
  private MergeFunction myMergeFunction = new HaxeMetaMergeFunction();

  public HaxeMetaCoalescingLexerAdapter(Lexer original) {
    super(original);
  }

  @Override
  public MergeFunction getMergeFunction() {
    return myMergeFunction;
  }

  public static class HaxeMetaMergeFunction implements MergeFunction {
    @Override
    public IElementType merge(IElementType type, Lexer originalLexer) {
      if (type == META_ID) return HaxeTokenTypes.EMBEDDED_META;

      if (type != META_WITH_ARGS) {
        return type;
      }

      // Yeah, metas can contain metas.  Because compiler metas can contain arbitrary code.
      int reEntryCount = 1;
      int end = originalLexer.getBufferEnd();
      while (0 != reEntryCount && originalLexer.getTokenEnd() < end) {
        IElementType tokenType = originalLexer.getTokenType();
        if (META_WITH_ARGS == tokenType) ++reEntryCount;
        else if (META_WITH_ARGS_END == tokenType) --reEntryCount;
        originalLexer.advance();
      }

      return HaxeTokenTypes.EMBEDDED_META;
    }
  }

}
