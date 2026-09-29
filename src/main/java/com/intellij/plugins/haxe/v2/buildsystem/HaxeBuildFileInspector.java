package com.intellij.plugins.haxe.v2.buildsystem;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.hxml.psi.HXMLProperty;
import com.intellij.plugins.haxe.hxml.psi.HXMLTypes;
import com.intellij.plugins.haxe.hxml.psi.HXMLValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a build file's declared target, defines and library dependencies
 * statically; call inside a read action. HXP files are Haxe code and report
 * empty info here: {@code HaxeLimeProjectInfoService} evaluates them (and
 * lime/openfl projects with their conditions applied).
 */
@CustomLog
public final class HaxeBuildFileInspector {

  private HaxeBuildFileInspector() {
  }

  /**
   * The build's info; for an hxml with a {@code --next} chain this is the
   * FIRST section — the one the compiler itself answers display requests from.
   * Sections are isolated compilations, so aggregating them would report
   * define/library sets no single build ever has; section-aware consumers use
   * {@link #inspectSections} with a selection instead.
   */
  @NotNull
  public static HaxeBuildFileInfo inspect(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    // inspectSections never answers an empty list (unreadable files come back as one EMPTY info)
    return inspectSections(project, buildFile).getFirst();
  }

  /** One info per {@code --next} compilation section (see {@link #sectionContents}); non-hxml files have one. */
  @NotNull
  public static List<HaxeBuildFileInfo> inspectSections(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    if (buildFile.type() == HaxeBuildFileType.HXML) {
      List<String> sections = sectionContents(project, buildFile.file());
      if (sections.isEmpty()) return List.of(HaxeBuildFileInfo.EMPTY);
      return sections.stream()
        .map(HxmlFileParser::parse)
        .toList();
    }

    String content = loadText(buildFile.file());
    if (content == null) return List.of(HaxeBuildFileInfo.EMPTY);
    // exhaustive on purpose: a NEW type must decide its inspection here instead of silently reading as EMPTY
    return switch (buildFile.type()) {
      case OPENFL, LIME, NMML -> List.of(ProjectXmlParser.parse(content));
      case HXML, HXP_PROJECT, HXP_SCRIPT -> List.of(HaxeBuildFileInfo.EMPTY);
    };
  }

  /**
   * The hxml's {@code --next} compilation sections, split via the HXML
   * grammar's PSI, each expanded to its own EFFECTIVE content: the
   * {@code --each} block applied first, then the section's include references
   * merged independently — a file several sections share expands into every
   * one of them. The ROOT file's chain entries are the sections; an included
   * file's internal {@code --next} travels INSIDE its section (haxe still
   * runs those sub-compilations when the section compiles). A section a lone
   * separator leaves empty is a silent compiler no-op and produces no entry.
   * Single-section files come back as the one whole effective content;
   * unreadable files as an empty list; a file without PSI comes back as one
   * section, its chain unsplit. Call inside a read action.
   */
  @NotNull
  public static List<String> sectionContents(@NotNull Project project, @NotNull VirtualFile buildFile) {
    String effective = effectiveContent(buildFile);
    if (effective == null) return List.of();
    PsiFile psiFile = PsiManager.getInstance(project).findFile(buildFile);
    if (psiFile == null) return List.of(effective);

    List<String> eachBlock = new ArrayList<>();
    List<String> currentSection = new ArrayList<>();
    List<List<String>> sectionLines = new ArrayList<>();
    sectionLines.add(currentSection);
    for (PsiElement child : psiFile.getChildren()) {
      if (isIgnorableInSection(child)) continue;
      if (child instanceof HXMLProperty property) {
        String key = property.getKey().getText();
        // a value on the separator's own line is the FOLLOWING section's
        // first argument
        if (HxmlFileParser.isNextSeparator(key)) {
          currentSection = sectionStartingWith(property.getValue());
          sectionLines.add(currentSection);
          continue;
        }
        if (HxmlFileParser.isEachSeparator(key)) {
          eachBlock.addAll(currentSection);
          currentSection.clear();
          HXMLValue value = property.getValue();
          if (value != null) {
            currentSection.add(value.getText());
          }
          continue;
        }
      }
      currentSection.add(child.getText());
    }

    sectionLines.removeIf(List::isEmpty);
    boolean singleSection = sectionLines.size() <= 1 && eachBlock.isEmpty();
    if (singleSection) return List.of(effective);

    String sharedPrefix = eachBlock.isEmpty() ? "" : String.join("\n", eachBlock) + "\n";
    HxmlFileParser.IncludeReader reader = includeReader(buildFile);
    return sectionLines.stream()
      .map(lines -> HxmlFileParser.flatten(sharedPrefix + String.join("\n", lines), reader))
      .toList();
  }

  @NotNull
  private static List<String> sectionStartingWith(@Nullable HXMLValue value) {
    List<String> lines = new ArrayList<>();
    if (value != null) {
      lines.add(value.getText());
    }
    return lines;
  }

  /** Line breaks and comments carry no compiler arguments. */
  private static boolean isIgnorableInSection(@NotNull PsiElement element) {
    if (element instanceof PsiWhiteSpace) return true;
    IElementType type = element.getNode().getElementType();
    return type == HXMLTypes.CRLF || type == HXMLTypes.COMMENT;
  }

  /**
   * The build file's EFFECTIVE hxml - includes merged the way haxe reads them
   * (every reference resolved next to the root file, which is also the working
   * directory the compile runs in). Null when the file cannot be read.
   */
  @Nullable
  public static String effectiveContent(@NotNull VirtualFile buildFile) {
    String content = loadText(buildFile);
    return content == null ? null : HxmlFileParser.flatten(content, includeReader(buildFile));
  }

  @NotNull
  private static HxmlFileParser.IncludeReader includeReader(@NotNull VirtualFile root) {
    return relativePath -> {
      VirtualFile parent = root.getParent();
      VirtualFile included = parent == null ? null : parent.findFileByRelativePath(relativePath);
      return included == null ? null : loadText(included);
    };
  }

  @Nullable
  public static String loadText(@NotNull VirtualFile file) {
    // An open editor's unsaved changes live in the Document, not the VFS - prefer it
    // so a tree refresh reflects what the user sees without requiring a save.
    Document document = FileDocumentManager.getInstance().getCachedDocument(file);
    if (document != null) {
      return document.getText();
    }
    try {
      return VfsUtilCore.loadText(file);
    }
    catch (IOException e) {
      log.debug("Unable to read build file " + file.getPath());
      return null;
    }
  }
}
