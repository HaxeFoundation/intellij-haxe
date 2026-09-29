/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2017 AS3Boyan
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

import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.IElementType;

import java.io.IOException;

/**
 * This class exists solely to add hooks to the generated _HaxeLexer.  A better
 * solution could be to use a JFlex skeleton that allows for hooks.  Since we
 * use the skeleton provided with grammar-kit, this is easier to maintain.
 *
 * Created by ebishton on 4/14/17.
 */
public class HaxeGeneratedLexerWrapper extends _HaxeLexer {
  // snapshotted in advance() because FlexAdapter records yystate() right
  // before lexing each token - the pair must describe the same instant
  private boolean valueContextAtTokenStart;

  public HaxeGeneratedLexerWrapper(Project project, boolean remapInactiveToPpbody) {
    super(project);
    this.remapInactiveToPpbody = remapInactiveToPpbody;
  }

  /**
   * Whether the token about to be lexed follows a value-completing token -
   * the context deciding if a {@code <} is an operator or an XML-literal
   * start (see {@link HaxeFlexLexer#VALUE_CONTEXT_STATE_FLAG}).
   */
  boolean isValueContextAtTokenStart() {
    return valueContextAtTokenStart;
  }

  @Override
  public IElementType advance() throws IOException {
    valueContextAtTokenStart = lastSignificantToken != null
                               && HaxeTokenTypeSets.VALUE_COMPLETING_TOKENS.contains(lastSignificantToken);
    return super.advance();
  }

  public void reset(CharSequence buffer, int start, int end, int initialState) {
    super.reset(buffer, start, end, initialState & ~HaxeFlexLexer.VALUE_CONTEXT_STATE_FLAG);
    super.ccsupport.reset(super.context);
    // ID stands in for whichever value-completing token preceded the restart
    // point - only set membership matters to isExpressionExpected()
    super.lastSignificantToken =
      (initialState & HaxeFlexLexer.VALUE_CONTEXT_STATE_FLAG) != 0 ? HaxeTokenTypes.ID : null;

    super.states.clear();
    super.xmlContexts.clear();
  }
}
