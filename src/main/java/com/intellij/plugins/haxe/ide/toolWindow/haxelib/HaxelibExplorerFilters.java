package com.intellij.plugins.haxe.ide.toolWindow.haxelib;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.plugins.haxe.HaxeBundle;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;

/**
 * The explorer tree's visibility state: INCLUSIVE "show" toggles per VERSION
 * kind — each version node is visible while its kind's toggle is on, and a
 * library node stays only while at least one of its versions would show, so
 * hiding "not installed" prunes the download rows under installed libraries
 * and drops catalog-only ones entirely — plus the two narrowing restrictions
 * (updates available; current selection behind the latest release).
 * Rendered as an always-visible VERTICAL strip of toggle
 * buttons beside the tree, so the applied set is readable at a glance.
 */
final class HaxelibExplorerFilters {

  enum Filter {INSTALLED, NOT_INSTALLED, DEV, GIT, ONLY_UPDATES, BEHIND_LATEST}

  private final Set<Filter> active =
    EnumSet.of(Filter.INSTALLED, Filter.NOT_INSTALLED, Filter.DEV, Filter.GIT);
  private final Runnable onChange;

  HaxelibExplorerFilters(@NotNull Runnable onChange) {
    this.onChange = onChange;
  }

  boolean isActive(@NotNull Filter filter) {
    return active.contains(filter);
  }

  @NotNull
  DefaultActionGroup createToggleGroup() {
    DefaultActionGroup group = new DefaultActionGroup();
    group.add(new FilterToggle(Filter.INSTALLED,
                               () -> HaxeBundle.message("haxelib.explorer.filter.installed"),
                               AllIcons.Actions.Checked));
    group.add(new FilterToggle(Filter.NOT_INSTALLED,
                               () -> HaxeBundle.message("haxelib.explorer.filter.not.installed"),
                               AllIcons.Actions.Download));
    group.add(new FilterToggle(Filter.DEV,
                               () -> HaxeBundle.message("haxelib.explorer.filter.dev"),
                               AllIcons.Nodes.HomeFolder));
    group.add(new FilterToggle(Filter.GIT,
                               () -> HaxeBundle.message("haxelib.explorer.filter.git"),
                               AllIcons.Vcs.Branch));
    group.addSeparator();
    group.add(new FilterToggle(Filter.ONLY_UPDATES,
                               () -> HaxeBundle.message("haxelib.explorer.filter.only.updates"),
                               AllIcons.General.ArrowUp));
    group.add(new FilterToggle(Filter.BEHIND_LATEST,
                               () -> HaxeBundle.message("haxelib.explorer.filter.behind.latest"),
                               AllIcons.Vcs.History));
    return group;
  }

  private final class FilterToggle extends ToggleAction implements DumbAware {
    private final Filter filter;

    private FilterToggle(@NotNull Filter filter, @NotNull Supplier<String> text, @NotNull Icon icon) {
      super(text, icon);
      this.filter = filter;
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
      return active.contains(filter);
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
      if (state) {
        active.add(filter);
      }
      else {
        active.remove(filter);
      }
      onChange.run();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
      return ActionUpdateThread.EDT;
    }
  }
}
