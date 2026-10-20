package com.intellij.plugins.haxe.buildsystem.nmml;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.plugins.haxe.buildsystem.ProjectXmlVocabulary;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.DomFileDescription;
import icons.HaxeIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class NMMLFileDescription extends DomFileDescription<NmmlProjectXml> {

    public NMMLFileDescription() {
        super(NmmlProjectXml.class, ProjectXmlVocabulary.ROOT_ELEMENT);
    }

    @Override
    public @Nullable Icon getFileIcon(@Iconable.IconFlags int flags) {
        return HaxeIcons.NMML_LOGO;
    }

    @Override
    public boolean isMyFile(@NotNull XmlFile file, @Nullable Module module) {
        return isNmmlFile(file);
    }

    /** Any file under NME's extension with a {@code <project>} root, the rule the build-file scanner applies. */
    private static boolean isNmmlFile(@NotNull XmlFile file) {
        XmlDocument document = file.getDocument();
        XmlTag root = document == null ? null : document.getRootTag();
        if (root == null) return false;
        return FileUtilRt.extensionEquals(file.getName(), NMMLFileType.DEFAULT_EXTENSION)
               && ProjectXmlVocabulary.ROOT_ELEMENT.equalsIgnoreCase(root.getLocalName());
    }
}
