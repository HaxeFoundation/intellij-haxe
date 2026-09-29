package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.display.protocol.DisplayMethods;
import com.intellij.plugins.haxe.display.protocol.MetadataEntry;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.v2.buildtools.server.HaxeCompilationServerManager;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The compiler's metadata registry ({@code display/metadata}): the built-in
 * metadata plus any that libraries registered with
 * {@code Compiler.registerCustomMetadata}. Metadata completion merges the
 * entries into its own. The unused-member inspections use the registry to
 * decide which metadata keeps a member alive: known metadata may be consumed
 * invisibly by a macro, while unknown metadata is likely a typo.
 *
 * Answers from its cache only, so it is safe under the read lock. The
 * registry is hydrated once per server. It belongs to the compiler binary,
 * so it is keyed by the server's port.
 */
@Service(Service.Level.PROJECT)
@CustomLog
public final class HaxeCompilerMetadataService {

  private record Registry(@NotNull List<MetadataEntry> entries, @NotNull Set<String> bareNames) {
  }

  private final Project project;
  private final AtomicBoolean hydrating = new AtomicBoolean();
  // modules with different SDKs run several servers, each with its own registry
  private final Map<Integer, Registry> registries = new ConcurrentHashMap<>();

  public HaxeCompilerMetadataService(@NotNull Project project) {
    this.project = project;
  }

  @NotNull
  public static HaxeCompilerMetadataService getInstance(@NotNull Project project) {
    return project.getService(HaxeCompilerMetadataService.class);
  }

  /**
   * The registry entries, or null while the registry is unavailable or not
   * hydrated yet. Cache-only; call in a read action.
   */
  @Nullable
  public List<MetadataEntry> entries(@NotNull VirtualFile contextFile) {
    Registry known = lookup(contextFile);
    return known != null ? known.entries() : null;
  }

  /** Registry names without their leading colon, or null while unavailable. */
  @Nullable
  public Set<String> knownBareNames(@NotNull VirtualFile contextFile) {
    Registry known = lookup(contextFile);
    return known != null ? known.bareNames() : null;
  }

  public void clearCache() {
    registries.clear();
  }

  @Nullable
  private Registry lookup(@NotNull VirtualFile contextFile) {
    if (!HaxeCompilerSettings.getInstance(project).isCompilerDiagnosticsEnabled()) return null;
    if (DumbService.isDumb(project)) return null;
    HaxeCompilerDisplayService.DisplayContext context = HaxeCompilerDisplayService.getInstance(project).contextFor(contextFile);
    if (context == null) return null;
    // only the registry of this SDK's running server applies
    int currentPort = HaxeCompilationServerManager.getInstance(project).runningPortForSdk(context.sdkName());
    Registry known = currentPort > 0 ? registries.get(currentPort) : null;
    if (known != null) {
      return known;
    }
    scheduleHydration(context);
    return null;
  }

  private void scheduleHydration(@NotNull HaxeCompilerDisplayService.DisplayContext context) {
    if (!hydrating.compareAndSet(false, true)) return;
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      try {
        HaxeCompilerDisplayService.Connected connected =
          HaxeCompilerDisplayService.getInstance(project).connectFor(context, DisplayMethods.METADATA);
        if (connected == null) return;
        List<MetadataEntry> entries = connected.client().metadata(connected.args());
        Set<String> bareNames = entries.stream()
          .map(MetadataEntry::bareName)
          .collect(Collectors.toUnmodifiableSet());
        registries.put(connected.port(), new Registry(entries, bareNames));
        log.info("haxe metadata registry loaded: " + entries.size() + " entries");
        HaxeCompilerCaches.restartHighlightingLater(project, "haxe: compiler metadata updated");
      } catch (DisplayRequestException e) {
        log.info("display/metadata failed: " + e.getMessage());
      } finally {
        hydrating.set(false);
      }
    });
  }
}
