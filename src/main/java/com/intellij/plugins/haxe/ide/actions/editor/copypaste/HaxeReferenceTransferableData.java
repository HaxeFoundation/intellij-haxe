package com.intellij.plugins.haxe.ide.actions.editor.copypaste;

import com.intellij.codeInsight.editorActions.TextBlockTransferableData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.DataFlavor;
import java.io.Serializable;

// based on ReferenceTransferableData
public class HaxeReferenceTransferableData  implements TextBlockTransferableData, Cloneable, Serializable {
    private final HaxeReferenceData[] myReferenceDatas;

    public HaxeReferenceTransferableData(HaxeReferenceData @NotNull [] referenceDatas) {
        myReferenceDatas = referenceDatas;
    }

    @Override
    public @Nullable DataFlavor getFlavor() {
        return HaxeReferenceData.getDataFlavor();
    }

    @Override
    public int getOffsetCount() {
        return myReferenceDatas.length * 2;
    }

    @Override
    public int getOffsets(int @NotNull [] offsets, int index) {
        for (HaxeReferenceData data : myReferenceDatas) {
            offsets[index++] = data.startOffset;
            offsets[index++] = data.endOffset;
        }
        return index;
    }

    @Override
    public int setOffsets(int @NotNull [] offsets, int index) {
        for (HaxeReferenceData data : myReferenceDatas) {
            data.startOffset = offsets[index++];
            data.endOffset = offsets[index++];
        }
        return index;
    }

    @Override
    public HaxeReferenceTransferableData clone() {
        HaxeReferenceData[] newReferenceData = new HaxeReferenceData[myReferenceDatas.length];
        for (int i = 0; i < myReferenceDatas.length; i++) {
            newReferenceData[i] = (HaxeReferenceData)myReferenceDatas[i].clone();
        }
        return new HaxeReferenceTransferableData(newReferenceData);
    }

    public HaxeReferenceData @NotNull [] getData() {
        return myReferenceDatas;
    }
}