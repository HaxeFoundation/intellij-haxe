package com.intellij.plugins.haxe.buildsystem.lime;

import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@UtilityClass
public class LimeOpenFlUtil {
    public boolean isOpenfl(XmlTag rootTag) {
        return Arrays.stream(rootTag.findSubTags("haxelib")).anyMatch(xmlTag -> "openfl".equalsIgnoreCase(xmlTag.getAttribute("name").getValue()));
    }

    public boolean isOpenFlFile(@NotNull XmlFile file) {
        if(!FileUtilRt.extensionEquals(file.getName(), "xml")) return false;

        XmlDocument document = file.getDocument();
        if(document == null) return false;

        XmlTag rootTag = document.getRootTag();
        if(rootTag == null) return false;

        return LimeOpenFlUtil.isOpenfl(rootTag);
    }

    public boolean isLimeFile(@NotNull XmlFile file) {
        if(!FileUtilRt.extensionEquals(file.getName(), "xml")) return false;

        XmlDocument document = file.getDocument();
        if (document == null) return false;

        XmlTag rootTag = document.getRootTag();
        if (rootTag == null) return false;

        return !LimeOpenFlUtil.isOpenfl(rootTag);
    }

}
