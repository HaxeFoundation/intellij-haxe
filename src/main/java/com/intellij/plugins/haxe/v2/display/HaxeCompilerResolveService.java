package com.intellij.plugins.haxe.v2.display;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.display.protocol.DisplayMethods;
import com.intellij.plugins.haxe.display.protocol.JsonTypeRef;
import com.intellij.plugins.haxe.display.protocol.server.HaxeServerContext;
import com.intellij.plugins.haxe.display.protocol.server.TypeBlueprint;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeModule;
import com.intellij.plugins.haxe.lang.psi.HaxeReference;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeResolveResult;
import com.intellij.plugins.haxe.model.HaxeBaseMemberModel;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeModuleModel;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

/**
 * The compiler-backed last resort of {@code HaxeResolver}. When static
 * resolution fails, the member is looked up in the blueprint of its class.
 * A blueprint is the compiler's description of one type after all macros
 * ran ({@code server/type}): every member with its type, macro-generated
 * members included. The class is the enclosing one for an unqualified
 * reference and the receiver's class for a qualified one.
 *
 * Resolve runs under the read lock, so this path only reads the cache. A
 * miss schedules background hydration and fails this once; the highlighting
 * restart after hydration resolves again from the cache. A resolved member
 * is a declaration in a synthetic extern class rendered from the blueprint.
 * That is real PSI, so type inference, completion and chained member access
 * all work on it: a generated {@code panel:ui.Panel} field makes
 * {@code panel.title} resolve statically against the real {@code Panel}.
 *
 * Active only when the completion mode uses the compiler (Settings |
 * Compiler | Haxe Compiler); in "IDE only" this service answers nothing. The
 * compiler-diagnostics toggle governs only problem highlighting.
 */
@Service(Service.Level.PROJECT)
@CustomLog
public final class HaxeCompilerResolveService {

  private record BlueprintKey(@NotNull String contextKey, @NotNull String dotPath) {
  }

  /**
   * Marks blueprint-rendered files with their type's dot path. The
   * generated-code preview's goto handler recognizes a blueprint-resolved
   * member by this key on the member's containing file.
   */
  public static final Key<String> BLUEPRINT_DOT_PATH = Key.create("haxe.blueprint.dotpath");

  private static final long FAILURE_COOLDOWN_MS = 30_000;
  /**
   * The types declared in StdTypes.hx. They have no module of their own, so
   * {@code server/type} answers "No such module" for them. An unresolved
   * receiver evaluates to Dynamic, so without this list every unresolved
   * member access would request a blueprint of Dynamic.
   */
  private static final Set<String> STD_TYPES_DECLARATIONS = Set.of(
    "Void", "Bool", "Int", "Float", "Single", "Dynamic", "Null",
    "Iterator", "Iterable", "KeyValueIterator", "KeyValueIterable", "ArrayAccess");
  /**
   * Keeps the fallback one level deep. Resolving the RECEIVER inside the
   * fallback runs the resolver again, and its failures would enter this
   * fallback again. The resolver cannot cache failures (a failure cached on
   * one path would mask a success reachable through another), so failed
   * sub-chains are recomputed on every visit, and nested fallbacks multiply
   * that cost without bound. A reference therefore gets the fallback only as
   * the subject of resolution, never inside another reference's fallback.
   */
  private static final ThreadLocal<Boolean> inFallback = ThreadLocal.withInitial(() -> false);

  private final Project project;
  private final Map<BlueprintKey, TypeBlueprint> blueprints = new ConcurrentHashMap<>();
  private final Set<BlueprintKey> hydrating = ConcurrentHashMap.newKeySet();
  private final Map<BlueprintKey, Long> failedAt = new ConcurrentHashMap<>();
  private final Map<BlueprintKey, HaxeFile> blueprintFiles = new ConcurrentHashMap<>();

  public HaxeCompilerResolveService(@NotNull Project project) {
    this.project = project;
  }

  @NotNull
  public static HaxeCompilerResolveService getInstance(@NotNull Project project) {
    return project.getService(HaxeCompilerResolveService.class);
  }

