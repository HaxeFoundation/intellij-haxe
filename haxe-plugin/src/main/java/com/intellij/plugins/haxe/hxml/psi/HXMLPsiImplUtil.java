/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
package com.intellij.plugins.haxe.hxml.psi;

import com.intellij.lang.ASTNode;

/**
 * Created by as3boyan on 01.11.14.
 */
public class HXMLPsiImplUtil {
  public static String getOption(HXMLProperty property) {
    ASTNode node = property.getNode().findChildByType(HXMLTypes.OPTION);

    if (node != null) {
      return node.getText();
    }
    else {
      return null;
    }
  }

  public static String getValue(HXMLProperty property) {
    ASTNode node = property.getNode().findChildByType(HXMLTypes.VALUE);

    if (node != null) {
      return node.getText();
    }
    else {
      return null;
    }
  }

  public static String getValue(HXMLClasspath classpath) {
    ASTNode node = classpath.getNode().findChildByType(HXMLTypes.VALUE);

    if (node != null) {
      return node.getText();
    }
    else {
      return null;
    }
  }

  public static String getValue(HXMLLib lib) {
    ASTNode node = lib.getNode().findChildByType(HXMLTypes.VALUE);

    if (node != null) {
      return node.getText();
    }
    else {
      return null;
    }
  }
}
