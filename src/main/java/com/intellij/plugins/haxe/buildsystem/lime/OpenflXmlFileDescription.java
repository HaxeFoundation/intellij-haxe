package com.intellij.plugins.haxe.buildsystem.lime;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import icons.HaxeIcons;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

@NoArgsConstructor
public class OpenflXmlFileDescription extends LimeXmlFileDescription {


    @Override
    public Icon getFileIcon(@Iconable.IconFlags int flags) {
        return HaxeIcons.OPENFL_LOGO;
    }


    @Override
    public boolean isMyFile(@NotNull XmlFile file, @Nullable Module module) {
        if(!FileUtilRt.extensionEquals(file.getName(), "xml")) return false;

        XmlDocument document = file.getDocument();
        if(document == null) return false;

        XmlTag rootTag = document.getRootTag();
        if(rootTag == null) return false;

        return LimeOpenFlUtil.isOpenfl(rootTag);
    }

}