package com.intellij.plugins.haxe.ide.projectStructure.detection;

import com.intellij.openapi.util.text.StringUtil;
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
import java.util.Locale;
import java.util.Set;

@CustomLog
@UtilityClass
public class HaxeProjectFileDetectionUtil {

  /** The extensions of Lime/OpenFL project files: {@code .xml} and Lime's own {@code .lime} (Lime also reads {@code .nmml}, which is classified as NME). */
  public static final Set<String> LIME_XML_EXTENSIONS = Set.of("xml", "lime");

  public boolean isOpenFLProject(VirtualFile file) {
    if (!hasLimeXmlExtension(file)) return false;
    return guessProjectType(file) == XmlProjectType.OPENFL;
  }

  public boolean isLimeProject(VirtualFile file) {
    if (!hasLimeXmlExtension(file)) return false;
    return guessProjectType(file) == XmlProjectType.LIME;
  }

  public boolean isHxmlProject(VirtualFile file) {
    return hasExtension(file, "hxml");
  }

  /**
   * Any file under NME's extension with a {@code <project>} element. NME needs no
   * {@code <haxelib>} or {@code <source>} (it adds itself and uses the project
   * folder), and a project declaring the openfl lib is still built by NME.
   */
  public boolean isNMMLProject(VirtualFile file) {
    if (!hasExtension(file, "nmml")) return false;
    HaxeXmlProjectParser parsed = parse(file);
    return parsed != null && parsed.hasProjectTag;
  }

  private XmlProjectType guessProjectType(VirtualFile file) {
    HaxeXmlProjectParser parsed = parse(file);
    return parsed == null ? XmlProjectType.UNKNOWN : parsed.getProjectType();
  }

  public static List<String> sourcePaths(VirtualFile file) {
    HaxeXmlProjectParser parsed = parse(file);
    return parsed == null ? List.of() : parsed.getSources();
  }

  /** The file's project elements; null when it cannot be read. */
  @Nullable
  private static HaxeXmlProjectParser parse(VirtualFile file) {
    try {
      byte[] data = VfsUtil.loadBytes(file);
      HaxeXmlProjectParser builder = new HaxeXmlProjectParser();
      NanoXmlUtil.parse(new ByteArrayInputStream(data), builder);
      return builder;
    }
    catch (IOException e) {
      log.warn("Unable to read content of file");
      return null;
    }
  }

  private enum XmlProjectType {
    OPENFL,
    LIME,
    UNKNOWN
  }

  private static class HaxeXmlProjectParser extends NanoXmlUtil.BaseXmlBuilder {
    // <source path> and its alias <classpath path>; historic nmml spells the latter <classpath name>
    private static final Set<String> SOURCE_ELEMENTS = Set.of("source", "classpath");
    private static final Set<String> SOURCE_PATH_ATTRIBUTES = Set.of("path", "name");

    String currentElement = null;
    boolean hasOpenFlLib = false;
    boolean hasLibTags = false;
    boolean hasProjectTag = false;

    List<String> sources = new ArrayList<>();

    @Override
    public void addAttribute(String key, @Nullable String nsPrefix, @Nullable String nsSystemID, String value, String type) throws Exception {
      super.addAttribute(key, nsPrefix, nsSystemID, value, type);
      if (this.currentElement.equals("haxelib")) {
        hasLibTags = true;
        if (key.equalsIgnoreCase("name") && value.equalsIgnoreCase("openfl")) {
          hasOpenFlLib = true;
        }
      }
      if (SOURCE_ELEMENTS.contains(currentElement) && SOURCE_PATH_ATTRIBUTES.contains(key.toLowerCase(Locale.ROOT))) {
        sources.add(value);
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
      if (hasProjectTag && (hasLibTags || !sources.isEmpty())) return XmlProjectType.LIME;
      return XmlProjectType.UNKNOWN;
    }

    public List<String> getSources() {
      return sources;
    }

  }

  private static boolean hasLimeXmlExtension(@Nullable VirtualFile file) {
    return isFile(file) && LIME_XML_EXTENSIONS.contains(extensionOf(file));
  }

  private static boolean hasExtension(@Nullable VirtualFile file, String extension) {
    return isFile(file) && extensionOf(file).equals(extension);
  }

  private static boolean isFile(@Nullable VirtualFile file) {
    return file != null && !file.isDirectory();
  }

  private static String extensionOf(VirtualFile file) {
    return StringUtil.toLowerCase(StringUtil.notNullize(file.getExtension()));
  }
}
