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

import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.IElementType;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;

import static com.intellij.plugins.haxe.lang.lexer.HaxeLexer.*;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.CC_PASSIVE;

@Slf4j
public abstract class ConditionalCompilationLexer extends StackableStateLexer {

  //CC logic borrowed from  intellij haxe plugin

  HaxeConditionalCompilationLexerSupport ccsupport;

  protected int zzLexicalState = YYINITIAL;

  public abstract CharSequence yytext();


  public ConditionalCompilationLexer() {
  }

  public void createConditionalCompilationSupport(Project project) {
    ccsupport = new HaxeConditionalCompilationLexerSupport(project);
  }

  public IElementType emitToken(IElementType tokenType) {
    if (yystate() != CONDITIONAL_COMPILATION_PASSIVE) {
      return tokenType;
    }
    else {
//            return ccsupport.mapToken(tokenType);
      return CC_PASSIVE;
    }
  }

  List<IElementType> tokens = new LinkedList<>();
  protected boolean passive;

//    protected IElementType processConditional(IElementType type) {
//        //        ccsupport.processConditional(yytext(), type);
//
//            tokens.add(type);
//        //
////        if (CONDITIONAL_COMPILATION_IF.equals(type)) {
//////              ccStart();
////        } else if (CONDITIONAL_COMPILATION_END.equals(type)) {
//////              ccEnd();
//////            popState();
////        }
//////        } else if (zzLexicalState != CC_BLOCK) {
//////            // Maybe the #if is missing, but if we're not at the end, we want to be sure that we're
//////            // in the conditional state.
//////            LOG.debug("Unexpected lexical state. Missing starting #if?");
//////            //ccStart();
//////        }
////
////        if (CONDITIONAL_COMPILATION_IF.equals(type) || CONDITIONAL_COMPILATION_ELSEIF.equals(type)) {
////            conditionStart();
////        }
//        return type;
//    }

  /**
   * Deal with compiler conditional block constructs (e.g. #if...#end).
   */
  protected IElementType processConditional(IElementType type) {


//        if (CONDITIONAL_COMPILATION_IF.equals(type)) {
////            ccStart();
//        } else if (CONDITIONAL_COMPILATION_END.equals(type)) {
////            ccEnd();
//        } else if (zzLexicalState != CONDITIONAL_COMPILATION_BLOCK) {
//            // Maybe the #if is missing, but if we're not at the end, we want to be sure that we're
//            // in the conditional state.
//            LOG.debug("Unexpected lexical state. Missing starting #if?");
//            ccStart();
//        }

    ccsupport.processConditional(yytext(), type);

    if (CONDITIONAL_COMPILATION_IF.equals(type)) {
      conditionExpressionStart();
    }
    else if (CONDITIONAL_COMPILATION_ELSEIF.equals(type)) {
      endCCBlock(); // end  #if/#elseif block (if it exists)
      conditionExpressionStart();
    }
    else if (CONDITIONAL_COMPILATION_ELSE.equals(type)) {
      endCCBlock(); // end  #if/#elseif block (if it exists)
      startCCBlock();
    }
    else if (CONDITIONAL_COMPILATION_END.equals(type)) {
      endCCBlock();
    }


    return type;
  }

  private void endCCBlock() {
    if (yystate() == CONDITIONAL_COMPILATION_BLOCK || yystate() == CONDITIONAL_COMPILATION_PASSIVE) {
      popState();
    }
    else {
      log.warn("end cc block called when not in a CC block");
    }
  }

  private void startCCBlock() {
    if (ccsupport.currentContextIsActive()) {
      pushState(CONDITIONAL_COMPILATION_BLOCK);
    }
    else {
      pushState(CONDITIONAL_COMPILATION_PASSIVE);
    }
  }

  protected void conditionExpressionStart() {
    pushState(CONDITIONAL_COMPILATION_EXPRESSION);
//        ccsupport.conditionStart();
  }


  protected IElementType conditionExpressionAppend(IElementType type) {
    ccsupport.conditionAppend(yytext(), type);
    if (ccsupport.conditionIsComplete()) {
      conditionExpressionEnd();
      startCCBlock();
    }
    return type;
  }


  protected void conditionExpressionEnd() {
    ccsupport.conditionEnd();
    if (yystate() == CONDITIONAL_COMPILATION_EXPRESSION) {
      popState();
    }
    else {
      log.warn("CC condition end without being in CC state");
    }
  }

//    // We use the CC_BLOCK state to tell the highlighters, etc. that their context
//    // has to go back to the start of the conditional (even though that may be a ways).  Basically,
//    // we need to keep the state as something other than YYINITIAL.
//    protected void ccStart() {
//        pushState(CONDITIONAL_COMPILATION_EXPRESSION);
//    } // Until we know better
//
//    protected void ccEnd() {
//        // When there is no #if, but there is an end, popping the state produces an EmptyStackException
//        // and messes up further processing.
//        if (zzLexicalState == CONDITIONAL_COMPILATION_BLOCK) {
//            popState();
//        }
//    }


  // These deal with the state of lexing the *condition* for compiler conditionals

//    public IElementType evaluate(IElementType type) {
//        //        pushState(COMPILER_CONDITIONAL); ccsupport.conditionStart();
//        if (CONDITIONAL_COMPILATION_IF.equals(type)) {
////            passive = false;
//            ccsupport.conditionStart();
//        }
//        if (CONDITIONAL_COMPILATION_ELSEIF.equals(type)) {
////            passive = true;
//            ccsupport.conditionEnd();
//            ccsupport.conditionStart();
//        }
//        if (CONDITIONAL_COMPILATION_ELSE.equals(type)) {
////            passive = true;
//            ccsupport.conditionEnd();
//            ccsupport.conditionStart();
//        }
//        if (CONDITIONAL_COMPILATION_END.equals(type)) {
////            passive = false;
//            ccsupport.conditionEnd();
//        }
//        return type;
//
//    }

//    public IElementType evaluateAndPop(IElementType type) {
////        passive = false;
//        popState();
//        IElementType elementType = evaluate(type);
//        return elementType;
//
//    }
//    private void conditionStart() {
//        //        pushState(COMPILER_CONDITIONAL); ccsupport.conditionStart();
//    }
//
////    private boolean conditionIsComplete() {
////                return ccsupport.conditionIsComplete();
////    }
//
////      private IElementType conditionAppend(IElementType type) {
////         // ccsupport.conditionAppend(yytext(),type);
////          if (ccsupport.conditionIsComplete()) {
////              conditionEnd();
////          }
////          return PPEXPRESSION;
////      }
//      private void conditionEnd() {
//        //  ccsupport.conditionEnd();
//          popState();
//      }

  // We use the CC_BLOCK state to tell the highlighters, etc. that their context
  // has to go back to the start of the conditional (even though that may be a ways).  Basically,
  // we need to keep the state as something other than YYINITIAL.
//      private void ccStart() { pushState(CC_BLOCK); } // Until we know better
//      private void ccEnd() {
//          // When there is no #if, but there is an end, popping the state produces an EmptyStackException
//          // and messes up further processing.
//          if (zzLexicalState == CC_BLOCK) {
//              popState();
//          }
//      }
}
