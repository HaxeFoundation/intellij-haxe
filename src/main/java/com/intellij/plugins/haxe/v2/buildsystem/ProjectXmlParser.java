package com.intellij.plugins.haxe.v2.buildsystem;

import com.intellij.plugins.haxe.buildsystem.ProjectXmlVocabulary;
import com.intellij.plugins.haxe.buildsystem.lime.LimeProjectShape;
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

  /**
   * The shape of the text for classifying a file before its PSI exists: the
   * root element, and the library and source elements below it however nested.
   */
  @NotNull
  public static LimeProjectShape parseShape(@NotNull String content) {
    LimeProjectShape.Builder shape = new LimeProjectShape.Builder();
    try {
      XMLStreamReader reader = readerOf(content);
      boolean rootSeen = false;
      while (reader.hasNext()) {
        if (reader.next() != XMLStreamConstants.START_ELEMENT) continue;
        String element = reader.getLocalName();
        if (!rootSeen) {
          shape.root(element);
          rootSeen = true;
          continue;
        }
        shape.element(element);
        shape.library(element, attribute(reader, ProjectXmlVocabulary.NAME_ATTRIBUTE));
      }
    }
    catch (XMLStreamException e) {
      // malformed or partially edited file - keep whatever was collected so far
      if (log.isDebugEnabled()) log.debug("Failed to parse project xml: " + e.getMessage());
    }
    return shape.build();
  }

  @NotNull
  public static HaxeBuildFileInfo parse(@NotNull String content) {
    List<HaxeDefine> defines = new ArrayList<>();
    List<HaxeLibDependency> libraries = new ArrayList<>();
    List<String> classpaths = new ArrayList<>();

    try {
      XMLStreamReader reader = readerOf(content);
      while (reader.hasNext()) {
        if (reader.next() == XMLStreamConstants.START_ELEMENT) {
          handleElement(reader, defines, libraries, classpaths);
        }
      }
    }
    catch (XMLStreamException e) {
      // malformed or partially edited file - keep whatever was collected so far
      if (log.isDebugEnabled()) log.debug("Failed to parse project xml: " + e.getMessage());
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
    return appAttribute(content, ProjectXmlVocabulary.PATH_ATTRIBUTE);
  }

  /**
   * The {@code <include>} project files the file merges, as spelled
   * ({@code path}, or legacy {@code name}), in file order and regardless of
   * {@code if}/{@code unless}. A {@code haxelib} include is left out: lime
   * reads that library's own include file and ignores a path beside it.
   */
  @NotNull
  public static List<String> parseIncludePaths(@NotNull String content) {
    List<String> includes = new ArrayList<>();
    try {
      XMLStreamReader reader = readerOf(content);
      while (reader.hasNext()) {
        if (reader.next() != XMLStreamConstants.START_ELEMENT) continue;
        if (!ProjectXmlVocabulary.INCLUDE_ELEMENT.equals(reader.getLocalName().toLowerCase(Locale.ROOT))) continue;
        if (attribute(reader, ProjectXmlVocabulary.HAXELIB_ATTRIBUTE) != null) continue;
        String path = pathOrLegacyName(reader);
        if (path != null) {
          includes.add(path);
        }
      }
    }
    catch (XMLStreamException e) {
      if (log.isDebugEnabled()) log.debug("Failed to parse project xml: " + e.getMessage());
    }
    return List.copyOf(includes);
  }

  @Nullable
  private static String appAttribute(@NotNull String content, @NotNull String attributeName) {
    try {
      XMLStreamReader reader = readerOf(content);
      while (reader.hasNext()) {
        if (reader.next() != XMLStreamConstants.START_ELEMENT) continue;
        if (!ProjectXmlVocabulary.APP_ELEMENT.equals(reader.getLocalName().toLowerCase(Locale.ROOT))) continue;
        String value = StringUtil.nullize(attribute(reader, attributeName), true);
        if (value != null) {
          return value.trim();
        }
      }
    }
    catch (XMLStreamException e) {
      if (log.isDebugEnabled()) log.debug("Failed to parse project xml: " + e.getMessage());
    }
    return null;
  }

  private static void handleElement(@NotNull XMLStreamReader reader,
                                    @NotNull List<HaxeDefine> defines,
                                    @NotNull List<HaxeLibDependency> libraries,
                                    @NotNull List<String> classpaths) {
    String tag = reader.getLocalName().toLowerCase(Locale.ROOT);
    if (tag.equals(ProjectXmlVocabulary.LIBRARY_ELEMENT)) {
      String name = attribute(reader, ProjectXmlVocabulary.NAME_ATTRIBUTE);
      if (name != null) {
        libraries.add(new HaxeLibDependency(name, StringUtil.nullize(attribute(reader, ProjectXmlVocabulary.VERSION_ATTRIBUTE))));
      }
    }
    else if (ProjectXmlVocabulary.DEFINE_ELEMENTS.contains(tag)) {
      String name = attribute(reader, ProjectXmlVocabulary.NAME_ATTRIBUTE);
      if (name != null) {
        defines.add(new HaxeDefine(name, StringUtil.nullize(attribute(reader, ProjectXmlVocabulary.VALUE_ATTRIBUTE))));
      }
    }
    else if (ProjectXmlVocabulary.SOURCE_ELEMENTS.contains(tag)) {
      String path = pathOrLegacyName(reader);
      if (path != null) {
        classpaths.add(path);
      }
    }
  }

  /** The element's {@code path} attribute, else its legacy {@code name} spelling; null when neither carries a value. */
  @Nullable
  private static String pathOrLegacyName(@NotNull XMLStreamReader reader) {
    String path = attribute(reader, ProjectXmlVocabulary.PATH_ATTRIBUTE);
    if (path == null) path = attribute(reader, ProjectXmlVocabulary.NAME_ATTRIBUTE);
    return path == null || path.isBlank() ? null : path.trim();
  }

  @Nullable
  private static String attribute(@NotNull XMLStreamReader reader, @NotNull String name) {
    return reader.getAttributeValue(null, name);
  }

  /** A reader over the text as the tools read it: attribute values may hold a raw {@code <} or {@code &} ({@link ProjectXmlEscaping}). */
  @NotNull
  private static XMLStreamReader readerOf(@NotNull String content) throws XMLStreamException {
    String escaped = ProjectXmlEscaping.escapeAttributeValues(content);
    return createSecureFactory().createXMLStreamReader(new StringReader(escaped));
  }

  @NotNull
  private static XMLInputFactory createSecureFactory() {
    XMLInputFactory factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    return factory;
  }
}
