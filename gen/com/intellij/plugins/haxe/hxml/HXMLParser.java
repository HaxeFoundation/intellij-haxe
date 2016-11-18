/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2016 AS3Boyan
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

// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.hxml;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static com.intellij.plugins.haxe.hxml.psi.HXMLTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class HXMLParser implements PsiParser {

  public ASTNode parse(IElementType root_, PsiBuilder builder_) {
    parseLight(root_, builder_);
    return builder_.getTreeBuilt();
  }

  public void parseLight(IElementType root_, PsiBuilder builder_) {
    boolean result_;
    builder_ = adapt_builder_(root_, builder_, this, null);
    Marker marker_ = enter_section_(builder_, 0, _COLLAPSE_, null);
    if (root_ == CLASSPATH) {
      result_ = classpath(builder_, 0);
    }
    else if (root_ == DEFINE) {
      result_ = define(builder_, 0);
    }
    else if (root_ == HXML) {
      result_ = hxml(builder_, 0);
    }
    else if (root_ == LIB) {
      result_ = lib(builder_, 0);
    }
    else if (root_ == MAIN) {
      result_ = main(builder_, 0);
    }
    else if (root_ == PROPERTY) {
      result_ = property(builder_, 0);
    }
    else if (root_ == QUALIFIED_NAME) {
      result_ = qualifiedName(builder_, 0);
    }
    else {
      result_ = parse_root_(root_, builder_, 0);
    }
    exit_section_(builder_, 0, marker_, root_, result_, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType root_, PsiBuilder builder_, int level_) {
    return simpleFile(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // '-cp' SEPARATOR VALUE
  public static boolean classpath(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classpath")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<classpath>");
    result_ = consumeToken(builder_, "-cp");
    result_ = result_ && consumeTokens(builder_, 0, SEPARATOR, VALUE);
    exit_section_(builder_, level_, marker_, CLASSPATH, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // '-D' SEPARATOR VALUE
  public static boolean define(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "define")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<define>");
    result_ = consumeToken(builder_, "-D");
    result_ = result_ && consumeTokens(builder_, 0, SEPARATOR, VALUE);
    exit_section_(builder_, level_, marker_, DEFINE, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // HXML_FILE
  public static boolean hxml(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "hxml")) return false;
    if (!nextTokenIs(builder_, HXML_FILE)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, HXML_FILE);
    exit_section_(builder_, marker_, HXML, result_);
    return result_;
  }

  /* ********************************************************** */
  // hxml | lib | define | classpath | main | property | qualifiedName | COMMENT | CRLF
  static boolean item_(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "item_")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = hxml(builder_, level_ + 1);
    if (!result_) result_ = lib(builder_, level_ + 1);
    if (!result_) result_ = define(builder_, level_ + 1);
    if (!result_) result_ = classpath(builder_, level_ + 1);
    if (!result_) result_ = main(builder_, level_ + 1);
    if (!result_) result_ = property(builder_, level_ + 1);
    if (!result_) result_ = qualifiedName(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, COMMENT);
    if (!result_) result_ = consumeToken(builder_, CRLF);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // '-lib' SEPARATOR VALUE (':' VALUE)?
  public static boolean lib(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "lib")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<lib>");
    result_ = consumeToken(builder_, "-lib");
    result_ = result_ && consumeTokens(builder_, 0, SEPARATOR, VALUE);
    result_ = result_ && lib_3(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, LIB, result_, false, null);
    return result_;
  }

  // (':' VALUE)?
  private static boolean lib_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "lib_3")) return false;
    lib_3_0(builder_, level_ + 1);
    return true;
  }

  // ':' VALUE
  private static boolean lib_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "lib_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ":");
    result_ = result_ && consumeToken(builder_, VALUE);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // '-main' SEPARATOR qualifiedName
  public static boolean main(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "main")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<main>");
    result_ = consumeToken(builder_, "-main");
    result_ = result_ && consumeToken(builder_, SEPARATOR);
    result_ = result_ && qualifiedName(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, MAIN, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // (KEY SEPARATOR VALUE) | KEY
  public static boolean property(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "property")) return false;
    if (!nextTokenIs(builder_, KEY)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = property_0(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, KEY);
    exit_section_(builder_, marker_, PROPERTY, result_);
    return result_;
  }

  // KEY SEPARATOR VALUE
  private static boolean property_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "property_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, KEY, SEPARATOR, VALUE);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // QUALIFIEDCLASSNAME
  public static boolean qualifiedName(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "qualifiedName")) return false;
    if (!nextTokenIs(builder_, QUALIFIEDCLASSNAME)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, QUALIFIEDCLASSNAME);
    exit_section_(builder_, marker_, QUALIFIED_NAME, result_);
    return result_;
  }

  /* ********************************************************** */
  // item_*
  static boolean simpleFile(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "simpleFile")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!item_(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "simpleFile", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

}
