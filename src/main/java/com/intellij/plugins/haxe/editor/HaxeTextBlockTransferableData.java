/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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

import com.intellij.codeInsight.editorActions.TextBlockTransferableData;

import java.awt.datatransfer.DataFlavor;

/**
 * Created by as3boyan on 09.10.14.
 */
public class HaxeTextBlockTransferableData implements TextBlockTransferableData {
  @Override
  public DataFlavor getFlavor() {
    return null;
  }

  @Override
  public int getOffsetCount() {
    return 0;
  }

  @Override
  public int getOffsets(int[] offsets, int index) {
    return 0;
  }

  @Override
  public int setOffsets(int[] offsets, int index) {
    return 0;
  }
}