  /**
   * The cache-only fallback. Null when this reference is out of scope, the
   * blueprint is not hydrated yet, or the compiler does not know the member
   * either. Never touches the network; safe under the read lock.
   *
   * Unqualified identifiers look up the enclosing class; qualified ones look
   * up the RECEIVER's statically resolved class ({@code this.x},
   * {@code ClassName.x}, {@code view.x}). The receiver itself usually
   * resolves statically even when the member is generated.
   */
  @Nullable
  public List<? extends PsiElement> tryResolve(@NotNull HaxeReference reference) {
    if (!(reference instanceof HaxeReferenceExpression expression)) return null;
    if (!HaxeCompilerSettings.getInstance(project).getCompletionMode().usesCompiler()) return null;
    if (DumbService.isDumb(project)) return null;
    String name = expression.getReferenceName();
    if (name == null || name.isEmpty()) return null;

    // check the context BEFORE resolving the receiver: targetClassOf recurses
    // into resolve, which is wasted work when there is no build context
    // (fixture tests, non-v2 projects) and this path cannot answer anyway
    VirtualFile contextFile = HaxeCompilerDisplayService.physicalFileOf(expression);
    if (contextFile == null) return null;
    if (HaxeCompilerDisplayService.getInstance(project).contextFor(contextFile) == null) return null;

    if (inFallback.get()) return null;
    inFallback.set(true);
    try {
      HaxeClass targetClass = targetClassOf(expression);
      if (targetClass == null) return null;

      // a member with a source declaration needs no blueprint
      HaxeClassModel targetModel = targetClass.getModel();
      HaxeBaseMemberModel memberModel = targetModel.getMember(name, null);
      if (memberModel != null) return null;

      BlueprintLookup lookup = blueprintLookup(contextFile, targetClass.getQualifiedName());
      if (lookup == null || lookup.blueprint().findMember(name) == null) return null;
      PsiElement member = blueprintMember(lookup.key(), lookup.blueprint(), name);
      return member != null ? List.of(member) : null;
    } finally {
      inFallback.set(false);
    }
  }

  /** The class whose blueprint can hold the referenced member. */
  @Nullable
  private static HaxeClass targetClassOf(@NotNull HaxeReferenceExpression expression) {
    if (expression.getFirstChild() instanceof HaxeReference receiver) {
      HaxeResolveResult receiverResult = receiver.resolveHaxeClass();
      return receiverResult != null ? receiverResult.getHaxeClass() : null;
    }
    return PsiTreeUtil.getParentOfType(expression, HaxeClass.class);
  }

  /**
   * The hydrated blueprint of a type, resolved through the EDITED file's
   * build context (so library receivers hydrate too). Cache-only; a miss
   * schedules hydration and returns null. Call in a read action.
   */
  @Nullable
  public TypeBlueprint blueprintForType(@NotNull VirtualFile contextFile, @Nullable String dotPath) {
    if (!HaxeCompilerSettings.getInstance(project).getCompletionMode().usesCompiler()) return null;
    if (DumbService.isDumb(project)) return null;
    BlueprintLookup lookup = blueprintLookup(contextFile, dotPath);
    return lookup != null ? lookup.blueprint() : null;
  }

  /**
   * The blueprint-rendered class of a compiler-known type, addressed through
   * an explicit display context — for the type catalog, whose entries carry
   * no source file. Cache-only; a miss schedules hydration and returns null.
   * Call in a read action.
   */
  @Nullable
  public HaxeClassModel blueprintClass(@NotNull HaxeCompilerDisplayService.DisplayContext context, @NotNull String dotPath) {
    if (!HaxeCompilerSettings.getInstance(project).getCompletionMode().usesCompiler()) return null;
    BlueprintKey key = new BlueprintKey(HaxeCompilerDisplayService.contextKey(context), dotPath);
    TypeBlueprint blueprint = blueprints.get(key);
    if (blueprint == null) {
      scheduleHydration(key, context);
      return null;
    }
    return renderedClassModel(key, blueprint);
  }

  private record BlueprintLookup(@NotNull BlueprintKey key, @NotNull TypeBlueprint blueprint) {
  }

  @Nullable
  private BlueprintLookup blueprintLookup(@NotNull VirtualFile contextFile, @Nullable String dotPath) {
    if (dotPath == null || dotPath.isEmpty() || STD_TYPES_DECLARATIONS.contains(dotPath)) return null;
    HaxeCompilerDisplayService.DisplayContext context = HaxeCompilerDisplayService.getInstance(project).contextFor(contextFile);
    if (context == null) return null;

    BlueprintKey key = new BlueprintKey(HaxeCompilerDisplayService.contextKey(context), dotPath);
    TypeBlueprint blueprint = blueprints.get(key);
    if (blueprint == null) {
      // hydration compiles the context, which is pointless while the edited
      // file does not parse; cached blueprints are served regardless
      if (HaxeCompilerDisplayService.isSyntaxClean(project, contextFile)) {
        scheduleHydration(key, context);
      }
      return null;
    }
    return new BlueprintLookup(key, blueprint);
  }

