package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.plugins.haxe.display.protocol.DisplayMethods;
import com.intellij.plugins.haxe.display.protocol.server.HaxeServerContext;
import com.intellij.plugins.haxe.display.protocol.server.ModuleInfo;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension.fqn.HaxeFullyQualifiedClassNameIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.fqn.HaxeFullyQualifiedClassNameStubIndex;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

/**
 * The compiler's catalog of generated types: every type the compilation
 * server knows that exists in NO source file the IDE indexes, i.e. types
 * created by macros ({@code Context.defineType}, {@code defineModule}). The
 * unified index facades consult it as a third source beside the stub and
 * file-based indexes. That gives completion, resolve and import candidates
 * for generated types without touching IntelliJ's index lifecycle: the
 * server, not the VFS, drives filling and refresh.
 *
 * Queries answer from the cache only, so they are safe under the read lock.
 * An empty catalog schedules a background fill. The fill walks
 * {@code server/contexts}, {@code server/modules} and {@code server/module},
 * and keeps only the types the source indexes cannot find. The server's
 * module cache stays empty until a real compile, so the fill first compiles
 * each context once ({@code --no-output}).
 */
@Service(Service.Level.PROJECT)
@CustomLog
public final class HaxeCompilerTypeCatalogService {

  /** One compiler-known type without source; {@code fqn} is its qualified name in the form the class-name indexes use. */
  public record GeneratedType(@NotNull String contextKey, @NotNull String fqn, @NotNull String name) {
  }

  /**
   * One context's generated types, by qualified and by short name.
   * {@code moduleSigns} holds each module's {@code sign}, which the server
   * changes whenever it retypes the module; the next fill reuses the entries
   * of modules whose sign is unchanged.
   */
  private record ContextCatalog(@NotNull HaxeCompilerDisplayService.DisplayContext context,
                                @NotNull Map<String, GeneratedType> byFqn,
                                @NotNull Map<String, List<GeneratedType>> byName,
                                @NotNull Map<String, String> moduleSigns) {
  }

  private static final long FAILURE_COOLDOWN_MS = 60_000;

  private final Project project;
  private final Map<String, ContextCatalog> catalogs = new ConcurrentHashMap<>();
  private final AtomicBoolean filling = new AtomicBoolean();
  private final Map<String, Long> failedAt = new ConcurrentHashMap<>();
  /** Limits fill attempts while the catalog stays empty for a legitimate reason (no contexts, server off). */
  private volatile long lastFillScheduledAt;

  public HaxeCompilerTypeCatalogService(@NotNull Project project) {
    this.project = project;
  }

  @NotNull
  public static HaxeCompilerTypeCatalogService getInstance(@NotNull Project project) {
    return project.getService(HaxeCompilerTypeCatalogService.class);
  }

  // --- queries (cache-only, safe under the read lock) ---

  @NotNull
  public List<GeneratedType> byName(@NotNull String name) {
    if (!shouldAnswer()) return List.of();
    List<GeneratedType> result = new ArrayList<>();
    for (ContextCatalog catalog : catalogs.values()) {
      result.addAll(catalog.byName().getOrDefault(name, List.of()));
    }
    return result;
  }

  @NotNull
  public List<GeneratedType> byFqn(@NotNull String fqn) {
    if (!shouldAnswer()) return List.of();
    List<GeneratedType> result = new ArrayList<>();
    for (ContextCatalog catalog : catalogs.values()) {
      GeneratedType entry = catalog.byFqn().get(fqn);
      if (entry != null) result.add(entry);
    }
    return result;
  }

  /** The FQN of every generated type: the compiler's share of qualified-name enumeration. */
  @NotNull
  public Set<String> allFqns() {
    if (!shouldAnswer()) return Set.of();
    Set<String> result = new HashSet<>();
    for (ContextCatalog catalog : catalogs.values()) {
      result.addAll(catalog.byFqn().keySet());
    }
    return result;
  }

  @NotNull
  public Set<String> allNames() {
    if (!shouldAnswer()) return Set.of();
    Set<String> result = new HashSet<>();
    for (ContextCatalog catalog : catalogs.values()) {
      result.addAll(catalog.byName().keySet());
    }
    return result;
  }

  /**
   * The class rendered from the entry's blueprint. Cache-only like every
   * query here: a missing blueprint schedules hydration, and this call
   * answers null. Call in a read action.
   */
  @Nullable
  public HaxeClassModel renderedClass(@NotNull GeneratedType entry) {
    ContextCatalog catalog = catalogs.get(entry.contextKey());
    if (catalog == null) return null;
    return HaxeCompilerResolveService.getInstance(project).blueprintClass(catalog.context(), entry.fqn());
  }

  /** Whether the catalog answers at all, which the completion mode decides. An empty catalog also schedules its fill. */
  private boolean shouldAnswer() {
    if (!HaxeCompilerSettings.getInstance(project).getCompletionMode().usesCompiler()) return false;
    if (catalogs.isEmpty()) scheduleFillAll();
    return true;
  }

