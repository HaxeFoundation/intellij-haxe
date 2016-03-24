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
package com.intellij.plugins.haxe.hxml.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.hxml.psi.impl.*;

public interface HXMLTypes {

  IElementType CLASSPATH = new HXMLElementType("CLASSPATH");
  IElementType DEFINE = new HXMLElementType("DEFINE");
  IElementType HXML = new HXMLElementType("HXML");
  IElementType LIB = new HXMLElementType("LIB");
  IElementType MAIN = new HXMLElementType("MAIN");
  IElementType PROPERTY = new HXMLElementType("PROPERTY");
  IElementType QUALIFIED_NAME = new HXMLElementType("QUALIFIED_NAME");

  IElementType COMMENT = new HXMLTokenType("COMMENT");
  IElementType CRLF = new HXMLTokenType("CRLF");
  IElementType HXML_FILE = new HXMLTokenType("HXML_FILE");
  IElementType KEY = new HXMLTokenType("KEY");
  IElementType QUALIFIEDCLASSNAME = new HXMLTokenType("QUALIFIEDCLASSNAME");
  IElementType SEPARATOR = new HXMLTokenType("SEPARATOR");
  IElementType VALUE = new HXMLTokenType("VALUE");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
       if (type == CLASSPATH) {
        return new HXMLClasspathImpl(node);
      }
      else if (type == DEFINE) {
        return new HXMLDefineImpl(node);
      }
      else if (type == HXML) {
        return new HXMLHxmlImpl(node);
      }
      else if (type == LIB) {
        return new HXMLLibImpl(node);
      }
      else if (type == MAIN) {
        return new HXMLMainImpl(node);
      }
      else if (type == PROPERTY) {
        return new HXMLPropertyImpl(node);
      }
      else if (type == QUALIFIED_NAME) {
        return new HXMLQualifiedNameImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