  // TODO: no automatic blueprint invalidation - an edited macro input (a
  //  layout file a macro reads, a lib's include.xml) only takes effect after
  //  Purge Caches or a restart.
  public void clearCaches() {
    blueprints.clear();
    blueprintFiles.clear();
    failedAt.clear();
  }

  // --- synthetic PSI ---

  /**
   * The member's declaration in the extern class rendered from the
   * blueprint. It is non-physical PSI with real type tags, built the way
   * {@code HaxeSyntheticDeclarations} builds {@code trace}, so navigation
   * lands on a readable declaration.
   */
  @Nullable
  private PsiElement blueprintMember(@NotNull BlueprintKey key, @NotNull TypeBlueprint blueprint, @NotNull String name) {
    HaxeClassModel renderedClass = renderedClassModel(key, blueprint);
    if (renderedClass == null) return null;
    HaxeBaseMemberModel member = renderedClass.getMember(name, null);
    return member != null ? member.getBasePsi() : null;
  }

  @Nullable
  private HaxeClassModel renderedClassModel(@NotNull BlueprintKey key, @NotNull TypeBlueprint blueprint) {
    HaxeFile file = blueprintFiles.computeIfAbsent(key, k -> buildBlueprintFile(k, blueprint));
    HaxeModule module = file.getModule();
    if (module == null) return null;
    HaxeModuleModel model = (HaxeModuleModel)module.getModel();
    return model.getClass(StringUtil.getShortName(key.dotPath()));
  }

  @NotNull
  private HaxeFile buildBlueprintFile(@NotNull BlueprintKey key, @NotNull TypeBlueprint blueprint) {
    String typeName = StringUtil.getShortName(key.dotPath());
    String header = """
      /**
         This class does not exist as source. It is the compiler's
         post-macro blueprint of `%s` -
         members generated by macros resolve here.
      **/
      extern class %s {
      """.formatted(key.dotPath(), typeName);
    StringBuilder text = new StringBuilder(header);
    for (TypeBlueprint.Member member : blueprint.fields()) {
      appendMember(text, member, false);
    }
    for (TypeBlueprint.Member member : blueprint.statics()) {
      appendMember(text, member, true);
    }
    text.append("}\n");
    HaxeFile file = HaxeElementGenerator.createFile(project, typeName, text.toString());
    file.putUserData(BLUEPRINT_DOT_PATH, key.dotPath());
    return file;
  }

  private static void appendMember(@NotNull StringBuilder text, @NotNull TypeBlueprint.Member member, boolean isStatic) {
    text.append("\tpublic ");
    if (isStatic) text.append("static ");
    JsonTypeRef type = member.type();
    if (member.isMethod() && type != null && type.isFunction()) {
      text.append("function ").append(member.name())
        .append('(').append(HaxeTypeSyntax.parameterListText(type)).append("):")
        .append(HaxeTypeSyntax.returnTypeText(type));
    } else {
      text.append("var ").append(member.name()).append(':').append(HaxeTypeSyntax.safeTypeText(type));
    }
    text.append(";\n");
  }

  // --- hydration ---

  /**
   * Synchronous hydration for the gated live-integration tests (production
   * hydration stays background-scheduled and never runs in unit-test mode).
   */
  @TestOnly
  public void hydrateNowForTests(@NotNull HaxeCompilerDisplayService.DisplayContext context, @NotNull String dotPath) {
    BlueprintKey key = new BlueprintKey(HaxeCompilerDisplayService.contextKey(context), dotPath);
    TypeBlueprint blueprint = hydrate(key, context);
    if (blueprint != null) {
      blueprints.put(key, blueprint);
    }
  }

