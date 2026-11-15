package com.intellij.plugins.haxe.buildsystem.lime;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.xml.XmlSchemaProvider;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;

public class LimeXmlSchemaProvider extends XmlSchemaProvider {

    private static final String PROJECT_SCHEMA = "/schema/lime/lime-project.xsd";
    // the config sections, under the namespace LimeXmlExtension binds the config prefix to
    private static final String CONFIG_SCHEMA = "/schema/lime/lime-config.xsd";

    @Override
    public XmlFile getSchema(@NotNull @NonNls String url, @Nullable Module module, @NotNull PsiFile baseFile) {
        String schema = LimeXmlExtension.CONFIG_NAMESPACE.equals(url) ? CONFIG_SCHEMA : PROJECT_SCHEMA;
        final URL resource = LimeXmlSchemaProvider.class.getResource(schema);
        final VirtualFile fileByURL = VfsUtil.findFileByURL(resource);
        PsiFile result = baseFile.getManager().findFile(fileByURL);
        if (result instanceof XmlFile) {
            return (XmlFile) result.copy();
        }
        return null;
    }

    @Override
    public boolean isAvailable(final @NotNull XmlFile file) {
        return LimeOpenFlUtil.isLimeDialectFile(file);
    }
}
