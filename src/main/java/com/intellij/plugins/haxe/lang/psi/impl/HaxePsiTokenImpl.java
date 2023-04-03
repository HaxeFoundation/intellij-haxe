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
package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.plugins.haxe.lang.psi.HaxePsiToken;
import com.intellij.psi.impl.source.tree.java.PsiJavaTokenImpl;
import com.intellij.psi.tree.IElementType;

/**
 * Our default token type is a PsiJavaToken so that our
 * PSI tree is more compatible with the Java one, thus, we can use
 * more of the Java code without doing so much work.
 *
 * Created by ebishton on 4/26/17.
 */
public class HaxePsiTokenImpl extends PsiJavaTokenImpl implements HaxePsiToken {

  public HaxePsiTokenImpl(IElementType type, CharSequence text) {
    super(type, text);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("HaxePsiToken:");
    sb.append(getElementType().toString());
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      sb.append(' ');
      sb.append('"');
      CharSequence text = this.getChars();
      int len = Math.min(100, text.length());
      sb.append(text.subSequence(0, len));
      sb.append('"');
    }
    return sb.toString();
  }
}
