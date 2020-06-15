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

import com.intellij.lexer.LookAheadLexer;

/**
 * This is the top-level lexer for metadata. It is called by the HaxeMetadataParserDefinition to parse
 * "@meta({Haxe code})". We use a separate parser for metadata because it allows the metadata to be an
 * "embedded" language -- one that can appear at any point within the Haxe code, but isn't strictly part
 * of the language syntax and doesn't interrupt normal Haxe syntax rules.
 *
 * When we are parsing the metadata, it really just goes into an ID, parens, and arguments. The arguments
 * are themselves "embedded" Haxe code, and we send that back to the Haxe parser.
 */
public class HaxeMetadataLexer extends LookAheadLexer {
  public HaxeMetadataLexer() {
    super(new HaxeMetadataArgumentCoalescingLexerAdapter(new HaxeMetadataFlexLexer(new MetadataLexer())));
  }
}
