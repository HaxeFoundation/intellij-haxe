package com.intellij.plugins.haxe.v2.buildsystem;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
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
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads a build file's declared target, defines and library dependencies
 * statically; call inside a read action. HXP files are Haxe code and report
 * empty info here: {@code HaxeLimeProjectInfoService} evaluates them (and
 * lime/openfl projects with their conditions applied). An hxml's sections
 * and an xml project file's declared info are cached on the file's PSI, so
 * the many readers (tool window, resolve context, define context, library
 * sync, test runs) share one answer until the file, or a file it includes,
 * changes.
 */
@CustomLog
public final class HaxeBuildFileInspector {

  private static final Key<CachedValue<List<String>>> SECTION_CONTENTS = Key.create("haxe.build.file.sections");
  private static final Key<CachedValue<HaxeBuildFileInfo>> PROJECT_XML_INFO = Key.create("haxe.build.file.xml.info");

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

    // exhaustive on purpose: a NEW type must decide its inspection here instead of silently reading as EMPTY
    return switch (buildFile.type()) {
      case OPENFL, LIME, NMML -> List.of(projectXmlInfo(project, buildFile.file()));
      case HXML, HXP_PROJECT, HXP_SCRIPT -> List.of(HaxeBuildFileInfo.EMPTY);
    };
  }

  /** The xml project file's declared info, cached on its PSI; a file without PSI parses afresh. */
  @NotNull
  private static HaxeBuildFileInfo projectXmlInfo(@NotNull Project project, @NotNull VirtualFile file) {
    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
    if (psiFile == null) return parseProjectXml(file);
    return CachedValuesManager.getCachedValue(psiFile, PROJECT_XML_INFO, () -> {
      List<Object> dependencies = new ArrayList<>();
      addTextDependencies(dependencies, project, file);
      return CachedValueProvider.Result.create(parseProjectXml(file), dependencies.toArray());
    });
  }

  @NotNull
  private static HaxeBuildFileInfo parseProjectXml(@NotNull VirtualFile file) {
    String content = loadText(file);
    return content == null ? HaxeBuildFileInfo.EMPTY : ProjectXmlParser.parse(content);
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
   * section, its chain unsplit. Cached on the PSI until the file or a file
   * it includes changes. Call inside a read action.
   */
  @NotNull
  public static List<String> sectionContents(@NotNull Project project, @NotNull VirtualFile buildFile) {
    PsiFile psiFile = PsiManager.getInstance(project).findFile(buildFile);
    if (psiFile == null) {
      // without PSI the chain cannot be split, and nothing holds a cache
      String effective = effectiveContent(buildFile);
      return effective == null ? List.of() : List.of(effective);
    }
    CachedValueProvider<List<String>> provider = () -> computeSectionContents(project, psiFile, buildFile);
    return CachedValuesManager.getCachedValue(psiFile, SECTION_CONTENTS, provider);
  }

  @NotNull
  private static CachedValueProvider.Result<List<String>> computeSectionContents(@NotNull Project project,
                                                                                 @NotNull PsiFile psiFile,
                                                                                 @NotNull VirtualFile buildFile) {
    IncludeTracker includes = new IncludeTracker(buildFile);
    List<String> sections = splitAndFlatten(psiFile, buildFile, includes);
    return CachedValueProvider.Result.create(sections, includes.dependencies(project));
  }

  /**
   * What a cached result must follow for one file: its text as
   * {@link #loadText} reads it (the Document while one is cached, else the
   * file), and its PSI. The PsiFile alone is not enough: its stamp moves
   * only when the tree is rebuilt, which trails a reloaded or edited
   * document until the commit, and a read in that window would cache the
   * old text.
   */
  private static void addTextDependencies(@NotNull List<Object> dependencies,
                                          @NotNull Project project,
                                          @NotNull VirtualFile file) {
    Document document = FileDocumentManager.getInstance().getCachedDocument(file);
    dependencies.add(document != null ? document : file);
    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
    if (psiFile != null) dependencies.add(psiFile);
  }

  /** A top-level line of the root build file: its text and the element it came from. */
  private record SectionLine(@NotNull String text, @NotNull PsiElement element) {
  }

  /** The root file's lines by compilation section: the shared {@code --each} block, then the non-empty sections. */
  private record SectionSplit(@NotNull List<SectionLine> eachBlock, @NotNull List<List<SectionLine>> sections) {

    boolean singleSection() {
      return sections.size() <= 1 && eachBlock.isEmpty();
    }
  }

  @NotNull
  private static List<String> splitAndFlatten(@NotNull PsiFile psiFile,
                                              @NotNull VirtualFile buildFile,
                                              @NotNull HxmlFileParser.IncludeReader reader) {
    SectionSplit split = split(psiFile);
    if (split.singleSection()) {
      String content = loadText(buildFile);
      return content == null ? List.of() : List.of(HxmlFileParser.flatten(content, reader));
    }

    String sharedPrefix = split.eachBlock().isEmpty() ? "" : joinedText(split.eachBlock()) + "\n";
    return split.sections().stream()
      .map(lines -> HxmlFileParser.flatten(sharedPrefix + joinedText(lines), reader))
      .toList();
  }

  /**
   * The index into {@link #sectionContents} of the section a top-level line
   * of the root build file belongs to, by the same split: 0 in a
   * single-section file, -1 for a line of the {@code --each} block, which
   * every section shares. Call inside a read action.
   */
  public static int sectionIndexOf(@NotNull PsiFile psiFile, @NotNull PsiElement topLevelLine) {
    SectionSplit split = split(psiFile);
    if (split.singleSection()) return 0;
    if (holds(split.eachBlock(), topLevelLine)) return -1;
    List<List<SectionLine>> sections = split.sections();
    for (int index = 0; index < sections.size(); index++) {
      if (holds(sections.get(index), topLevelLine)) return index;
    }
    return 0;
  }

  @NotNull
  private static SectionSplit split(@NotNull PsiFile psiFile) {
    List<SectionLine> eachBlock = new ArrayList<>();
    List<SectionLine> currentSection = new ArrayList<>();
    List<List<SectionLine>> sections = new ArrayList<>();
    sections.add(currentSection);
    for (PsiElement child : psiFile.getChildren()) {
      if (isIgnorableInSection(child)) continue;
      if (child instanceof HXMLProperty property) {
        String key = property.getKey().getText();
        // a value on the separator's own line is the FOLLOWING section's
        // first argument
        if (HxmlFileParser.isNextSeparator(key)) {
          currentSection = sectionStartingWith(property.getValue());
          sections.add(currentSection);
          continue;
        }
        if (HxmlFileParser.isEachSeparator(key)) {
          eachBlock.addAll(currentSection);
          currentSection.clear();
          currentSection.addAll(sectionStartingWith(property.getValue()));
          continue;
        }
      }
      currentSection.add(new SectionLine(child.getText(), child));
    }
    sections.removeIf(List::isEmpty);
    return new SectionSplit(eachBlock, sections);
  }

  @NotNull
  private static List<SectionLine> sectionStartingWith(@Nullable HXMLValue value) {
    List<SectionLine> lines = new ArrayList<>();
    if (value != null) {
      lines.add(new SectionLine(value.getText(), value));
    }
    return lines;
  }

  @NotNull
  private static String joinedText(@NotNull List<SectionLine> lines) {
    return lines.stream()
      .map(SectionLine::text)
      .collect(Collectors.joining("\n"));
  }

  private static boolean holds(@NotNull List<SectionLine> lines, @NotNull PsiElement element) {
    return lines.stream().anyMatch(line -> line.element() == element);
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
    return content == null ? null : HxmlFileParser.flatten(content, new IncludeTracker(buildFile));
  }

  /**
   * Resolves includes next to the root file, as haxe does, and remembers
   * every file it read so a cached result can depend on them.
   */
  private static final class IncludeTracker implements HxmlFileParser.IncludeReader {
    private final VirtualFile root;
    private final Set<VirtualFile> read = new LinkedHashSet<>();

    private IncludeTracker(@NotNull VirtualFile root) {
      this.root = root;
    }

    @Override
    public @Nullable String read(@NotNull String relativePath) {
      VirtualFile parent = root.getParent();
      VirtualFile included = parent == null ? null : parent.findFileByRelativePath(relativePath);
      if (included == null) return null;
      read.add(included);
      return loadText(included);
    }

    /** The root file's text dependencies and those of every include read. */
    Object @NotNull [] dependencies(@NotNull Project project) {
      List<Object> dependencies = new ArrayList<>();
      addTextDependencies(dependencies, project, root);
      for (VirtualFile include : read) {
        addTextDependencies(dependencies, project, include);
      }
      return dependencies.toArray();
    }
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
      if (log.isDebugEnabled()) log.debug("Unable to read build file " + file.getPath());
      return null;
    }
  }
}
