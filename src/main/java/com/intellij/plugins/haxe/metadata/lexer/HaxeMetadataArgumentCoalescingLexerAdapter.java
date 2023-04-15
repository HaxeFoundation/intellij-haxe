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
package com.intellij.plugins.haxe.metadata.lexer;

import com.intellij.lexer.Lexer;
import com.intellij.lexer.LookAheadLexer;
import com.intellij.lexer.MergingLexerAdapter;
import com.intellij.psi.tree.TokenSet;

/** This class coalesces all of the Haxe code that appears inside of metadata into a single
 *  LazyPsiToken that isn't parsed by the metadata parser, but passed back to the Haxe language
 *  parser later.
 */
public class HaxeMetadataArgumentCoalescingLexerAdapter extends LookAheadLexer {
  private static final TokenSet tokensToMerge = TokenSet.create(
    HaxeMetadataTokenTypes.HAXE_CODE,
    HaxeMetadataTokenTypes.CT_META_ARGS,
    HaxeMetadataTokenTypes.RT_META_ARGS
  );

  public HaxeMetadataArgumentCoalescingLexerAdapter(Lexer original) {
    super(new MergingLexerAdapter(original, tokensToMerge));
  }
}