  private void scheduleHydration(@NotNull BlueprintKey key, @NotNull HaxeCompilerDisplayService.DisplayContext context) {
    // fixture tests have no server to hydrate from; never spawn the attempt
    if (ApplicationManager.getApplication().isUnitTestMode()) return;
    Long failed = failedAt.get(key);
    if (failed != null && System.currentTimeMillis() - failed < FAILURE_COOLDOWN_MS) return;
    if (!hydrating.add(key)) return;

    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      try {
        TypeBlueprint blueprint = hydrate(key, context);
        if (blueprint != null) {
          blueprints.put(key, blueprint);
          failedAt.remove(key);
          restartHighlighting();
        } else {
          failedAt.put(key, System.currentTimeMillis());
        }
      } catch (Throwable t) {
        log.warn("blueprint hydration failed for " + key.dotPath() + ": " + t.getMessage());
        failedAt.put(key, System.currentTimeMillis());
      } finally {
        hydrating.remove(key);
      }
    });
  }

  @Nullable
  private TypeBlueprint hydrate(@NotNull BlueprintKey key, @NotNull HaxeCompilerDisplayService.DisplayContext context) {
    HaxeCompilerDisplayService displayService = HaxeCompilerDisplayService.getInstance(project);
    HaxeCompilerDisplayService.Connected connected = displayService.connectFor(context, DisplayMethods.SERVER_TYPE);
    if (connected == null) return null;

    // a failed warm-up compile does not stop the lookup: the module may
    // already be in the server's cache from an earlier compile
    displayService.ensureContextCompiled(connected, key.contextKey());

    try {
      String dotPath = key.dotPath();
      return blueprintFromAnyContext(connected, modulePathOf(dotPath), StringUtil.getShortName(dotPath));
    } catch (DisplayRequestException e) {
      log.info("server/type failed for " + key.dotPath() + ": " + e.getMessage());
      return null;
    }
  }

  /**
   * The module declaring a type. A main type's dot path is its module path;
   * a sub-type's ({@code pack.Module.SubType}) has the module as its prefix.
   * Haxe requires package names to start lowercase and module names
   * uppercase, so an uppercase second-to-last segment names the module.
   */
  @NotNull
  static String modulePathOf(@NotNull String dotPath) {
    String owner = StringUtil.getPackageName(dotPath);
    if (owner.isEmpty()) return dotPath;
    String ownerName = StringUtil.getShortName(owner);
    return Character.isUpperCase(ownerName.charAt(0)) ? owner : dotPath;
  }

  /**
   * The type's blueprint, requested from the server context that holds its
   * module. A source module's context is found through the module listing. A
   * module created by {@code Context.defineType} is listed nowhere (see the
   * display-protocol README), so every typed context is tried in turn, and
   * server/type itself tells whether the type lives there.
   */
  @Nullable
  private TypeBlueprint blueprintFromAnyContext(@NotNull HaxeCompilerDisplayService.Connected connected,
                                                @NotNull String modulePath,
                                                @NotNull String typeName) throws DisplayRequestException {
    List<String> candidates = new ArrayList<>();
    for (HaxeServerContext context : connected.client().contexts(connected.args())) {
      try {
        if (connected.client().modules(connected.args(), context.signature()).contains(modulePath)) {
          // a context that lists the module certainly holds it; try it first
          candidates.add(0, context.signature());
          continue;
        }
      } catch (DisplayRequestException ignored) {
        // some contexts reject module listing - try the next
      }
      if (context.holdsTypedModules()) {
        candidates.add(context.signature());
      }
    }
    for (String signature : candidates) {
      try {
        return connected.client().typeBlueprint(connected.args(), signature, modulePath, typeName);
      } catch (DisplayRequestException ignored) {
        // the type does not live in this context - try the next
      }
    }
    return null;
  }

  /**
   * A new blueprint changes what DEPENDENT references resolve to.
   * {@code panel.title} was resolved, and cached as unresolved, while
   * {@code panel} was still unknown. The fallback covers only the root
   * reference, so such stale results must be dropped before highlighting
   * restarts. Dropping the resolve caches is not enough, because
   * type-evaluation CachedValues depend on the PSI modification count and
   * survive it. Hence the full PSI cache drop, which happens about once per
   * hydrated class per session.
   */
  private void restartHighlighting() {
    ApplicationManager.getApplication().invokeLater(() -> {
      if (!project.isDisposed()) {
        PsiManager.getInstance(project).dropPsiCaches();
        DaemonCodeAnalyzer.getInstance(project).restart("haxe: compiler resolve results hydrated");
      }
    }, ModalityState.nonModal());
  }
}
