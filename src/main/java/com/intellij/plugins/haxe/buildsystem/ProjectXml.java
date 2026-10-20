package com.intellij.plugins.haxe.buildsystem;

import com.intellij.util.xml.DomElement;

/**
 * The DOM model of Lime, OpenFL and NME project files. Each kind registers its
 * own empty subtype as root element class: the platform's file icon provider
 * caches a file's description as its root element class name and restores it
 * by that name, so descriptions sharing one class get mixed up.
 */
public interface ProjectXml extends DomElement {
}
