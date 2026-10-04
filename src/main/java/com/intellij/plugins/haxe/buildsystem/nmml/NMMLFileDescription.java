package com.intellij.plugins.haxe.buildsystem.nmml;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.plugins.haxe.buildsystem.ProjectXml;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomFileDescription;
import icons.HaxeIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class NMMLFileDescription extends DomFileDescription<NmmlProjectXml> {

    public NMMLFileDescription() {
        super(NmmlProjectXml.class, ProjectXml.TAG_NAME);
    }

    @Override
    public @Nullable Icon getFileIcon(@Iconable.IconFlags int flags) {
        return HaxeIcons.NMML_LOGO;
    }

    @Override
    public boolean isMyFile(@NotNull XmlFile file, @Nullable Module module) {
        return isNmmlFile(file);
    }

    private static boolean isNmmlFile(@NotNull XmlFile file) {
        XmlDocument document = file.getDocument();
        if (document == null || document.getRootTag() == null) return false;
        return FileUtilRt.extensionEquals(file.getName(), NMMLFileType.INSTANCE.getDefaultExtension());
    }
}
