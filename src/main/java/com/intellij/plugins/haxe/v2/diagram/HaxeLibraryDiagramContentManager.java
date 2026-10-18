package com.intellij.plugins.haxe.v2.diagram;

import com.intellij.diagram.AbstractDiagramNodeContentManager;
import com.intellij.diagram.DiagramBuilder;
import com.intellij.diagram.DiagramCategory;
import com.intellij.icons.AllIcons;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.diagram.HaxeLibraryDiagramElement.Define;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** The diagram's one toggle: Defines, listing each box's compiler defines; on by default. */
final class HaxeLibraryDiagramContentManager extends AbstractDiagramNodeContentManager {

  static final DiagramCategory DEFINES =
    new DiagramCategory(() -> HaxeBundle.message("haxe.library.diagram.category.defines"), AllIcons.Nodes.Parameter, true, true);
  static final DiagramCategory[] CATEGORIES = {DEFINES};

  @Override
  public DiagramCategory @NotNull [] getContentCategories() {
    return CATEGORIES;
  }

  @Override
  public boolean isInCategory(@Nullable Object nodeElement, @Nullable Object item, @NotNull DiagramCategory category, @Nullable DiagramBuilder builder) {
    return item instanceof Define && DEFINES.equals(category);
  }
}
