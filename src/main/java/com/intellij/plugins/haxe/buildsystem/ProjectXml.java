package com.intellij.plugins.haxe.buildsystem;

import com.intellij.util.xml.DomElement;
import org.jetbrains.annotations.NonNls;

public interface ProjectXml extends DomElement {
    @NonNls String TAG_NAME = "project";

}
