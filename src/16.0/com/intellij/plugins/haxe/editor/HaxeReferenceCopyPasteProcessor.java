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

import com.intellij.codeInsight.daemon.impl.CollectHighlightsUtil;
import com.intellij.codeInsight.editorActions.CopyPastePostProcessor;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.plugins.haxe.ide.index.HaxeComponentIndex;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeComponent;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.util.HaxeAddImportHelper;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by as3boyan on 08.10.14.
 */
public class HaxeReferenceCopyPasteProcessor extends CopyPastePostProcessor<HaxeTextBlockTransferableData>  {

  @NotNull
  @Override
  public List<HaxeTextBlockTransferableData> collectTransferableData(PsiFile file, Editor editor, int[] startOffsets, int[] endOffsets) {
    return Collections.emptyList(); //TODO impl
  }

  @NotNull
  @Override
  public List<HaxeTextBlockTransferableData> extractTransferableData(Transferable content) {
    Object transferData = null;
    try {
      transferData = content.getTransferData(DataFlavor.stringFlavor);
    }
    catch (UnsupportedFlavorException e) {
      e.printStackTrace();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return Collections.emptyList(); //TODO impl
  }

  @Override
  public void processTransferableData(Project project, Editor editor, RangeMarker marker, int caretOffset, Ref<Boolean> indented, List<HaxeTextBlockTransferableData> values) {
    PsiDocumentManager.getInstance(project).commitAllDocuments();
    final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null) {
      return;
    }
    int[] startOffsets = new int[]{marker.getStartOffset()};
    int[] endOffsets = new int[]{marker.getEndOffset()};

    List<String> haxeClassList = new ArrayList<String>();

    String qualifiedName;
    for (int j = 0; j < startOffsets.length; j++) {
      final int startOffset = startOffsets[j];
      for (final PsiElement element : CollectHighlightsUtil.getElementsInRange(file, startOffset, endOffsets[j])) {
        if (element instanceof HaxeReferenceExpression) {
          HaxeReferenceExpression referenceExpression = (HaxeReferenceExpression)element;

          if (referenceExpression.resolve() == null) {
            final GlobalSearchScope scope = HaxeResolveUtil.getScopeForElement(referenceExpression);
            final List<HaxeComponent> components =
              HaxeComponentIndex.getItemsByName(referenceExpression.getText(), project, scope);
            if (!components.isEmpty() && components.size() == 1) {
              qualifiedName = ((HaxeClass)components.get(0)).getQualifiedName();
              if (!haxeClassList.contains(qualifiedName)) {
                haxeClassList.add(qualifiedName);
              }
            }
          }
        }
      }
    }

    if (haxeClassList.isEmpty()) {
      return;
    }

    HaxeRestoreReferencesDialog dialog = new HaxeRestoreReferencesDialog(project, ArrayUtil.toStringArray(haxeClassList));
    dialog.show();
    String[] selectedObjects = dialog.getSelectedElements();

    for (final String object : selectedObjects) {
      new WriteCommandAction(project, file) {
        @Override
        protected void run(@NotNull Result result) throws Throwable {
          HaxeAddImportHelper.addImport(object, file);
        }
      }.execute();
    }
  }
}
