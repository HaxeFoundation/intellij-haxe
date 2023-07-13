package com.intellij.plugins.haxe.buildsystem.lime;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlSchemaProvider;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;

public class LimeXmlSchemaProvider extends XmlSchemaProvider {
    @Override
    public XmlFile getSchema(@NotNull @NonNls String url, @Nullable Module module, @NotNull PsiFile baseFile) {
        final URL resource = LimeXmlSchemaProvider.class.getResource("/xsd/lime/lime-project.xsd");
        final VirtualFile fileByURL = VfsUtil.findFileByURL(resource);
        PsiFile result = baseFile.getManager().findFile(fileByURL);
        if (result instanceof XmlFile) {
            return (XmlFile) result.copy();
        }
        return null;
    }

    @Override
    public boolean isAvailable(final @NotNull XmlFile file) {
        XmlDocument document = file.getDocument();
        if (document == null) return false;

        XmlTag rootTag = document.getRootTag();
        if (rootTag == null) return false;

        return LimeOpenFlUtil.looksLikeALimeProjectFile(rootTag);
    }
}
