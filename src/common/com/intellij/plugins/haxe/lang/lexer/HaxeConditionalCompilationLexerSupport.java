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
import com.intellij.plugins.haxe.lang.util.HaxeConditionalExpression;
import com.intellij.plugins.haxe.util.HaxeDebugLogger;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.*;

/**
 * Support Conditional Compilation for the Haxe language.
 *
 * Created by ebishton on 3/23/17.
 */
public class HaxeConditionalCompilationLexerSupport {
  static final String classname = new Object(){}.getClass().getEnclosingClass().getName();
  static final HaxeDebugLogger LOG = HaxeDebugLogger.getInstance("#" + classname);
  static {      // Take this out when finished debugging.
    LOG.setLevel(org.apache.log4j.Level.DEBUG);
  }

  /**
   * State for tracking the condition of #if and #elseif conditionals.
   */
  private static final class Block {
    public enum Type {
      IF,
      ELSEIF,
      ELSE,
      END,
      UNKNOWN,
      ROOT
    }
    public static Type deriveBlockType(@Nullable IElementType el) {
      if (null == el || el == TokenType.DUMMY_HOLDER) {
        return Type.ROOT;
      }
      if (el == PPIF)          { return Type.IF; }
      else if (el == PPELSEIF) { return Type.ELSEIF; }
      else if (el == PPELSE)   { return Type.ELSE; }
      else if (el == PPEND)    { return Type.END; }
      else {
        LOG.debug("Unrecognized compiler conditional is being used.");
      }
      return Type.UNKNOWN;
    }

    public enum ActiveState {
      UNKNOWN,
      ACTIVE,
      INACTIVE
    }

    private IElementType myElementType;
    private HaxeConditionalExpression myCondition;
    private Type myBlockType;
    private ActiveState myActive;
    private Section mySection;

    public Block(Section s, IElementType el) {
      mySection = s;
      myElementType = el;
      myBlockType = deriveBlockType(el);
      switch (myBlockType) {
        case IF:
        case ELSEIF:
          myCondition = new HaxeConditionalExpression(null);
          break;
        default:
          break;
      }
      myActive = ActiveState.UNKNOWN;
    }

    @Nullable
    public HaxeConditionalExpression getCondition() {
      return myCondition;
    }

    public ActiveState getActive() {
      return myActive;
    }

    public void setActive(ActiveState newState) {
      myActive = newState;
    }

    public Type getType() {
      return myBlockType;
    }

    public void appendToCondition(CharSequence token, IElementType type) {
      myCondition.extend(token.toString(), type);
      myActive = ActiveState.UNKNOWN;
    }
  }  // End Block  //////////////////////////////////////////////////////////////////////////////////////////


  /**
   * Master controller for a conceptual set of conditionals, starting with an #if and ending with #end.
   */
  private class Section {
    private ArrayList<Block> myBlocks;
    private Section myParent;

    public Section(Section parent, IElementType el) {
      myBlocks = new ArrayList<Block>();
      myParent = parent;
      startBlock(el);
    }

    public Block startBlock(IElementType el) {
      endBlock(currentBlock());

      Block block = new Block(this, el);
      myBlocks.add(block);
      return block;
    }

    /**
     * Find the active block in the section.
     * @return the active block, if any; null, otherwise.
     */
    @Nullable
    public Block activeBlock() {
      return previousActiveBlock(null);
    }

    /* Note: previousActiveBlock and setActiveBlockState are recursive with regard to one another.
     *       This works because setActiveBlockState is always working with smaller subsets on each recursion.
     */
    @Nullable
    private Block previousActiveBlock(Block stopBlock) {
      for (Block b : myBlocks) {
        if (b.equals(stopBlock)) {
          break;
        }
        if (b.getActive() == Block.ActiveState.UNKNOWN) {
          setBlockActiveState(b);
        }
        if (b.getActive() == Block.ActiveState.ACTIVE) {
          return b;
        }
      }
      return null;

    }

    /* Note: previousActiveBlock and setActiveBlockState are recursive with regard to one another.  */
    private void setBlockActiveState(Block block) {
      if (block != null && block.getActive() == Block.ActiveState.UNKNOWN) {
        HaxeConditionalExpression condition = block.getCondition();
        boolean priorBlockIsActive = null != previousActiveBlock(block);
        if (priorBlockIsActive) {
          // Another block ahead of this one is already myActive, so this one can't be.
          block.setActive(Block.ActiveState.INACTIVE);
        } else if (condition != null) {  // IF or ELSEIF
          // No blocks ahead of this one are myActive, so calculate the condition
          // If the condition is incomplete, then the block is inactive.
          block.setActive(condition.evaluate(projectContext) ? Block.ActiveState.ACTIVE : Block.ActiveState.INACTIVE);
        } else {
          if (block.getType() == Block.Type.END) {
            block.setActive(Block.ActiveState.INACTIVE);
          }
          if (block.getType() == Block.Type.ELSE) {
            // No prior block was myActive, so this one has to be.
            block.setActive(Block.ActiveState.ACTIVE);
          }
        }
      }
    }

