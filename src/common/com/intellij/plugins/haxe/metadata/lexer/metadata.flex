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

import com.intellij.psi.tree.IElementType;
import java.util.*;
import com.intellij.lexer.FlexLexer;

%%
%{
  public static class Kind {
    enum KindEnum {
      COMPILE_TIME,
      RUN_TIME,
      UNKNOWN
    };

    public KindEnum kind;

    public Kind() {
      kind = KindEnum.UNKNOWN;
    }

    public void setKind(KindEnum kind) {
      this.kind = kind;
    }

    public IElementType emitHaxeCode() {
      switch(kind) {
        case COMPILE_TIME: return HaxeMetadataTokenTypes.CT_META_ARGS;
        case RUN_TIME: return HaxeMetadataTokenTypes.RT_META_ARGS;
      }
      // LOG an error??
      return HaxeMetadataTokenTypes.HAXE_CODE;
    }
  }

  private Kind kind = new Kind();

  public class State {

    public int state;

    public State(int state) {
      this.state = state;
    }

    public String toString() {
      return getName(state);
    }

    public String getName(int state) {
      switch (state) {
        case YYINITIAL: return "YYINITIAL";
        case HAXE_CODE: return "HAXE_CODE";
        case WAITING_FOR_TYPE: return "WAITING_FOR_TYPE";
        case WAITING_FOR_PARENS: return "WAITING_FOR_PARENS";
        case CLOSED: return "CLOSED";
      }
      return Integer.toString(state);
    }
  }

  private final Stack<State> stateStack = new Stack<>();

  public void pushState(int newState) {
    stateStack.push(new State(yystate()));
    yybegin(newState);
  }

  public void popState() {
    State state;
    if (stateStack.isEmpty()) {
      // LOG an error??
      return;
    }
    state = stateStack.pop();
    yybegin(state.state);
  }

  public MetadataLexer() {
    this((java.io.Reader)null);
  }
%}

%class MetadataLexer
%implements FlexLexer
%unicode
%public
%function advance
%type IElementType
%eof{ return;
%eof}

CRLF=\n|\r|\r\n
WHITE_SPACE=[\ \t\f]

mLETTER = [:letter:] | "_"
mDIGIT = [:digit:]
//ESCAPE_SEQUENCE=\\[^\r\n]

mMETA_PART = {mLETTER} ({mDIGIT} | {mLETTER})*
COMPILE_META_PREFIX="@:"
RUNTIME_META_PREFIX="@"
META_TYPE={mMETA_PART} ("." {mMETA_PART})*

%state YYINITIAL WAITING_FOR_TYPE WAITING_FOR_PARENS HAXE_CODE CLOSED

%%

<YYINITIAL> {
{COMPILE_META_PREFIX}           {
                                  pushState(WAITING_FOR_TYPE);
                                  kind.setKind(MetadataLexer.Kind.KindEnum.COMPILE_TIME);
                                  return HaxeMetadataTokenTypes.CT_META_PREFIX;
                                }
{RUNTIME_META_PREFIX}           {
                                  pushState(WAITING_FOR_TYPE);
                                  kind.setKind(MetadataLexer.Kind.KindEnum.RUN_TIME);
                                  return HaxeMetadataTokenTypes.RT_META_PREFIX;
                                }
}

<WAITING_FOR_TYPE> {META_TYPE}  { pushState(WAITING_FOR_PARENS); return HaxeMetadataTokenTypes.META_TYPE; }

<WAITING_FOR_PARENS> "("        { pushState(HAXE_CODE); return HaxeMetadataTokenTypes.PLPAREN; }

<HAXE_CODE> {

// Because this is a sub-lexer (e.g. the main lexer has already determined the length
// of the entire token), a paren is only an end token if it's the last token on the stream.
// Except when incrementally re-parsing, in which case we may be getting extra characters,
// such as spaces, at the end.
")"                         {
                              if (zzStartRead >= zzEndRead - 1) { // -1 for the extra char during reparse
                                pushState(CLOSED);
                                return HaxeMetadataTokenTypes.PRPAREN;
                              }
                              return kind.emitHaxeCode();
                            }

<<EOF>>                     { pushState(CLOSED); return HaxeMetadataTokenTypes.EOF; }

{CRLF}+                     { return kind.emitHaxeCode(); }
{WHITE_SPACE}+              { return kind.emitHaxeCode(); }

// No such thing as a bad character here.  Let the Haxe lexer deal with it.
[^]                         { return kind.emitHaxeCode(); }
}

<CLOSED> [^]                { return HaxeMetadataTokenTypes.EXTRA_DATA; }

{CRLF}+                     { return com.intellij.psi.TokenType.WHITE_SPACE; }
{WHITE_SPACE}+              { return com.intellij.psi.TokenType.WHITE_SPACE; }
//<<EOF>>                     { return HaxeMetadataTokenTypes.EOF; }

[^]                         { return HaxeMetadataTokenTypes.INVALID_META_CHARACTER; }
