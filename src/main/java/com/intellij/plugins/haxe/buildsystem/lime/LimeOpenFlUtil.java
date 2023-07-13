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
    public boolean looksLikeALimeProjectFile(XmlTag rootTag) {
        if(rootTag.getName().equalsIgnoreCase("project")) {
            XmlTag[] haxelibTags = rootTag.findSubTags("haxelib");
            XmlTag[] sourceTags = rootTag.findSubTags("source");
            XmlTag[] appTags = rootTag.findSubTags("app");
            XmlTag[] metaTags = rootTag.findSubTags("meta");

            int tagTypesFound = 0;

            tagTypesFound += haxelibTags.length > 0 ? 1 : 0;
            tagTypesFound += sourceTags.length > 0 ? 1 : 0;
            tagTypesFound += appTags.length > 0 ? 1 : 0;
            tagTypesFound += metaTags.length > 0 ? 1 : 0;

            // if we found more than 2 known lime/openfl tags (including that the root is project)
            // we consider that a good enough match (we want to avoid conflicts with maven xml that also starts with <project>)
            return tagTypesFound >= 2;
        }
        return false;
    }

    public boolean isOpenFlFile(@NotNull XmlFile file) {
        if(!FileUtilRt.extensionEquals(file.getName(), "xml")) return false;

        XmlDocument document = file.getDocument();
        if(document == null) return false;

        XmlTag rootTag = document.getRootTag();
        if(rootTag == null) return false;

        if (!looksLikeALimeProjectFile(rootTag)) return false;

        return LimeOpenFlUtil.isOpenfl(rootTag);
    }

    public boolean isLimeFile(@NotNull XmlFile file) {
        if(!FileUtilRt.extensionEquals(file.getName(), "xml")) return false;

        XmlDocument document = file.getDocument();
        if (document == null) return false;

        XmlTag rootTag = document.getRootTag();
        if (rootTag == null) return false;

        if (!looksLikeALimeProjectFile(rootTag)) return false;

        return !LimeOpenFlUtil.isOpenfl(rootTag);
    }

}
