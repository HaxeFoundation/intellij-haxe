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
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class HXMLParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, null);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    if (t == CLASSPATH) {
      r = classpath(b, 0);
    }
    else if (t == DEFINE) {
      r = define(b, 0);
    }
    else if (t == HXML) {
      r = hxml(b, 0);
    }
    else if (t == LIB) {
      r = lib(b, 0);
    }
    else if (t == MAIN) {
      r = main(b, 0);
    }
    else if (t == PROPERTY) {
      r = property(b, 0);
    }
    else if (t == QUALIFIED_NAME) {
      r = qualifiedName(b, 0);
    }
    else {
      r = parse_root_(t, b, 0);
    }
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return simpleFile(b, l + 1);
  }

  /* ********************************************************** */
  // '-cp' SEPARATOR VALUE
  public static boolean classpath(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "classpath")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<classpath>");
    r = consumeToken(b, "-cp");
    r = r && consumeTokens(b, 0, SEPARATOR, VALUE);
    exit_section_(b, l, m, CLASSPATH, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '-D' SEPARATOR VALUE
  public static boolean define(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "define")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<define>");
    r = consumeToken(b, "-D");
    r = r && consumeTokens(b, 0, SEPARATOR, VALUE);
    exit_section_(b, l, m, DEFINE, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // HXML_FILE
  public static boolean hxml(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "hxml")) return false;
    if (!nextTokenIs(b, HXML_FILE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, HXML_FILE);
    exit_section_(b, m, HXML, r);
    return r;
  }

  /* ********************************************************** */
  // hxml | lib | define | classpath | main | property | COMMENT | CRLF
  static boolean item_(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "item_")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = hxml(b, l + 1);
    if (!r) r = lib(b, l + 1);
    if (!r) r = define(b, l + 1);
    if (!r) r = classpath(b, l + 1);
    if (!r) r = main(b, l + 1);
    if (!r) r = property(b, l + 1);
    if (!r) r = consumeToken(b, COMMENT);
    if (!r) r = consumeToken(b, CRLF);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '-lib' SEPARATOR VALUE (':' VALUE)?
  public static boolean lib(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "lib")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<lib>");
    r = consumeToken(b, "-lib");
    r = r && consumeTokens(b, 0, SEPARATOR, VALUE);
    r = r && lib_3(b, l + 1);
    exit_section_(b, l, m, LIB, r, false, null);
    return r;
  }

  // (':' VALUE)?
  private static boolean lib_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "lib_3")) return false;
    lib_3_0(b, l + 1);
    return true;
  }

  // ':' VALUE
  private static boolean lib_3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "lib_3_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ":");
    r = r && consumeToken(b, VALUE);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '-main' SEPARATOR qualifiedName
  public static boolean main(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "main")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<main>");
    r = consumeToken(b, "-main");
    r = r && consumeToken(b, SEPARATOR);
    r = r && qualifiedName(b, l + 1);
    exit_section_(b, l, m, MAIN, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // (KEY SEPARATOR VALUE) | KEY
  public static boolean property(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "property")) return false;
    if (!nextTokenIs(b, KEY)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = property_0(b, l + 1);
    if (!r) r = consumeToken(b, KEY);
    exit_section_(b, m, PROPERTY, r);
    return r;
  }

  // KEY SEPARATOR VALUE
  private static boolean property_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "property_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, KEY, SEPARATOR, VALUE);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // QUALIFIEDCLASSNAME
  public static boolean qualifiedName(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "qualifiedName")) return false;
    if (!nextTokenIs(b, QUALIFIEDCLASSNAME)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, QUALIFIEDCLASSNAME);
    exit_section_(b, m, QUALIFIED_NAME, r);
    return r;
  }

  /* ********************************************************** */
  // item_*
  static boolean simpleFile(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "simpleFile")) return false;
    int c = current_position_(b);
    while (true) {
      if (!item_(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "simpleFile", c)) break;
      c = current_position_(b);
    }
    return true;
  }

}
