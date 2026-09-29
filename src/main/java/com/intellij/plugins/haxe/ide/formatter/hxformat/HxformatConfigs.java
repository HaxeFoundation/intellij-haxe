package com.intellij.plugins.haxe.ide.formatter.hxformat;

import com.intellij.application.options.CodeStyle;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the project's hxformat.json configs for
 * {@link HxformatSettingsModifier}. It finds the config that governs a file,
 * parses it (cached per path), persists the fallback config the user chose
 * with "Use as Haxe Formatting Rules", and switches the integration on or
 * off.
 * <p>
 * Any VFS change to a file named hxformat.json, and any change of the
 * fallback, clears the parse cache, increments the modification tracker and
 * asks the platform to recompute code style. The tracker invalidates the
 * per-file transient settings built from a config.
 */
@Service(Service.Level.PROJECT)
@State(name = "HaxeHxformatConfig", storages = @Storage("haxeFormatter.xml"))
public final class HxformatConfigs implements PersistentStateComponent<HxformatConfigs.State> {

  public static final class State {
    public String overrideConfigUrl;
  }

  public static final String HXFORMAT_FILE_NAME = "hxformat.json";
  private static final Logger LOG = Logger.getInstance(HxformatConfigs.class);
  // a configured ObjectMapper is thread-safe, so one instance serves every parse
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final Map<String, CachedConfig> parsedByPath = new ConcurrentHashMap<>();
  private final SimpleModificationTracker tracker = new SimpleModificationTracker();
  private final Project project;
  private State state = new State();

  public static HxformatConfigs getInstance(@NotNull Project project) {
    return project.getService(HxformatConfigs.class);
  }

  @Override
  public @NotNull State getState() {
    return state;
  }

  @Override
  public void loadState(@NotNull State state) {
    this.state = state;
  }

  public HxformatConfigs(@NotNull Project project) {
    this.project = project;
    project.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      @Override
      public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
        for (VFileEvent event : events) {
          // match the file NAME - a path ending in "myhxformat.json" is unrelated
          if (!HXFORMAT_FILE_NAME.equals(PathUtil.getFileName(event.getPath()))) continue;
          parsedByPath.clear();
          tracker.incModificationCount();
          CodeStyleSettingsManager.getInstance(project).notifyCodeStyleSettingsChanged();
          return;
        }
      }
    });
  }

  /**
   * The nearest hxformat.json in the file's directory or above it, up to the
   * project base directory; else the chosen fallback config; else null. This
   * is the CLI's upward search, bounded by the project. Content roots are no
   * boundary, so a config at the project root also governs nested modules.
   */
  @Nullable
  public static VirtualFile findConfig(@NotNull Project project, @NotNull VirtualFile file) {
    VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
    for (VirtualFile dir = file.getParent(); dir != null; dir = dir.getParent()) {
      VirtualFile config = dir.findChild(HXFORMAT_FILE_NAME);
      if (config != null && !config.isDirectory()) {
        return config;
      }
      if (dir.equals(projectDir)) {
        break;
      }
    }
    return getInstance(project).overrideConfig();
  }

  /** The chosen fallback config, or null when unset or gone from disk. */
  @Nullable
  public VirtualFile overrideConfig() {
    String url = state.overrideConfigUrl;
    if (StringUtil.isEmpty(url)) return null;
    // a VFS URL, so the config resolves in whatever filesystem holds it
    VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(url);
    return file != null && file.isValid() && !file.isDirectory() ? file : null;
  }

  @Nullable
  public String overrideConfigUrl() {
    return state.overrideConfigUrl;
  }

  /** Sets (or clears, with null) the fallback config and re-triggers code style recalculation. */
  public void setOverrideConfigUrl(@Nullable String url) {
    state.overrideConfigUrl = url;
    parsedByPath.clear();
    tracker.incModificationCount();
    CodeStyleSettingsManager.getInstance(project).notifyCodeStyleSettingsChanged();
  }

  /** Switches the scheme's hxformat integration ({@link HaxeCodeStyleSettings#USE_PROJECT_HXFORMAT}) and re-triggers code style recalculation. */
  public void setIntegrationEnabled(boolean enabled) {
    CodeStyle.getSettings(project).getCustomSettings(HaxeCodeStyleSettings.class).USE_PROJECT_HXFORMAT = enabled;
    CodeStyleSettingsManager.getInstance(project).notifyCodeStyleSettingsChanged();
  }

  /** Incremented whenever an hxformat.json or the fallback choice changes; the transient settings depend on it. */
  public ModificationTracker tracker() {
    return tracker;
  }

  /** The parsed config, or null when the file cannot be read or parsed. */
  @Nullable
  public JsonNode parsed(@NotNull VirtualFile configFile) {
    CachedConfig cached = parsedByPath.get(configFile.getPath());
    if (cached != null && cached.stamp == configFile.getModificationStamp()) {
      return cached.root;
    }
    JsonNode root = null;
    try {
      root = readJsonTree(configFile);
    }
    catch (Exception e) {
      LOG.warn("cannot parse " + configFile.getPath() + ": " + e.getMessage());
    }
    parsedByPath.put(configFile.getPath(), new CachedConfig(configFile.getModificationStamp(), root));
    return root;
  }

  /**
   * The file's JSON tree; loadText honors the file's detected charset/BOM.
   * An unreadable file throws IOException, malformed JSON a JacksonException.
   */
  @NotNull
  static JsonNode readJsonTree(@NotNull VirtualFile file) throws IOException {
    return MAPPER.readTree(VfsUtilCore.loadText(file));
  }

  private record CachedConfig(long stamp, @Nullable JsonNode root) {
  }
}
