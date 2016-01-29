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
