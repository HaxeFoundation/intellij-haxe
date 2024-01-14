package com.intellij.plugins.haxe.haxelib.definitions;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.plugins.haxe.haxelib.definitions.tags.ProjectXmlDefineValue;
import com.intellij.plugins.haxe.haxelib.definitions.tags.ProjectXmlHaxedefValue;
import com.intellij.plugins.haxe.haxelib.definitions.tags.ProjectXmlHaxelibValue;
import com.intellij.plugins.haxe.haxelib.definitions.tags.ProjectXmlUndefineValue;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ProjectXmlDefinitionsUtil {

  @Nullable
  public static XmlFile findProjectXml(Module module, String projectXml) {
    if (projectXml == null) return null;

    VirtualFileSystem vfs = VirtualFileManager.getInstance().getFileSystem(LocalFileSystem.PROTOCOL);
    VirtualFile file = vfs.findFileByPath(projectXml);
    if (file == null) return null;
    PsiFile projectFile = PsiManager.getInstance(module.getProject()).findFile(file);
    if (projectFile instanceof XmlFile xmlFile) {
      return xmlFile;
    }
    return null;
  }

  // TODO merge definition classes to avoid duplicated code like this
  @NotNull
  public static List<ProjectXmlDefineValue> getDefinesFromProjectXmlFile(@NotNull XmlFile xmlFile) {
    List<ProjectXmlDefineValue> defines = new ArrayList<>();
    XmlDocument document = xmlFile.getDocument();

    if (document != null) {
      XmlTag rootTag = document.getRootTag();
      if (rootTag != null) {
        XmlTag[] defineTags = rootTag.findSubTags("define");
        for (XmlTag defineTag : defineTags) {
          String name = defineTag.getAttributeValue("name");
          String ifValue = defineTag.getAttributeValue("if");
          String unlessValue = defineTag.getAttributeValue("unless");
          defines.add(new ProjectXmlDefineValue(name, ifValue, unlessValue));
        }
      }
    }
    return defines;
  }
  @NotNull
  public static List<ProjectXmlHaxedefValue> getHaxeDefFromProjectXmlFile(@NotNull XmlFile xmlFile) {
    List<ProjectXmlHaxedefValue> defines = new ArrayList<>();
    XmlDocument document = xmlFile.getDocument();

    if (document != null) {
      XmlTag rootTag = document.getRootTag();
      if (rootTag != null) {
        XmlTag[] defineTags = rootTag.findSubTags("haxedef");
        for (XmlTag defineTag : defineTags) {
          String name = defineTag.getAttributeValue("name");
          String ifValue = defineTag.getAttributeValue("if");
          String unlessValue = defineTag.getAttributeValue("unless");
          defines.add(new ProjectXmlHaxedefValue(name, ifValue, unlessValue));
        }
      }
    }
    return defines;
  }
  @NotNull
  public static List<ProjectXmlUndefineValue> getUndefinesFromProjectXmlFile(@NotNull XmlFile xmlFile) {
    List<ProjectXmlUndefineValue> defines = new ArrayList<>();
    XmlDocument document = xmlFile.getDocument();

    if (document != null) {
      XmlTag rootTag = document.getRootTag();
      if (rootTag != null) {
        XmlTag[] defineTags = rootTag.findSubTags("undefine");
        for (XmlTag defineTag : defineTags) {
          String name = defineTag.getAttributeValue("name");
          String ifValue = defineTag.getAttributeValue("if");
          String unlessValue = defineTag.getAttributeValue("unless");
          defines.add(new ProjectXmlUndefineValue(name, ifValue, unlessValue));
        }
      }
    }
    return defines;
  }



  @NotNull
  public static List<ProjectXmlHaxelibValue> getLibsFromProjectXmlFile(@NotNull XmlFile xmlFile) {
    List<ProjectXmlHaxelibValue> haxeLibData = new ArrayList<>();
    XmlDocument document = xmlFile.getDocument();

    if (document != null) {
      XmlTag rootTag = document.getRootTag();
      if (rootTag != null) {
        XmlTag[] haxelibTags = rootTag.findSubTags("haxelib");
        for (XmlTag haxelibTag : haxelibTags) {
          String name = haxelibTag.getAttributeValue("name");
          String version = haxelibTag.getAttributeValue("version");
          String ifValue = haxelibTag.getAttributeValue("if");
          String unlessValue = haxelibTag.getAttributeValue("unless");
          haxeLibData.add(new ProjectXmlHaxelibValue(name, version, ifValue, unlessValue));
        }
      }
    }
    return haxeLibData;
  }
}
