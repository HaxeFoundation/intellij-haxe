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
package com.intellij.plugins.haxe.lang.lexer;

import com.intellij.lexer.FlexAdapter;
import com.intellij.openapi.project.Project;

public class HaxeFlexLexer extends FlexAdapter {
  /**
   * Folded into {@link #getState()} while the lexer is in YYINITIAL right
   * after a value-completing token. The editor's incremental highlighter
   * restarts lexing at token boundaries and reproduces the lexer's context
   * from the saved int state alone; without this bit a restart loses
   * {@code lastSignificantToken} and lexes a following {@code <} as an
   * XML-literal start. The parser always lexes from offset 0, so only editor
   * highlighting depends on it. Flex state numbers stop at 20, so the flag is
   * clear of them.
   */
  static final int VALUE_CONTEXT_STATE_FLAG = 0x100;

  public HaxeFlexLexer(Project context, boolean remapInactiveToPpbody) {
    super(new HaxeGeneratedLexerWrapper(context, remapInactiveToPpbody));
  }

  @Override
  public int getState() {
    int state = super.getState();
    if (state == _HaxeLexer.YYINITIAL && ((HaxeGeneratedLexerWrapper)getFlex()).isValueContextAtTokenStart()) {
      return state | VALUE_CONTEXT_STATE_FLAG;
    }
    return state;
  }
}
