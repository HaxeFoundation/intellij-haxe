/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
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
package com.intellij.plugins.haxe.ide.formatter;

import com.intellij.formatting.Block;
import com.intellij.formatting.FormattingDocumentModel;
import com.intellij.formatting.FormattingModel;
import com.intellij.formatting.FormattingModelProvider;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.build.MethodWrapper;
import com.intellij.plugins.haxe.build.IdeaTarget;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeFormattingModel implements FormattingModel {
  private final FormattingModel myModel;

  // Need an abstract method call because of an API change.
  private static final MethodWrapper<TextRange> shiftIndentInsideRangeWrapper =
          new MethodWrapper<TextRange>(FormattingModel.class, "shiftIndentInsideRange",
                                    IdeaTarget.IS_VERSION_14_1_5_COMPATIBLE
                                      ? new Class<?>[] {ASTNode.class, TextRange.class, int.class }
                                      : new Class<?>[] {TextRange.class, int.class} );

  public HaxeFormattingModel(final PsiFile file,
                             CodeStyleSettings settings,
                             final Block rootBlock) {
    myModel = FormattingModelProvider.createFormattingModelForPsiFile(file, rootBlock, settings);
  }

  @NotNull
  public Block getRootBlock() {
    return myModel.getRootBlock();
  }

  @NotNull
  public FormattingDocumentModel getDocumentModel() {
    return myModel.getDocumentModel();
  }

  public TextRange replaceWhiteSpace(TextRange textRange, String whiteSpace) {
    return myModel.replaceWhiteSpace(textRange, whiteSpace);
  }

  // @Override
  public TextRange shiftIndentInsideRange(ASTNode node, TextRange range, int i) {
    return shiftIndentInsideRangeWrapper.invoke(myModel, node, range, i);
  }

  // @Override
  public TextRange shiftIndentInsideRange(TextRange range, int indent) {
    return shiftIndentInsideRangeWrapper.invoke(myModel, range, indent);
  }

  public void commitChanges() {
    myModel.commitChanges();
  }
}
