package com.intellij.plugins.haxe.lang.lexer;

import com.intellij.lexer.Lexer;
import com.intellij.lexer.LexerPosition;
import com.intellij.lexer.LookAheadLexer;
import com.intellij.lexer.MergingLexerAdapter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.config.HaxeProjectSettings;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import gnu.trove.THashSet;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.*;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

public class HaxeLexer extends LookAheadLexer {
  public static Key<Object> DEFINES_KEY = Key.create("haxe.test.defines");
  private static final TokenSet tokensToMerge = TokenSet.create(
    MSL_COMMENT,
    MML_COMMENT,
    WSNLS
  );

  private final static Set<String> SDK_DEFINES = new THashSet<String>(Arrays.asList(
    "macro"
  ));

  @Nullable
  private Project myProject;

  public HaxeLexer(Project project) {
    super(new MergingLexerAdapter(new HaxeFlexLexer(), tokensToMerge));
    myProject = project;
  }

  @Override
  protected void lookAhead(Lexer baseLexer) {
    if (baseLexer.getTokenType() == PPERROR) {
      final LexerPosition position = baseLexer.getCurrentPosition();
      baseLexer.advance();
      while (HaxeTokenTypeSets.WHITESPACES.contains(baseLexer.getTokenType()) ||
             HaxeTokenTypeSets.COMMENTS.contains(baseLexer.getTokenType())) {
        baseLexer.advance();
      }
      if (HaxeTokenTypeSets.STRINGS.contains(baseLexer.getTokenType())) {
        baseLexer.advance();
      }
      else {
        baseLexer.restore(position);
      }
      advanceAs(baseLexer, PPERROR);
    }
    else if (baseLexer.getTokenType() == PPIF || baseLexer.getTokenType() == PPELSEIF) {
      advanceAs(baseLexer, PPIF);
      while (!lookAheadExpressionIsTrue(baseLexer)) {
        addToken(PPEXPRESSION);
        IElementType elementType = eatUntil(baseLexer, PPEND, PPELSE, PPELSEIF);
        if (elementType == PPELSEIF) {
          advanceAs(baseLexer, PPBODY);
          continue;
        }
        advanceAs(baseLexer, PPBODY);
        break;
      }
      addToken(PPEXPRESSION);
    }
    else if (baseLexer.getTokenType() == PPELSE) {
      eatUntil(baseLexer, PPEND);
      advanceAs(baseLexer, PPELSE);
    }
    else {
      super.lookAhead(baseLexer);
    }
  }

  @Nullable
  protected static IElementType eatUntil(Lexer baseLexer, IElementType... types) {
    final Set<IElementType> typeSet = new THashSet<IElementType>(Arrays.asList(types));
    IElementType type = null;
    int counter = 0;
    do {
      baseLexer.advance();
      type = baseLexer.getTokenType();
      if (type == PPIF) {
        ++counter;
      }
      if (counter > 0 && type == PPEND) {
        --counter;
        baseLexer.advance();
        type = baseLexer.getTokenType();
      }
    }
    while (type != null && (!typeSet.contains(type) || counter > 0));
    return type;
  }

  protected boolean lookAheadExpressionIsTrue(Lexer baseLexer) {
    IElementType type = null;
    // reverse polish notation
    final LinkedList<IElementType> stack = new LinkedList<IElementType>();
    final LinkedList<String> rpn = new LinkedList<String>();
    do {
      final LexerPosition position = baseLexer.getCurrentPosition();
      baseLexer.advance();
      type = baseLexer.getTokenType();
      if (HaxeTokenTypeSets.WHITESPACES.contains(type) || HaxeTokenTypeSets.ONLY_COMMENTS.contains(type)) {
        continue;
      }
      final String tokenText = baseLexer.getTokenText();
      if (type == ID) {
        if (canCalculate(rpn, stack)) {
          //revert
          baseLexer.restore(position);
          break;
        }
        rpn.addFirst(tokenText);
      }
      else if (type == PRPAREN) {
        do {
          IElementType typeOnStack = stack.pollLast();
          if (typeOnStack == PLPAREN) {
            break;
          }
          rpn.addFirst(typeOnStack.toString());
        }
        while (!stack.isEmpty());
        while (!stack.isEmpty() && stack.getLast() == ONOT) {
          rpn.addFirst(stack.pollLast().toString());
        }
      }
      else if (type == OCOND_AND || type == OCOND_OR) {
        while (!stack.isEmpty() && (stack.getLast() == OCOND_AND || stack.getLast() == OCOND_OR)) {
          rpn.addFirst(stack.pollLast().toString());
        }
        stack.add(type);
      }
      else if (type == ONOT || type == PLPAREN) {
        stack.add(type);
      }
      else {
        baseLexer.restore(position);
        break;
      }
    }
    while (true);
    try {
      return calculate(rpn, stack);
    }
    catch (CalculationException e) {
      return false;
    }
  }

  private boolean canCalculate(LinkedList<String> rpn, LinkedList<IElementType> stack) {
    try {
      calculate(rpn, stack);
      return true;
    }
    catch (CalculationException e) {
      return false;
    }
  }

  private boolean calculate(LinkedList<String> rpn, LinkedList<IElementType> stack) throws CalculationException {
    final LinkedList<String> list = new LinkedList<String>();
    for (IElementType type : stack) {
      if (type == PLPAREN) {
        throw new CalculationException("Balance error");
      }
      list.add(type.toString());
    }
    Collections.reverse(list);
    list.addAll(rpn);
    return calculateImpl(list);
  }

  private boolean calculateImpl(LinkedList<String> list) throws CalculationException {
    if (list.isEmpty()) {
      throw new CalculationException("Incorrect expression");
    }
    final String token = list.pollFirst();
    if ("!".equals(token)) {
      return !calculateImpl(list);
    }
    else if ("&&".equals(token)) {
      boolean a = calculateImpl(list);
      boolean b = calculateImpl(list);
      return a && b;
    }
    else if ("||".equals(token)) {
      boolean a = calculateImpl(list);
      boolean b = calculateImpl(list);
      return a || b;
    }
    else {
      return isDefined(token);
    }
  }

  protected boolean isDefined(String name) {
    if (name == null) {
      return false;
    }
    if (myProject == null) {
      return SDK_DEFINES.contains(name);
    }
    String[] definitions = null;
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      final Object userData = myProject.getUserData(DEFINES_KEY);
      if (userData instanceof String) {
        definitions = ((String)userData).split(",");
      }
    }
    else {
      definitions = HaxeProjectSettings.getInstance(myProject).getUserCompilerDefinitions();
    }
    return definitions != null && Arrays.asList(definitions).contains(name);
  }

  private static class CalculationException extends Exception {
    private CalculationException(String message) {
      super(message);
    }
  }
}