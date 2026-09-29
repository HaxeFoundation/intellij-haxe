package com.intellij.plugins.haxe.v2.buildsystem;

import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeDefine;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeLibDependency;
import com.intellij.openapi.util.text.StringUtil;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * StAX parser for XML-based Haxe project files (Lime/OpenFL project.xml, NMML),
 * collecting haxelib dependencies and defines. Conditional attributes
 * (if / unless) are not evaluated, so every entry in the file is listed;
 * {@code HaxeLimeProjectInfoService} provides the target-accurate evaluation.
 */
@CustomLog
public final class ProjectXmlParser {

  private ProjectXmlParser() {
  }

  @NotNull
  public static HaxeBuildFileInfo parse(@NotNull String content) {
    List<HaxeDefine> defines = new ArrayList<>();
    List<HaxeLibDependency> libraries = new ArrayList<>();
    List<String> classpaths = new ArrayList<>();

    try {
      XMLStreamReader reader = createSecureFactory().createXMLStreamReader(new StringReader(content));
      while (reader.hasNext()) {
        if (reader.next() == XMLStreamConstants.START_ELEMENT) {
          handleElement(reader, defines, libraries, classpaths);
        }
      }
    }
    catch (XMLStreamException e) {
      // malformed or partially edited file - keep whatever was collected so far
      log.debug("Failed to parse project xml: " + e.getMessage());
    }
    return new HaxeBuildFileInfo(null, null, List.copyOf(defines), List.copyOf(libraries), List.copyOf(classpaths));
  }

  /// The `<app file="...">` attribute — the base name of the launcher
  /// executable lime produces (e.g. `NyanCat` → `NyanCat.exe` on
  /// Windows); null when the file declares none.
  @Nullable
  public static String parseAppFile(@NotNull String content) {
    return appAttribute(content, "file");
  }

  /// The `<app path="...">` attribute — the output root the tool exports
  /// every target under (conventionally `Export`); null when the file
  /// declares none (the tools default to `bin`).
  @Nullable
  public static String parseAppPath(@NotNull String content) {
    return appAttribute(content, "path");
  }

  @Nullable
  private static String appAttribute(@NotNull String content, @NotNull String attributeName) {
    try {
      XMLStreamReader reader = createSecureFactory().createXMLStreamReader(new StringReader(content));
      while (reader.hasNext()) {
        if (reader.next() != XMLStreamConstants.START_ELEMENT) continue;
        if (!"app".equals(reader.getLocalName().toLowerCase(Locale.ROOT))) continue;
        String value = StringUtil.nullize(attribute(reader, attributeName), true);
        if (value != null) {
          return value.trim();
        }
      }
    }
    catch (XMLStreamException e) {
      log.debug("Failed to parse project xml: " + e.getMessage());
    }
    return null;
  }

  private static void handleElement(@NotNull XMLStreamReader reader,
                                    @NotNull List<HaxeDefine> defines,
                                    @NotNull List<HaxeLibDependency> libraries,
                                    @NotNull List<String> classpaths) {
    String tag = reader.getLocalName().toLowerCase(Locale.ROOT);
    switch (tag) {
      case "haxelib" -> {
        String name = attribute(reader, "name");
        if (name != null) {
          libraries.add(new HaxeLibDependency(name, StringUtil.nullize(attribute(reader, "version"))));
        }
      }
      case "haxedef", "define" -> {
        String name = attribute(reader, "name");
        if (name != null) {
          defines.add(new HaxeDefine(name, StringUtil.nullize(attribute(reader, "value"))));
        }
      }
      // lime/openfl use <source path>, historic NMML uses <classpath name>
      case "source", "classpath" -> {
        String path = attribute(reader, "path");
        if (path == null) path = attribute(reader, "name");
        if (path != null && !path.isBlank()) {
          classpaths.add(path.trim());
        }
      }
      default -> { }
    }
  }

  @Nullable
  private static String attribute(@NotNull XMLStreamReader reader, @NotNull String name) {
    return reader.getAttributeValue(null, name);
  }

  @NotNull
  private static XMLInputFactory createSecureFactory() {
    XMLInputFactory factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    return factory;
  }
}
