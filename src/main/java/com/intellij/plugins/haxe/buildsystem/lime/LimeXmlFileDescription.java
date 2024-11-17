package com.intellij.plugins.haxe.buildsystem.lime;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.plugins.haxe.buildsystem.ProjectXml;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.DomFileDescription;
import icons.HaxeIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class LimeXmlFileDescription extends DomFileDescription<ProjectXml> {

    public LimeXmlFileDescription() {
        super(ProjectXml.class, ProjectXml.TAG_NAME);
    }

    @Override
    public @Nullable Icon getFileIcon(@NotNull XmlFile file, @Iconable.IconFlags int flags) {
        if(isLimeFile(file)) {
            return HaxeIcons.LIME_LOGO;
        }
        return null;
    }


    @Override
    public boolean isMyFile(@NotNull XmlFile file, @Nullable Module module) {
        return isLimeFile(file);
    }

    private boolean isLimeFile(@NotNull XmlFile file) {
        return LimeOpenFlUtil.isLimeFile(file);
    }


}