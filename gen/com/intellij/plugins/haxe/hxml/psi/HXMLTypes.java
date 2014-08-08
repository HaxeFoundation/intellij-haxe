// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.hxml.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.hxml.psi.impl.*;

public interface HXMLTypes {

  IElementType PROPERTY = new HXMLElementType("PROPERTY");

  IElementType COMMENT = new HXMLTokenType("COMMENT");
  IElementType CRLF = new HXMLTokenType("CRLF");
  IElementType KEY = new HXMLTokenType("KEY");
  IElementType SEPARATOR = new HXMLTokenType("SEPARATOR");
  IElementType VALUE = new HXMLTokenType("VALUE");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
       if (type == PROPERTY) {
        return new HXMLPropertyImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
