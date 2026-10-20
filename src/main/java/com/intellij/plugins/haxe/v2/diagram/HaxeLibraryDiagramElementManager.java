package com.intellij.plugins.haxe.v2.diagram;

import com.intellij.diagram.AbstractDiagramElementManager;
import com.intellij.diagram.DiagramBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeDefine;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibraryGraph;
import com.intellij.plugins.haxe.v2.diagram.HaxeLibraryDiagramElement.Define;
import com.intellij.ui.SimpleColoredText;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * How the diagram presents an element: the root element comes out of the
 * opener's data context, a box is titled with the file or library name, and
 * its items are compiler defines - the ones a file's own lines declare, for a
 * library its {@code -D name=version} and the defines of its extraParams.hxml,
 * and for a library's include file the ones the build takes from it - which
 * the Defines category toggles.
 */
final class HaxeLibraryDiagramElementManager extends AbstractDiagramElementManager<HaxeLibraryDiagramElement> {

  @Override
  public @Nullable HaxeLibraryDiagramElement findInDataContext(@NotNull DataContext context) {
    return HaxeLibraryDiagramElement.DATA_KEY.getData(context);
  }

  @Override
  public @NotNull Collection<HaxeLibraryDiagramElement> findElementsInDataContext(@NotNull DataContext context) {
    HaxeLibraryDiagramElement element = findInDataContext(context);
    return element == null ? List.of() : List.of(element);
  }

  @Override
  public boolean isAcceptableAsNode(@Nullable Object element) {
    return element instanceof HaxeLibraryDiagramElement;
  }

  @Override
  public @NotNull String getElementTitle(@NotNull HaxeLibraryDiagramElement element) {
    if (!element.isBuildFile()) return element.key();
    String includeTitle = HaxeLibraryIncludeFiles.titleOf(element.key());
    // otherwise the file name: everything up to the last slash dropped
    return includeTitle != null ? includeTitle : FileUtil.toSystemIndependentName(element.key()).replaceAll(".*/", "");
  }

  @Override
  public @Nullable String getNodeTooltip(@NotNull HaxeLibraryDiagramElement element) {
    return element.isBuildFile() ? element.key() : null;
  }

  /** The items need the project, which only the builder-aware overload has. */
  @Override
  public Object @NotNull [] getNodeItems(@NotNull HaxeLibraryDiagramElement element) {
    return new Object[0];
  }

  @Override
  public Object @NotNull [] getNodeItems(@NotNull HaxeLibraryDiagramElement element, @NotNull DiagramBuilder builder) {
    if (builder.getDataModel() instanceof HaxeLibraryDiagramDataModel model) {
      List<Define> fileDefines = model.fileDefines(element);
      if (fileDefines != null) return fileDefines.toArray();
    }
    return libraryDefines(builder.getProject(), element).toArray();
  }

  /** The defines a library contributes, from the last sync; none for a file the model has not read. */
  @NotNull
  private static List<Define> libraryDefines(@NotNull Project project, @NotNull HaxeLibraryDiagramElement element) {
    List<Define> defines = new ArrayList<>();
    if (element.isBuildFile()) return defines;
    HaxeLibraryGraph.Library library = HaxeLibraryGraph.getInstance(project).library(element.key());
    if (library == null) return defines;
    defines.add(new Define(library.name(), library.version()));
    for (HaxeDefine define : library.extraDefines()) {
      defines.add(new Define(define.name(), define.value()));
    }
    return defines;
  }

  @Override
  public @Nullable SimpleColoredText getItemName(@Nullable HaxeLibraryDiagramElement element, @Nullable Object item, @NotNull DiagramBuilder builder) {
    if (!(item instanceof Define define)) return null;
    SimpleColoredText text = new SimpleColoredText(define.name(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
    if (define.value() != null) {
      text.append("=" + define.value(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }
    return text;
  }

  @Override
  public @Nullable Icon getItemIcon(@Nullable HaxeLibraryDiagramElement element, @Nullable Object item, @NotNull DiagramBuilder builder) {
    return item instanceof Define ? AllIcons.Nodes.Parameter : null;
  }

  @Override
  public boolean canCollapse(@NotNull HaxeLibraryDiagramElement element) {
    return false;
  }

  @Override
  public boolean isContainerFor(@NotNull HaxeLibraryDiagramElement container, @NotNull HaxeLibraryDiagramElement element) {
    return false;
  }

  @Override
  public @NotNull String getEditorTitle(@Nullable HaxeLibraryDiagramElement element, @NotNull Collection<HaxeLibraryDiagramElement> additionalElements) {
    String subject = element == null ? "" : getElementTitle(element);
    return HaxeBundle.message("haxe.library.diagram.editor.title", subject);
  }
}
