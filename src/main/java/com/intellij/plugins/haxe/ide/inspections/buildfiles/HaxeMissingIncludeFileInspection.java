package com.intellij.plugins.haxe.ide.inspections.buildfiles;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.buildsystem.ProjectXmlVocabulary;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeProjectXmlFiles;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildFileIncludes;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.XmlElementVisitor;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

/// An `<include path>` (legacy `name`) of a Lime, OpenFL or NME project file,
/// or of a library's include file, naming nothing on disk: the path itself, or a folder without the
/// include.lime / include.nmml / include.xml Lime picks inside it. Paths
/// resolve against the project file's folder ([HaxeBuildFileIncludes#includeFile]).
/// A `${...}` path takes Lime's substitution and is not checked;
/// `<include haxelib>` names a library, not a path.
public class HaxeMissingIncludeFileInspection extends LocalInspectionTool {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    PsiFile file = holder.getFile();
    if (!HaxeProjectXmlFiles.isProjectOrLibraryIncludeXml(file)) return PsiElementVisitor.EMPTY_VISITOR;
    VirtualFile folder = HaxeProjectXmlFiles.folderOf(file);
    return folder == null ? PsiElementVisitor.EMPTY_VISITOR : new IncludeVisitor(holder, folder);
  }

  private static final class IncludeVisitor extends XmlElementVisitor {
    private final ProblemsHolder holder;
    private final VirtualFile folder;

    private IncludeVisitor(@NotNull ProblemsHolder holder, @NotNull VirtualFile folder) {
      this.holder = holder;
      this.folder = folder;
    }

    @Override
    public void visitXmlTag(@NotNull XmlTag tag) {
      if (!ProjectXmlVocabulary.INCLUDE_ELEMENT.equalsIgnoreCase(tag.getLocalName())) return;
      if (tag.getAttribute(ProjectXmlVocabulary.HAXELIB_ATTRIBUTE) != null) return;
      XmlAttribute pathAttribute = ProjectXmlVocabulary.pathAttribute(tag);
      XmlAttributeValue pathValue = pathAttribute == null ? null : pathAttribute.getValueElement();
      if (pathValue == null) return;
      String path = pathValue.getValue().trim();
      if (path.isEmpty() || ProjectXmlVocabulary.isToolExpanded(path)) return;
      if (HaxeBuildFileIncludes.includeFile(folder, path) != null) return;
      String message = HaxeBundle.message("haxe.inspections.missing.include.message", path);
      holder.registerProblem(pathValue, HaxeXmlAttributeValues.valueRange(pathValue), message);
    }  }
}
