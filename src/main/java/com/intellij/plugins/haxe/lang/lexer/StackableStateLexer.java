/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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

import com.intellij.lexer.FlexLexer;
import com.intellij.openapi.diagnostic.Logger;

import java.util.Stack;

public abstract class StackableStateLexer implements FlexLexer {

  static final Logger LOG = Logger.getInstance(HaxeLexer.class);

  record State(int stateId, int leftBraceCount, int leftParenCount) {
  }

  private final Stack<State> states = new Stack<>();

  protected int leftBraceCount = 0;
  protected int leftParenCount = 0;

  protected int getParentState() {
    return states.peek().stateId;
  }

  protected int getCurrentState() {
    return yystate();
  }

  protected void pushState(int stateId) {
    LOG.info("Setting State: " + stateId);
    states.push(new State(yystate(), leftBraceCount, leftParenCount));
    leftBraceCount = 0;
    leftParenCount = 0;
    yybegin(stateId);
  }

  protected void popState() {
    State state = states.pop();
    leftBraceCount = state.leftBraceCount;
    leftParenCount = state.leftParenCount;
    yybegin(state.stateId);
    LOG.info("Setting State: " + state.stateId);
  }
}
