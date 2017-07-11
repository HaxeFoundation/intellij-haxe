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
package com.intellij.plugins.haxe.lang.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.lexer.HaxeLexer;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.util.HaxeDebugTimeLog;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.resolve.FileContextUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

public class HaxeParserDefinition implements ParserDefinition {
  /** Set this false when you're finished debugging. */
  private static boolean debugging = false;

  public static class HaxeParserWrapper extends HaxeParser {
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
      return super.parse(t,b);
    }
  }


  @NotNull
  public Lexer createLexer(Project project) {
    return new HaxeLexer(project);
  }

  public PsiParser createParser(Project project) {
    return new HaxeParserWrapper();
  }

  public IFileElementType getFileNodeType() {
    return HaxeTokenTypeSets.HAXE_FILE;
  }

  @NotNull
  public TokenSet getWhitespaceTokens() {
    return HaxeTokenTypeSets.WHITESPACES;
  }

  @NotNull
  public TokenSet getCommentTokens() {
    return HaxeTokenTypeSets.COMMENTS;
  }

  @NotNull
  public TokenSet getStringLiteralElements() {
    return HaxeTokenTypeSets.STRINGS;
  }

  @NotNull
  public PsiElement createElement(ASTNode node) {
    return HaxeTokenTypes.Factory.createElement(node);
  }

  public PsiFile createFile(FileViewProvider viewProvider) {
    return new HaxeFile(viewProvider);
  }

  public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right) {
    return SpaceRequirements.MAY;
  }
}
