package com.intellij.plugins.haxe.buildsystem.lime;

import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.plugins.haxe.ide.projectStructure.detection.HaxeProjectFileDetectionUtil;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

@UtilityClass
public class LimeOpenFlUtil {
    public boolean isOpenfl(XmlTag rootTag) {
        return Arrays.stream(rootTag.findSubTags("haxelib"))
          .map(xmlTag -> xmlTag.getAttribute("name"))
          .filter(Objects::nonNull)
          .anyMatch(attribute -> "openfl".equalsIgnoreCase(attribute.getValue()));
    }
    // TODO: one recognition rule with HaxeProjectFileDetectionUtil, whose scanner parses bytes instead of PSI
    public boolean looksLikeALimeProjectFile(XmlTag rootTag) {
        if (!rootTag.getName().equalsIgnoreCase("project")) return false;
        // <classpath> is lime's alias of <source>
        boolean hasSources = hasSubTag(rootTag, "source") || hasSubTag(rootTag, "classpath");

        int tagKindsFound = 0;
        if (hasSubTag(rootTag, "haxelib")) tagKindsFound++;
        if (hasSources) tagKindsFound++;
        if (hasSubTag(rootTag, "app")) tagKindsFound++;
        if (hasSubTag(rootTag, "meta")) tagKindsFound++;
        // two kinds of lime/openfl elements: a maven pom also starts with <project>
        return tagKindsFound >= 2;
    }

    private boolean hasSubTag(XmlTag tag, String name) {
        return tag.findSubTags(name).length > 0;
    }

    public boolean isOpenFlFile(@NotNull XmlFile file) {
        if(!hasProjectFileExtension(file)) return false;

        XmlDocument document = file.getDocument();
        if(document == null) return false;

        XmlTag rootTag = document.getRootTag();
        if(rootTag == null) return false;

        if (!looksLikeALimeProjectFile(rootTag)) return false;

        return LimeOpenFlUtil.isOpenfl(rootTag);
    }

    public boolean isLimeFile(@NotNull XmlFile file) {
        if(!hasProjectFileExtension(file)) return false;

        XmlDocument document = file.getDocument();
        if (document == null) return false;

        XmlTag rootTag = document.getRootTag();
        if (rootTag == null) return false;

        if (!looksLikeALimeProjectFile(rootTag)) return false;

        return !LimeOpenFlUtil.isOpenfl(rootTag);
    }

    private boolean hasProjectFileExtension(@NotNull XmlFile file) {
        String extension = FileUtilRt.getExtension(file.getName()).toLowerCase(Locale.ROOT);
        return HaxeProjectFileDetectionUtil.LIME_XML_EXTENSIONS.contains(extension);
    }

}
