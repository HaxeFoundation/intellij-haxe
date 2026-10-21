package com.intellij.plugins.haxe.buildsystem.lime;

import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.plugins.haxe.buildsystem.ProjectXmlVocabulary;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Set;

/**
 * What makes an xml document a Lime/OpenFL project file: a {@code <project>}
 * root with a library or a source path declared somewhere below it (a maven
 * pom or an ant build is rooted in {@code <project>} too, but carries
 * neither). A library's include file may carry an {@code <extension>} root
 * instead; lime reads it like a project, but it is never a build of its own.
 * Both recognisers build one, the PSI side from the document and the
 * build-file scanner from the text (ProjectXmlParser), and ask it, so they
 * cannot disagree.
 */
public record LimeProjectShape(boolean projectRoot, boolean extensionRoot, boolean haxelib, boolean openflLib, boolean sources) {

  /** The extensions of Lime/OpenFL project files: {@code .xml} and Lime's own {@code .lime} (Lime also reads {@code .nmml}, which is classified as NME). */
  public static final Set<String> FILE_EXTENSIONS = Set.of("xml", "lime");
  public static final LimeProjectShape NONE = new LimeProjectShape(false, false, false, false, false);
  private static final String OPENFL_LIBRARY = "openfl";

  public static boolean hasFileExtension(@NotNull String fileName) {
    return FILE_EXTENSIONS.contains(FileUtilRt.getExtension(fileName).toLowerCase(Locale.ROOT));
  }

  /** The shape of a parsed document: the root and every element below it, however nested. */
  @NotNull
  public static LimeProjectShape of(@NotNull XmlTag root) {
    Builder builder = new Builder();
    builder.root(root.getLocalName());
    collect(root, builder);
    return builder.build();
  }

  private static void collect(@NotNull XmlTag tag, @NotNull Builder builder) {
    for (XmlTag child : tag.getSubTags()) {
      builder.element(child.getLocalName());
      builder.library(child.getLocalName(), child.getAttributeValue(ProjectXmlVocabulary.NAME_ATTRIBUTE));
      collect(child, builder);
    }
  }

  public boolean isLimeFamily() {
    return projectRoot && declaresContent();
  }

  /** Whether the root is one lime reads: {@code <project>}, or the {@code <extension>} of a library's include file. */
  public boolean hasToolRoot() {
    return projectRoot || extensionRoot;
  }

  /** Whether the shape alone marks a library's include file: an {@code <extension>} root declaring a library or a source path. */
  public boolean isExtensionInclude() {
    return extensionRoot && declaresContent();
  }

  public boolean isOpenfl() {
    return isLimeFamily() && openflLib;
  }

  public boolean isLime() {
    return isLimeFamily() && !openflLib;
  }

  private boolean declaresContent() {
    return haxelib || sources;
  }

  /** Collects a shape element by element; {@link #of} feeds it from the PSI, the project.xml parser from its stream. */
  public static final class Builder {
    private boolean projectRoot;
    private boolean extensionRoot;
    private boolean haxelib;
    private boolean openflLib;
    private boolean sources;

    public void root(@NotNull String elementName) {
      projectRoot = ProjectXmlVocabulary.ROOT_ELEMENT.equalsIgnoreCase(elementName);
      extensionRoot = ProjectXmlVocabulary.EXTENSION_ROOT_ELEMENT.equalsIgnoreCase(elementName);
    }

    /** An element below the root. */
    public void element(@NotNull String elementName) {
      String name = elementName.toLowerCase(Locale.ROOT);
      haxelib |= ProjectXmlVocabulary.LIBRARY_ELEMENT.equals(name);
      sources |= ProjectXmlVocabulary.SOURCE_ELEMENTS.contains(name);
    }

    /** The {@code name} attribute of an element below the root; only a library element's counts. */
    public void library(@NotNull String elementName, @Nullable String libraryName) {
      openflLib |= ProjectXmlVocabulary.LIBRARY_ELEMENT.equalsIgnoreCase(elementName) && OPENFL_LIBRARY.equalsIgnoreCase(libraryName);
    }

    @NotNull
    public LimeProjectShape build() {
      return new LimeProjectShape(projectRoot, extensionRoot, haxelib, openflLib, sources);
    }
  }
}
