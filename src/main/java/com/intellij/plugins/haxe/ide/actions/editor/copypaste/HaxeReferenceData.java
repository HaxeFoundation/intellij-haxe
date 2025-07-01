package com.intellij.plugins.haxe.ide.actions.editor.copypaste;

import com.intellij.codeInsight.editorActions.ReferenceData;
import org.jetbrains.annotations.NonNls;

import java.awt.datatransfer.DataFlavor;
import java.io.Serializable;

public class HaxeReferenceData extends ReferenceData implements Cloneable, Serializable {
    public static @NonNls DataFlavor ourFlavor;

    public final boolean isStatic;
    public final boolean isExtensionMethod;

    public HaxeReferenceData(int startOffset, int endOffset, String qualifiedName, boolean isStatic, boolean isExtensionMethod) {
        super(startOffset, endOffset, qualifiedName, null);
        this.isStatic = isStatic;
        this.isExtensionMethod = isExtensionMethod;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (Exception exception) {
            throw new RuntimeException();
        }
    }

    public static DataFlavor getDataFlavor() {
        if (ourFlavor != null) {
            return ourFlavor;
        } else {
            try {
                ourFlavor = new DataFlavor("application/x-haxe-local-objectref;class=" + HaxeReferenceData.class.getName(), "HaxeReferenceData", HaxeReferenceData.class.getClassLoader());
            } catch (IllegalArgumentException | ClassNotFoundException | NoClassDefFoundError var1) {
                return null;
            }

            return ourFlavor;
        }
    }
}