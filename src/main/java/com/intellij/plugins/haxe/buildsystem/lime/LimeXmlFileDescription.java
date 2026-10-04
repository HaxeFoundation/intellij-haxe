package com.intellij.plugins.haxe.buildsystem.lime;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Iconable;
import com.intellij.plugins.haxe.buildsystem.ProjectXml;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomFileDescription;
import icons.HaxeIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class LimeXmlFileDescription extends DomFileDescription<LimeProjectXml> {

    public LimeXmlFileDescription() {
        super(LimeProjectXml.class, ProjectXml.TAG_NAME);
    }

    @Override
    public @Nullable Icon getFileIcon(@Iconable.IconFlags int flags) {
        return HaxeIcons.LIME_LOGO;
    }

    @Override
    public boolean isMyFile(@NotNull XmlFile file, @Nullable Module module) {
        return LimeOpenFlUtil.isLimeFile(file);
    }
}
