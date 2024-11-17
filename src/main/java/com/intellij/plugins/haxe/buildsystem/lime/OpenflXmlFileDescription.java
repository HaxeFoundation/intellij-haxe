package com.intellij.plugins.haxe.buildsystem.lime;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.xml.XmlFile;
import icons.HaxeIcons;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

@NoArgsConstructor
public class OpenflXmlFileDescription extends LimeXmlFileDescription {


    @Override
    public @Nullable Icon getFileIcon(@NotNull XmlFile file, @Iconable.IconFlags int flags) {
        if(isOpenFlFile(file)) {
            return HaxeIcons.OPENFL_LOGO;
        }
        return null;
    }


    @Override
    public boolean isMyFile(@NotNull XmlFile file, @Nullable Module module) {
        return isOpenFlFile(file);
    }

    private boolean isOpenFlFile(@NotNull XmlFile file) {
        return LimeOpenFlUtil.isOpenFlFile(file);
    }

}