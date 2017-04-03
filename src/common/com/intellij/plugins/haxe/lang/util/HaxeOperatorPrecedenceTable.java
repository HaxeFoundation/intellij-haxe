/*
 * Copyright 2017 Eric Bishton
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
package com.intellij.plugins.haxe.lang.util;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

/**
 * Lookup table for operators, useful in parsing and evaluating expressions.
 * Haxe uses the C precedence rules for operators.
 *
 * Created by ebishton on 3/31/17.
 */
public class HaxeOperatorPrecedenceTable {

  public static class OperatorNotFoundException extends Exception {
    public OperatorNotFoundException(String msg) {
      super(msg);
    }
  }

  public enum Associativity {
    LEFT,
    RIGHT,
    DEPENDS
  }

  public enum Arity {
    NULLARY,
    UNARY,
    BINARY,
    TERNARY,
    VARIADIC
  }

  static final HashMap<IElementType, Descriptor> operators = new HashMap<IElementType, Descriptor>(17);
  static final HaxeOperatorPrecedenceTable singleton = new HaxeOperatorPrecedenceTable();

  //public static HaxeOperatorPrecedenceTable instance() {
  //  return singleton;
  //}

  /**
   * Compare two operators to determine their precedence toward one another according to
   * the normal rules for the shunting-yard algorithm.  To whit:
   *   while:
   *    o1 is left-associative and its precedence is less than or equal to that of o2, or
   *    o1 is right associative, and has precedence less than that of o2,
   *      pop 02 off the operator stack onto the ouput queue.
   *
   * @param inputop o1 (from the input queue)
   * @param stackop o2 (on top of the operator stack)
   * @return true if o1 is of lesser precedence; false, if o1 is of higher precedence.
   */
  public static boolean shuntingYardCompare(@NotNull IElementType inputop, @NotNull IElementType stackop)
      throws OperatorNotFoundException {
    Descriptor id = operators.get(inputop);
    Descriptor sd = operators.get(stackop);
    if (null == id || null == sd) {
      throw new OperatorNotFoundException( "Operator entry not found for '" + (null == id ? inputop.toString() : stackop.toString()) + "'");
    }
    return id.associativity == Associativity.RIGHT
      ? id.precedence < sd.precedence
      : id.precedence <= sd.precedence;
  }

  @Nullable
  public static Arity getArity(@NotNull IElementType op) {
    Descriptor id = operators.get(op);
    return null == id ? null : id.arity;
  }

  private static class Descriptor {
    public IElementType operator;
    public int precedence;
    public Associativity associativity;
    public Arity arity;

    public Descriptor(Arity ar, IElementType op, int precedence, Associativity lrassoc) {
      this.operator = op;
      this.precedence = precedence;
      this.associativity = lrassoc;
      this.arity = ar;
    }
  }

  private HaxeOperatorPrecedenceTable() {
    // TODO: Add all operators. Don't forget FatArrow, TripleDot, UnsignedRightShift, UnsignedRightShiftAndAssign

    // Precedence values are from 0 - 100, with 100 being higher.
    // elements between section markers (=======) are at the same level.

    // ========================================================================================
    // Parens (function calls), [] brackets, . member selection, -> member selection via pointer (not Haxe),
    // ++/--postfix increment and decrement.
    addOperator(Arity.VARIADIC, PRPAREN,           100, Associativity.LEFT);
    addOperator(Arity.VARIADIC, PLPAREN,           100, Associativity.LEFT);

    // ========================================================================================
    // ++/-- Prefix increment and decrement, + - unary plus/minus, ! logical negation, ~bitwise complement,
    // (cast) type cast , * dereference via pointer (not Haxe), & addressOf (not Haxe), sizeof (not Haxe)
    addOperator(Arity.UNARY,    ONOT,               95, Associativity.RIGHT);

    // ========================================================================================
    // * / % Multiplication/Division/Modulus

    // ========================================================================================
    // + - Addition/subtraction

    // ========================================================================================
    // << >> Bitwise left/right shift

    // ========================================================================================
    // < <= Relational less than/less than or equal to
    // > >= Relational greater than/greater than or equal to
    addOperator(Arity.BINARY,   OGREATER,           75, Associativity.LEFT);
    addOperator(Arity.BINARY,   OGREATER_OR_EQUAL,  75, Associativity.LEFT);
    addOperator(Arity.BINARY,   OLESS,              75, Associativity.LEFT);
    addOperator(Arity.BINARY,   OLESS_OR_EQUAL,     75, Associativity.LEFT);

    // ========================================================================================
    // == != Relational equal/not equal to
    addOperator(Arity.BINARY,   OEQ,                70, Associativity.LEFT);
    addOperator(Arity.BINARY,   ONOT_EQ,            70, Associativity.LEFT);

    // ========================================================================================
    // & Bitwise AND

    // ========================================================================================
    // ^ Bitwise exclusive OR (XOR)

    // ========================================================================================
    // | Bitwise inclusive OR

    // ========================================================================================
    // && Logical AND
    addOperator(Arity.BINARY,   OCOND_AND,          50, Associativity.LEFT);

    // ========================================================================================
    // || Logical OR
    addOperator(Arity.BINARY,   OCOND_OR,           45, Associativity.LEFT);

    // ========================================================================================
    // ? : Ternary conditional  Right-to-left association

    // ========================================================================================
    // = Assignment, += -= *= /= %= &= ^= |= <<= >>=  Operate and assign  Right-to-left association

    // ========================================================================================
    // , comma (separate expressions)

    // ========================================================================================
    // Other things that may be interesting:
    //    ; sequence point
    //    { } block expression.
  }

  private void addOperator(Arity arity, IElementType type, int precedence, Associativity lrassoc) {
    operators.put(type, new Descriptor(arity, type, precedence, lrassoc));
  }
}