  public void clearCaches() {
    catalogs.clear();
    failedAt.clear();
    lastFillScheduledAt = 0;
  }

  /**
   * Synchronous fill for the gated live-integration tests (production fills
   * stay background-scheduled and never run in unit-test mode). Performs the
   * warm-up compile and server round-trips on the calling thread.
   */
  @TestOnly
  public void fillNowForTests() {
    clearCaches();
    fillAllContexts();
  }

  /** Synchronous materialization for the gated live-integration tests: hydrates the blueprint, then renders. */
  @TestOnly
  @Nullable
  public HaxeClassModel materializeNowForTests(@NotNull GeneratedType entry) {
    ContextCatalog catalog = catalogs.get(entry.contextKey());
    if (catalog == null) return null;
    HaxeCompilerResolveService.getInstance(project).hydrateNowForTests(catalog.context(), entry.fqn());
    return renderedClass(entry);
  }

  // --- background fill ---

  private void scheduleFillAll() {
    // fixture tests have no compilation server to fill from, and the fill's
    // background index queries only add storage contention to the test run
    if (ApplicationManager.getApplication().isUnitTestMode()) return;
    // resolve running INSIDE an indexer reaches this service through the
    // unified index. A fill scheduled from there feeds a loop: the fill's
    // stub queries force more indexing, whose resolve calls land here again.
    // The next caller outside indexing schedules the fill instead.
    if (FileBasedIndex.getInstance().getFileBeingCurrentlyIndexed() != null) return;
    if (System.currentTimeMillis() - lastFillScheduledAt < FAILURE_COOLDOWN_MS) return;
    if (!filling.compareAndSet(false, true)) return;
    lastFillScheduledAt = System.currentTimeMillis();
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      try {
        fillAllContexts();
      } catch (Throwable t) {
        log.warn("type catalog fill failed: " + t.getMessage());
      } finally {
        filling.set(false);
      }
    });
  }

  private void fillAllContexts() {
    for (Map.Entry<String, HaxeCompilerDisplayService.DisplayContext> entry : moduleContexts().entrySet()) {
      fillContext(entry.getKey(), entry.getValue());
    }
  }

  /** Every module's display context, one per context key. */
  @NotNull
  private Map<String, HaxeCompilerDisplayService.DisplayContext> moduleContexts() {
    return ReadAction.computeBlocking(() -> {
      Map<String, HaxeCompilerDisplayService.DisplayContext> contexts = new LinkedHashMap<>();
      HaxeCompilerDisplayService displayService = HaxeCompilerDisplayService.getInstance(project);
      for (Module module : ModuleManager.getInstance(project).getModules()) {
        HaxeCompilerDisplayService.DisplayContext context = displayService.contextFor(module);
        if (context != null) {
          contexts.putIfAbsent(HaxeCompilerDisplayService.contextKey(context), context);
        }
      }
      return contexts;
    });
  }

  private void fillContext(@NotNull String contextKey, @NotNull HaxeCompilerDisplayService.DisplayContext context) {
    Long failed = failedAt.get(contextKey);
    if (failed != null && System.currentTimeMillis() - failed < FAILURE_COOLDOWN_MS) return;

    HaxeCompilerDisplayService displayService = HaxeCompilerDisplayService.getInstance(project);
    HaxeCompilerDisplayService.Connected connected = displayService.connectFor(context, DisplayMethods.SERVER_MODULES);
    if (connected == null) {
      failedAt.put(contextKey, System.currentTimeMillis());
      return;
    }
    // a failed warm-up compile must not abort the fill. The module cache may
    // already be filled by an earlier compile, and re-running init macros can
    // fail on redefinitions while the cache is fine.
    displayService.ensureContextCompiled(connected, contextKey);

    try {
      ContextCatalog previous = catalogs.get(contextKey);
      ContextCatalog fresh = collectCatalog(contextKey, context, connected, previous);
      if (fresh != null) {
        catalogs.put(contextKey, fresh);
        failedAt.remove(contextKey);
      }
    } catch (DisplayRequestException e) {
      log.info("type catalog fill failed for context " + contextKey + ": " + e.getMessage());
      failedAt.put(contextKey, System.currentTimeMillis());
    }
  }

  /**
   * The context's catalog, reusing the previous entries of unchanged modules.
   * Null while indexing is in progress, because the source indexes would
   * then miss source types and they would be taken for generated ones.
   */
  @Nullable
  private ContextCatalog collectCatalog(@NotNull String contextKey,
                                        @NotNull HaxeCompilerDisplayService.DisplayContext context,
                                        @NotNull HaxeCompilerDisplayService.Connected connected,
                                        @Nullable ContextCatalog previous) throws DisplayRequestException {
    Map<String, GeneratedType> byFqn = new HashMap<>();
    Map<String, String> moduleSigns = new HashMap<>();

    for (String signature : typedContextSignatures(connected)) {
      List<String> listedModules = connected.client().modules(connected.args(), signature);
      Set<String> listed = new HashSet<>(listedModules);
      Set<String> dependencyOnly = new TreeSet<>();

      for (String modulePath : listedModules) {
        ModuleInfo info = moduleInfo(connected, signature, modulePath);
        if (info == null) continue;
        moduleSigns.put(modulePath, info.sign());

        // Context.defineType modules never appear in the listing (and haxe 4's
        // server/module rejects them); they only surface in the dependency
        // lists of the modules USING them (see the display-protocol README)
        for (String dependency : info.dependencies()) {
          if (!listed.contains(dependency)) {
            dependencyOnly.add(dependency);
          }
        }

        boolean unchanged = previous != null && info.sign().equals(previous.moduleSigns().get(modulePath));
        if (unchanged) {
          copyEntries(previous, info.types(), byFqn);
          continue;
        }
        List<String> generated = sourcelessTypes(info.types());
        if (generated == null) return null;
        for (String fqn : generated) {
          byFqn.put(fqn, new GeneratedType(contextKey, fqn, StringUtil.getShortName(fqn)));
        }
      }

      // A module seen only as a dependency is taken to be its single type, as
      // Context.defineType creates it; there is no ModuleInfo listing more.
      // Real source modules in the set (std, haxelibs) drop out in the
      // source-index check.
      // TODO: Context.defineModule can create multi-type modules; whether the
      //  server exposes their type lists anywhere is unverified - only the
      //  module-named type is catalogued.
      List<String> generatedDependencies = sourcelessTypes(new ArrayList<>(dependencyOnly));
      if (generatedDependencies == null) return null;
      for (String fqn : generatedDependencies) {
        byFqn.put(fqn, new GeneratedType(contextKey, fqn, StringUtil.getShortName(fqn)));
      }
    }

    Map<String, List<GeneratedType>> byName = new HashMap<>();
    for (GeneratedType entry : byFqn.values()) {
      byName.computeIfAbsent(entry.name(), k -> new ArrayList<>()).add(entry);
    }
    return new ContextCatalog(context, Map.copyOf(byFqn), Map.copyOf(byName), Map.copyOf(moduleSigns));
  }

  /**
   * The signatures of the server contexts holding TYPED modules. The macro
   * context's modules exist only for the macro interpreter and must not
   * surface in completion.
   */
  @NotNull
  private static List<String> typedContextSignatures(@NotNull HaxeCompilerDisplayService.Connected connected)
    throws DisplayRequestException {
    List<String> signatures = new ArrayList<>();
    for (HaxeServerContext serverContext : connected.client().contexts(connected.args())) {
      if (serverContext.holdsTypedModules()) {
        signatures.add(serverContext.signature());
      }
    }
    return signatures;
  }

  @Nullable
  private static ModuleInfo moduleInfo(@NotNull HaxeCompilerDisplayService.Connected connected,
                                       @NotNull String signature,
                                       @NotNull String modulePath) {
    try {
      return connected.client().module(connected.args(), signature, modulePath);
    } catch (DisplayRequestException e) {
      // a module that vanished between the listing and the detail request
      return null;
    }
  }

  /** Copies the previous fill's entries for the types of an unchanged module. */
  private static void copyEntries(@NotNull ContextCatalog previous,
                                  @NotNull List<String> typeFqns,
                                  @NotNull Map<String, GeneratedType> byFqn) {
    for (String fqn : typeFqns) {
      GeneratedType entry = previous.byFqn().get(fqn);
      if (entry != null) {
        byFqn.put(fqn, entry);
      }
    }
  }

  /**
   * The types no source index knows, i.e. the generated ones. Null while
   * indexing is in progress, since the answer would then be wrong.
   *
   * A non-blocking read on purpose: these index queries can be forced to
   * index PENDING files inline (for example everything a define change just
   * invalidated). A blocking read action holding the lock through that work
   * starves the reparse's write action, and the EDT then freezes behind this
   * background fill. A non-blocking read yields to the write and restarts.
   */
  @Nullable
  private List<String> sourcelessTypes(@NotNull List<String> typeFqns) {
    return ReadAction.nonBlocking(() -> {
      if (DumbService.isDumb(project)) return null;
      GlobalSearchScope scope = GlobalSearchScope.allScope(project);
      List<String> generated = new ArrayList<>();
      for (String fqn : typeFqns) {
        // only the two SOURCE indexes: the unified facade would recurse into
        // this catalog
        boolean inSource = !HaxeFullyQualifiedClassNameStubIndex.getByFqn(fqn, project, scope).isEmpty()
                           || !HaxeFullyQualifiedClassNameIndex.getByFqn(fqn, project, scope).isEmpty();
        if (!inSource) {
          generated.add(fqn);
        }
      }
      return generated;
    }).executeSynchronously();
  }
}
