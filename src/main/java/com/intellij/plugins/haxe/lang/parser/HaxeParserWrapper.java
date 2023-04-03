/*
 * Copyright 2017-2020 Eric Bishton
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
package com.intellij.plugins.haxe.lang.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.plugins.haxe.metadata.lexer.HaxeMetadataTokenTypes;
import com.intellij.plugins.haxe.util.HaxeDebugTimeLog;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.resolve.FileContextUtil;
import com.intellij.psi.tree.IElementType;
import lombok.CustomLog;

@CustomLog
public class HaxeParserWrapper extends HaxeParser {


  /** Turns on debugging when running the parser.  On when the logger is in debug mode. */
  private static boolean debugging = log.isDebugEnabled();

  @Override
  public ASTNode parse(IElementType t, PsiBuilder b) {
    if (debugging) {
      HaxeDebugTimeLog timeLog = null;

      // The file is attached to the user data. :/  I suspect this was a hack, but it's what we've got available.
      PsiFile file = b.getUserDataUnprotected(FileContextUtil.CONTAINING_FILE_KEY);

      String description = null != file ? file.getName() : t.toString();
      timeLog = HaxeDebugTimeLog.startNew("HaxeParser " + description, HaxeDebugTimeLog.Since.StartAndPrevious);

      ASTNode node = super.parse(t, b);

      timeLog.stamp("Finished parsing.");
      timeLog.printIfTimeExceeds(20 /* milliseconds */);

      return node;
    }
    return super.parse(t, b);
  }

  @Override
  protected boolean parse_root_(IElementType type, PsiBuilder builder) {
    return parse_root(type, builder, 0);
  }

  private boolean parse_root(IElementType type, PsiBuilder builder, int level) {
    if (type == HaxeMetadataTokenTypes.CT_META_ARGS) {
      return compileTimeMetaArgList(builder, level + 1);
    }
    else if (type == HaxeMetadataTokenTypes.RT_META_ARGS) {
      return runTimeMetaArgList(builder, level + 1);
    }
    return haxeFile(builder, level + 1);
  }
}
