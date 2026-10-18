package com.intellij.plugins.haxe.v2.editor;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.buildtools.context.HaxeResolveContext;
import com.intellij.plugins.haxe.v2.buildtools.context.HaxeResolveContextLabels;
import com.intellij.plugins.haxe.v2.buildtools.context.HaxeResolveContexts;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeDefineContextService;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildSettingsListener;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeResolveContextListener;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeResolveContextStore;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerCaches;
import com.intellij.util.messages.MessageBusConnection;
import kotlinx.coroutines.CoroutineScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The status-bar switcher for the build a Haxe file is analysed under:
 * "Context: build.hxml [hl]". Its popup lists every build containing the
 * file, the current one checked, plus "Select automatically" while the file
 * is pinned or shows a followed context. Hidden for non-Haxe files and for
 * files no build contains.
 */
// TODO: multiverse: the IDE's editor switcher + a CodeInsightContextPresentationProvider replace this widget
public final class HaxeResolveContextWidget extends EditorBasedStatusBarPopup {
  public static final String ID = "HaxeResolveContext";

  /** A popup row: a context to pin, or the automatic choice ({@code context} null). */
  private record Choice(@Nullable HaxeResolveContext context, @NotNull String text) {
  }

  public HaxeResolveContextWidget(@NotNull Project project, @NotNull CoroutineScope scope) {
    super(project, false, scope);
  }

  @Override
  public @NotNull String ID() {
    return ID;
  }

  @Override
  protected @NotNull StatusBarWidget createInstance(@NotNull Project project) {
    return new HaxeResolveContextWidget(project, getScope());
  }

  @Override
  public void install(@NotNull StatusBar statusBar) {
    super.install(statusBar);
    // a context choice or a build setting (active file, target, section) changes the answer
    MessageBusConnection connection = getProject().getMessageBus().connect(this);
    connection.subscribe(HaxeResolveContextListener.TOPIC, (HaxeResolveContextListener)this::update);
    connection.subscribe(HaxeBuildSettingsListener.TOPIC, HaxeBuildSettingsListener.onAny(this::update));
  }

  /**
   * The context depends on the file and the build settings, never on what is
   * typed into it: the base's per-edit update is skipped.
   */
  @Override
  protected void updateForDocument(@Nullable Document document) {
  }

  @Override
  protected @NotNull WidgetState getWidgetState(@Nullable VirtualFile file) {
    if (file == null || !FileTypeRegistry.getInstance().isFileOfType(file, HaxeFileType.INSTANCE)) {
      return WidgetState.HIDDEN;
    }
    return HaxeReadActions.compute(() -> stateFor(file));
  }

  @NotNull
  private WidgetState stateFor(@NotNull VirtualFile file) {
    HaxeResolveContexts contexts = HaxeResolveContexts.getInstance(getProject());
    List<HaxeResolveContext> reaching = contexts.contextsReaching(file);
    HaxeResolveContext current = contexts.contextFor(file);
    if (current == null || reaching.isEmpty()) {
      return WidgetState.HIDDEN;
    }
    boolean pinned = contexts.isPinned(file);
    String label = HaxeResolveContextLabels.label(getProject(), current);
    String tooltip = tooltipFor(file, contexts, pinned, reaching.size());
    WidgetState state = new WidgetState(tooltip, HaxeBundle.message("haxe.resolve.context.widget.text", label), reaching.size() > 1 || pinned);
    if (pinned) {
      state.setIcon(AllIcons.General.Pin_tab);
    }
    return state;
  }

  @NotNull
  private static String tooltipFor(@NotNull VirtualFile file, @NotNull HaxeResolveContexts contexts, boolean pinned, int reaching) {
    if (pinned) return HaxeBundle.message("haxe.resolve.context.widget.tooltip.pinned");
    if (contexts.isFollowed(file)) return HaxeBundle.message("haxe.resolve.context.widget.tooltip.followed");
    return HaxeBundle.message("haxe.resolve.context.widget.tooltip.auto", reaching);
  }

  @Override
  protected @Nullable ListPopup createPopup(@NotNull DataContext context) {
    VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(context);
    if (file == null) return null;
    List<Choice> choices = HaxeReadActions.compute(() -> choicesFor(file));
    if (choices.isEmpty()) return null;
    HaxeResolveContext current = HaxeReadActions.compute(() -> HaxeResolveContexts.getInstance(getProject()).contextFor(file));
    return JBPopupFactory.getInstance().createListPopup(new ChoiceStep(file, choices, current));
  }

  /** The builds containing the file, then "Select automatically" while a pin or a followed context applies. */
  @NotNull
  private List<Choice> choicesFor(@NotNull VirtualFile file) {
    HaxeResolveContexts contexts = HaxeResolveContexts.getInstance(getProject());
    List<Choice> choices = new ArrayList<>();
    for (HaxeResolveContext context : contexts.contextsReaching(file)) {
      choices.add(new Choice(context, HaxeResolveContextLabels.label(getProject(), context)));
    }
    if (contexts.isPinned(file) || contexts.isFollowed(file)) {
      choices.add(new Choice(null, HaxeBundle.message("haxe.resolve.context.popup.auto")));
    }
    return choices;
  }

  private void apply(@NotNull VirtualFile file, @NotNull Choice choice) {
    HaxeResolveContextStore store = HaxeResolveContextStore.getInstance(getProject());
    if (choice.context() == null) {
      store.unpin(file);
    }
    else {
      store.pin(file, choice.context().path());
    }
    // the file's tree and its compiler-backed answers (diagnostics, resolve fallback) belong to the previous context
    HaxeDefineContextService.getInstance(getProject()).relexFile(file);
    HaxeCompilerCaches.clearAndRehighlight(getProject(), "haxe: resolve context changed");
  }

  private final class ChoiceStep extends BaseListPopupStep<Choice> {
    private final VirtualFile file;
    private final HaxeResolveContext current;

    ChoiceStep(@NotNull VirtualFile file, @NotNull List<Choice> choices, @Nullable HaxeResolveContext current) {
      super(HaxeBundle.message("haxe.resolve.context.popup.title"), choices);
      this.file = file;
      this.current = current;
    }

    @Override
    public @NotNull String getTextFor(Choice value) {
      return value.text();
    }

    @Override
    public @Nullable Icon getIconFor(Choice value) {
      return value.context() != null && Objects.equals(value.context(), current) ? AllIcons.Actions.Checked : null;
    }

    @Override
    public @Nullable PopupStep<?> onChosen(Choice selectedValue, boolean finalChoice) {
      return doFinalStep(() -> apply(file, selectedValue));
    }
  }
}
