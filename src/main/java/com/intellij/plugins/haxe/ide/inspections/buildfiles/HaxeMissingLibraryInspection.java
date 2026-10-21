package com.intellij.plugins.haxe.ide.inspections.buildfiles;

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.buildsystem.hxml.psi.HXMLFile;
import com.intellij.plugins.haxe.hxml.psi.HXMLLib;
import com.intellij.plugins.haxe.hxml.psi.HXMLValue;
import com.intellij.plugins.haxe.hxml.psi.HXMLVisitor;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeLibDependency;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeProjectXmlFiles;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeInstalledLibraries;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeInstalledLibraries.InstalledLibrary;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeInstalledLibraries.LibraryStatus;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxelibMutations;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.XmlElementVisitor;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/// A `-lib` line or `<haxelib name>` naming a library haxelib has not
/// installed, or not in the pinned version. It reads the installed picture
/// the tool window shows ([HaxeInstalledLibraries]): until the first fetch
/// lands it reports nothing and requests one, and a landed fetch re-runs it.
/// The quick fix is the tool window's install.
public class HaxeMissingLibraryInspection extends LocalInspectionTool {
  private static final String HAXELIB_TAG = "haxelib";

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    PsiFile file = holder.getFile();
    boolean buildFile = file instanceof HXMLFile || HaxeProjectXmlFiles.isProjectXml(file);
    if (!buildFile) return PsiElementVisitor.EMPTY_VISITOR;

    Map<String, InstalledLibrary> installed = installedPicture(file.getProject());
    if (installed == null) return PsiElementVisitor.EMPTY_VISITOR;
    MissingLibraryReporter reporter = new MissingLibraryReporter(holder, installed);
    return file instanceof HXMLFile ? new HxmlLibraryVisitor(reporter) : new ProjectXmlLibraryVisitor(reporter);
  }

  /** The installed picture as last fetched; null while there is none, with a fetch requested that re-runs the inspection when it lands. */
  @Nullable
  private static Map<String, InstalledLibrary> installedPicture(@NotNull Project project) {
    HaxeInstalledLibraries libraries = HaxeInstalledLibraries.getInstance(project);
    Map<String, InstalledLibrary> installed = libraries.snapshot();
    if (installed == null) libraries.refreshAsync();
    return installed;
  }

  /** Registers a problem for each dependency the installed picture lacks, with the install as its fix. */
  private record MissingLibraryReporter(@NotNull ProblemsHolder holder, @NotNull Map<String, InstalledLibrary> installed) {

    void check(@NotNull PsiElement element, @NotNull TextRange nameRange, @NotNull HaxeLibDependency library) {
      LibraryStatus status = HaxeInstalledLibraries.status(installed, library);
      if (status.installed()) return;
      InstallLibraryFix fix = new InstallLibraryFix(library.name(), library.version(), status.selectedVersion());
      holder.registerProblem(element, nameRange, message(library, status), fix);
    }

    /** Names the missing version when the library itself is installed. */
    @NotNull
    private static String message(@NotNull HaxeLibDependency library, @NotNull LibraryStatus status) {
      if (status.selectedVersion() != null) {
        return HaxeBundle.message("haxe.inspections.missing.library.version.message", library.name(), library.version());
      }
      return HaxeBundle.message("haxe.inspections.missing.library.message", library.name());
    }
  }

  private static final class HxmlLibraryVisitor extends HXMLVisitor {
    private final MissingLibraryReporter reporter;

    private HxmlLibraryVisitor(@NotNull MissingLibraryReporter reporter) {
      this.reporter = reporter;
    }

    @Override
    public void visitLib(@NotNull HXMLLib lib) {
      HXMLValue value = ContainerUtil.getFirstItem(lib.getValueList());
      if (value == null) return;
      HaxeLibDependency library = HaxeLibDependency.parse(librarySpec(value.getText()));
      reporter.check(value, TextRange.from(0, library.name().length()), library);
    }

    /// The `name[:version]` part of the value: the lexer keeps a trailing comment inside the value token.
    @NotNull
    private static String librarySpec(@NotNull String valueText) {
      int end = StringUtil.indexOfAny(valueText, " \t");
      return end < 0 ? valueText : valueText.substring(0, end);
    }
  }

  private static final class ProjectXmlLibraryVisitor extends XmlElementVisitor {
    private final MissingLibraryReporter reporter;

    private ProjectXmlLibraryVisitor(@NotNull MissingLibraryReporter reporter) {
      this.reporter = reporter;
    }

    @Override
    public void visitXmlTag(@NotNull XmlTag tag) {
      if (!HAXELIB_TAG.equalsIgnoreCase(tag.getLocalName())) return;
      XmlAttributeValue name = HaxeXmlAttributeValues.nameAttributeValue(tag);
      if (name == null || name.getValue().isBlank()) return;
      HaxeLibDependency library = new HaxeLibDependency(name.getValue(), StringUtil.nullize(tag.getAttributeValue("version")));
      reporter.check(name, HaxeXmlAttributeValues.valueRange(name), library);
    }
  }

  /** Runs the tool window's install for the library; the library sync afterwards refreshes the installed picture and with it this inspection. */
  private static final class InstallLibraryFix implements LocalQuickFix {
    private final String name;
    private final @Nullable String version;
    private final @Nullable String selectedVersion;

    private InstallLibraryFix(@NotNull String name, @Nullable String version, @Nullable String selectedVersion) {
      this.name = name;
      this.version = version;
      this.selectedVersion = selectedVersion;
    }

    @Override
    public @NotNull String getName() {
      return HaxeBundle.message("haxe.inspections.missing.library.fix", name);
    }

    @Override
    public @NotNull String getFamilyName() {
      return HaxeBundle.message("haxe.inspections.missing.library.fix.family");
    }

    @Override
    public boolean startInWriteAction() {
      return false;
    }

    @Override
    public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull ProblemDescriptor previewDescriptor) {
      return IntentionPreviewInfo.EMPTY;
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      HaxelibMutations.install(project, name, version, selectedVersion);
    }
  }
}
