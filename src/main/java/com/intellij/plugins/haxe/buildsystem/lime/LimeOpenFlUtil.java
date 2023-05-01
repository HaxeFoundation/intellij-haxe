package com.intellij.plugins.haxe.buildsystem.lime;

import com.intellij.psi.xml.XmlTag;
import lombok.experimental.UtilityClass;

import java.util.Arrays;

@UtilityClass
public class LimeOpenFlUtil {
    public boolean isOpenfl(XmlTag rootTag) {
        return Arrays.stream(rootTag.findSubTags("haxelib")).anyMatch(xmlTag -> "openfl".equalsIgnoreCase(xmlTag.getAttribute("name").getValue()));
    }
}
