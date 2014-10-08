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
package com.intellij.plugins.haxe.editor;

import com.intellij.codeInsight.editorActions.CopyPastePostProcessor;
import com.intellij.codeInsight.editorActions.TextBlockTransferableData;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.Transferable;

/**
 * Created by as3boyan on 08.10.14.
 */
public class HaxeReferenceCopyPasteProcessor implements CopyPastePostProcessor {
  @Nullable
  @Override
  public TextBlockTransferableData collectTransferableData(PsiFile file, Editor editor, int[] startOffsets, int[] endOffsets) {
    return null;
  }

  @Nullable
  @Override
  public TextBlockTransferableData extractTransferableData(Transferable content) {
    return null;
  }

  @Override
  public void processTransferableData(Project project, Editor editor, RangeMarker marker, int i, Ref ref, TextBlockTransferableData data) {
    int offset = editor.getCaretModel().getOffset();
  }
}
