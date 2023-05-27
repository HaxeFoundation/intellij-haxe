package com.intellij.plugins.haxe.ide.projectStructure.detection;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xml.NanoXmlUtil;
import lombok.CustomLog;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@CustomLog
@UtilityClass
public class HaxeProjectFileDetectionUtil {

  public boolean isOpenFLProject(VirtualFile file) {
    if (file.isDirectory()) return false;
    if (fileExtensionIs(file, "xml")) {
      return guessProjectType(file) == XmlProjectType.OPENFL;
    }
    return false;
  }

  public boolean isLimeProject(VirtualFile file) {
    if (file.isDirectory()) return false;
    if (fileExtensionIs(file, "xml")) {
      return guessProjectType(file) == XmlProjectType.LIME;
    }
    return false;
  }

  public boolean isHxmlProject(VirtualFile file) {
    if (file.isDirectory()) return false;
    return  fileExtensionIs(file, "hxml");
  }

  public boolean isNMMLProject(VirtualFile file) {
    if (file.isDirectory()) return false;
    // nmml files looks like  lime but has different file extension
    if (fileExtensionIs(file, "nmml")) {
      return guessProjectType(file) == XmlProjectType.LIME;
    }
    return false;
  }

  private XmlProjectType guessProjectType(VirtualFile file) {

    try {
      byte[] data = VfsUtil.loadBytes(file);
      HaxeXmlProjectParser builder = new HaxeXmlProjectParser();
      NanoXmlUtil.parse(new ByteArrayInputStream(data), builder);
      return builder.getProjectType();
    } catch (IOException e) {
      log.warn("Unable to read content of file");
      return XmlProjectType.UNKNOWN;
    }


  }

  public static List<String> sourcePaths(VirtualFile file) {

    try {
      byte[] data = VfsUtil.loadBytes(file);
      HaxeXmlProjectParser builder = new HaxeXmlProjectParser();
      NanoXmlUtil.parse(new ByteArrayInputStream(data), builder);
      return builder.getSources();
    }
    catch (IOException e) {
      log.warn("Unable to read content of file");
      return List.of();
    }
  }

  private enum XmlProjectType {
    OPENFL,
    LIME,
    UNKNOWN
  }

  private static class HaxeXmlProjectParser extends NanoXmlUtil.BaseXmlBuilder {
    String currentElement = null;
    boolean hasOpenFlLib = false;
    boolean hasProjectTag = false;

    List<String> sources = new ArrayList<>();

    @Override
    public void addAttribute(String key, @Nullable String nsPrefix, @Nullable String nsSystemID, String value, String type) throws Exception {
      super.addAttribute(key, nsPrefix, nsSystemID, value, type);
      if (this.currentElement.equals("haxelib")) {
        if (key.equalsIgnoreCase("name") && value.equalsIgnoreCase("openfl")) {
          hasOpenFlLib = true;
        }
      }
      if (this.currentElement.equals("source")) {
        if (key.equalsIgnoreCase("path")) {
          sources.add(value);
        }
      }
    }

    @Override
    public void startElement(String name, @Nullable String nsPrefix, @Nullable String nsSystemID, String systemID, int lineNr) throws Exception {
      super.startElement(name, nsPrefix, nsSystemID, systemID, lineNr);
      currentElement = name;
      if (name.equalsIgnoreCase("project")) {
        hasProjectTag = true;
      }
    }

    public XmlProjectType getProjectType() {
      if (hasProjectTag && hasOpenFlLib) return XmlProjectType.OPENFL;
      if (hasProjectTag) return XmlProjectType.LIME;
      return XmlProjectType.UNKNOWN;
    }

    public List<String> getSources() {
      return sources;
    }

  }

  private static boolean fileExtensionIs(VirtualFile file, String ext) {
    return Optional.ofNullable(file.getExtension())
      .map(s -> s.equalsIgnoreCase(ext))
      .orElse(false);
  }
}