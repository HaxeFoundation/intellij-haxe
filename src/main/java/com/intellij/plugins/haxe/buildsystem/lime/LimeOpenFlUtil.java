package com.intellij.plugins.haxe.buildsystem.lime;

import com.intellij.plugins.haxe.buildsystem.ProjectXmlVocabulary;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class LimeOpenFlUtil {

  /** Whether the file is a Lime or OpenFL project file: under a Lime extension with the lime-family shape. */
  public boolean isLimeFamilyFile(@NotNull XmlFile file) {
    LimeProjectShape shape = shapeOf(file);
    return shape != null && shape.isLimeFamily();
  }

  /**
   * Whether the file is a library's include file in the Lime dialect: named as
   * lime looks for it in a library's folder, with a root lime reads, or rooted
   * in {@code <extension>} with a library or a source path declared.
   */
  public boolean isLibraryIncludeFile(@NotNull XmlFile file) {
    LimeProjectShape shape = shapeOf(file);
    if (shape == null) return false;
    return ProjectXmlVocabulary.isLibraryIncludeFileName(file.getName())
           ? shape.hasToolRoot()
           : shape.isExtensionInclude();
  }

  public boolean isOpenFlFile(@NotNull XmlFile file) {
    LimeProjectShape shape = shapeOf(file);
    return shape != null && shape.isOpenfl();
  }

  public boolean isLimeFile(@NotNull XmlFile file) {
    LimeProjectShape shape = shapeOf(file);
    return shape != null && shape.isLime();
  }

  /** The file's shape, or null when its extension or its document rules it out. */
  @Nullable
  private LimeProjectShape shapeOf(@NotNull XmlFile file) {
    if (!LimeProjectShape.hasFileExtension(file.getName())) return null;
    XmlDocument document = file.getDocument();
    XmlTag rootTag = document == null ? null : document.getRootTag();
    return rootTag == null ? null : LimeProjectShape.of(rootTag);
  }
}