    public void endBlock(@Nullable Block toEnd) {
      if (toEnd != null) {
        setBlockActiveState(toEnd);
      }
    }

    @Nullable
    public Block currentBlock() {
      return myBlocks.isEmpty() ? null : myBlocks.get(myBlocks.size()-1);
    }

    @Nullable
    public Section getParent() {
      return myParent;
    }
  }  // End Section  //////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Base section for the lexer.  Theoretically, it shouldn't have blocks of its own, but it does as a way
   * to keep track of blocks when there is a missing #if.
   */
  public class RootSection extends Section {
    private Block rootBlock;
    public RootSection() {
      super(null, null);
      rootBlock = this.currentBlock();
      rootBlock.setActive(Block.ActiveState.ACTIVE);
    }

    @Override
    public Block startBlock(IElementType type) {
      if (type == PPIF) {
        LOG.debug("Root Section requested to start an invalid block type:" + type.toString());
      }
      return super.startBlock(null);
    }

  }  // End RootSection ////////////////////////////////////////////////////////////////////////////////////////


  private RootSection rootSection;
  private Section currentContext;
  private Project projectContext;

  public HaxeConditionalCompilationLexerSupport(Project context) {
    rootSection = new RootSection();
    currentContext =  rootSection;
    projectContext = context;
  }

  /**
   * Determines the current conditional compilation context.
   *
   */
  public Section getCurrentContext() {
    return currentContext;
  }

  public Block getCurrentBlock() {
    return getCurrentContext().currentBlock();
  }

  public boolean currentContextIsActive() {
    Section context = getCurrentContext();
    while (context != null && context.currentBlock() == context.activeBlock()) {
      context = context.getParent();
    }
    return context == null;
  }

  /**
   * Manipulates the conditional Section/Block stack based upon the CC token.
   * @param chars Characters that comprise the token.
   * @param type Detected token type.
   */
  public void processConditional(CharSequence chars, IElementType type) {
    if (PPIF.equals(type)) {
      // Start a new section...
      Section newSection = new Section(currentContext, type);
      currentContext = newSection;
    } else if (PPELSE.equals(type)) {
      currentContext.startBlock(type);
    } else if (PPELSEIF.equals(type)) {
      currentContext.startBlock(type);
    } else if (PPEND.equals(type)) {
      currentContext.endBlock(getCurrentBlock());
      Section parent = currentContext.getParent();
      if (null != parent) {
        currentContext = parent;
      }
    }
  }

  /**
   * Starts _condition_ processing
   */
  public void conditionStart() {
    // Anything to do here??
  }

  /**
   * Ends _condition_ processing.
   */
  public void conditionEnd() {
    HaxeConditionalExpression condition = getCurrentBlock().getCondition();
    if (null != condition && !condition.isComplete()) {
       LOG.debug("Ending condition before it is complete: '" + condition.toString() + "'");
    }
  }

  /**
   * Adds new tokens to the conditional on the current block.
   *
   * @param chars
   * @param type
   * @return true if it is OK to continue appending (condition is incomplete);
   *         false if there should be no further appending..
   */
  public void conditionAppend(CharSequence chars, IElementType type) {
    HaxeConditionalExpression condition = getCurrentBlock().getCondition();
    if (null == condition) {
      LOG.warn("Lexer is adding tokens to a conditional compilation block that has no condition.");
      return;
    }
    if (condition.isComplete()) {
      LOG.warn("Lexer is adding tokens to a conditional compilation block with an already completed condition.");
      return;
    }
    condition.extend(chars, type);
  }

  /**
   * Determine whether the current condition is complete and parseable.
   * @return
   */
  public boolean conditionIsComplete() {
    HaxeConditionalExpression condition = getCurrentBlock().getCondition();
    if (null == condition) {
      return false;
    }
    boolean complete = condition.isComplete();
    return complete;
  }

  /**
   * Map tokens to comments, as appropriate.
   */
  public IElementType mapToken(IElementType type) {

    // Special things that we don't map.
    if (WHITESPACES.contains(type)) {
      return type;
    }

    if (!currentContextIsActive()) {
      return PPBODY;
    }
    return type;
  }

}