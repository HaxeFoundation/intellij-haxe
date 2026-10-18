package com.intellij.plugins.haxe.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.lang.util.HaxeConditionalExpression;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The compiler's target-specific module files: {@code Module.<variant>.hx}
 * declares module {@code Module}, shadowing a plain {@code Module.hx} when the
 * variant is selected. The variant is the compilation's platform name — a stock
 * target's own name ({@code String.js.hx}) or a {@code --custom-target} name —
 * so exactly one variant is active per compilation and an inactive variant's
 * file is not part of the compilation at all.
 * <p>
 * The NAMING here is unconditional: a variant-shaped file always yields the
 * base module name, whether or not its variant is active. Stubs and indexes
 * are application-wide while variant activity is a per-project setting, so
 * activity may only influence QUERY-time candidate selection, never an indexed
 * name.
 */
// TODO: model ".macro.hx" activation (context-dependent within one compilation)
//       and "--custom-extension" suffixes; both currently rank as inactive.
public final class HaxeModuleVariants {

  /** The define carrying the compilation's platform name ({@code target.name=js}); a custom target sets its own name. */
  public static final String TARGET_NAME_DEFINE = "target.name";

  /** Defined (valueless) when the compilation runs a {@code --custom-target}. */
  public static final String CUSTOM_TARGET_DEFINE = "custom_target";

  /** The stock targets' platform names — each target defines its own name, which is also its variant suffix. */
  private static final Set<String> STOCK_PLATFORM_NAMES = Set.of(
    "js", "lua", "neko", "flash", "php", "cpp", "jvm", "java", "cs", "python", "hl", "eval");

  /**
   * The root packages that mirror compiler targets: the std library's target
   * externs ({@code std/flash} is package {@code flash}) and libraries shipping
   * per-target externs use exactly these names, so a class under one of them
   * is usable only while that target is active.
   */
  public static final Set<String> TARGET_ROOT_PACKAGES =
    Set.of("cpp", "cs", "flash", "hl", "java", "js", "jvm", "lua", "php", "python", "neko");

  /** The platform names Haxe 5 refuses as a {@code --custom-target}: its own targets. */
  private static final Set<String> RESERVED_TARGET_NAMES = Set.of(
    "js", "lua", "neko", "flash", "php", "cpp", "jvm", "python", "hl", "eval");

  /** The compiler's length limit for a {@code --custom-target} name. */
  public static final int MAX_CUSTOM_TARGET_LENGTH = 16;

  /** Why a {@code --custom-target} name is refused: by the compiler (reserved, too long) or by the variant lookup (shape). */
  public enum CustomTargetProblem {
    RESERVED, TOO_LONG, NOT_A_TARGET_NAME
  }

  private HaxeModuleVariants() {
  }

  /**
   * The variant suffix of a module file name (given without its {@code .hx}):
   * {@code "String.go"} → {@code "go"}; null for a plain name. The suffix must
   * be a target name (see {@link #isTargetName}), so {@code "Foo.Bar"} is not
   * read as module {@code Foo} with a variant {@code Bar}.
   */
  public static String variantOf(@NotNull String moduleFileName) {
    int dot = moduleFileName.lastIndexOf('.');
    if (dot <= 0 || dot == moduleFileName.length() - 1) return null;
    String variant = moduleFileName.substring(dot + 1);
    return isTargetName(variant) ? variant : null;
  }

  @Nullable
  public static String variantOf(@NotNull PsiFile file) {
    return variantOf(FileUtil.getNameWithoutExtension(file.getName()));
  }

  /** The module name a file declares: {@code "String.go"} → {@code "String"}; a plain name unchanged. */
  @NotNull
  public static String moduleNameOf(@NotNull String moduleFileName) {
    String variant = variantOf(moduleFileName);
    if (variant == null) return moduleFileName;
    return moduleFileName.substring(0, moduleFileName.length() - variant.length() - 1);
  }

  @NotNull
  public static String moduleNameOf(@NotNull PsiFile file) {
    return moduleNameOf(FileUtil.getNameWithoutExtension(file.getName()));
  }

  /**
   * Whether the name can be the target in a variant file name
   * ({@code Module.<target>.hx}): a lowercase ASCII letter or {@code _}, then
   * ASCII letters, digits or {@code _}, like every built-in platform name. A
   * custom target must match it too, or its variant files would never be
   * picked up.
   */
  public static boolean isTargetName(@NotNull String name) {
    if (name.isEmpty()) return false;
    char first = name.charAt(0);
    if (first != '_' && !(first >= 'a' && first <= 'z')) return false;
    for (int i = 1; i < name.length(); i++) {
      if (!isAsciiWordChar(name.charAt(i))) return false;
    }
    return true;
  }

  /** The problem with a {@code --custom-target} name, in the compiler's order of checks; null when it is accepted. */
  @Nullable
  public static CustomTargetProblem customTargetProblem(@NotNull String name) {
    if (RESERVED_TARGET_NAMES.contains(name)) return CustomTargetProblem.RESERVED;
    if (name.length() > MAX_CUSTOM_TARGET_LENGTH) return CustomTargetProblem.TOO_LONG;
    if (!isTargetName(name)) return CustomTargetProblem.NOT_A_TARGET_NAME;
    return null;
  }

  private static boolean isAsciiWordChar(char c) {
    return c == '_' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
  }

  /** Whether the variant is the project's active platform — its file then IS the base module. */
  public static boolean isActive(@NotNull String variant, @NotNull Project project) {
    return activeVariants(project).contains(variant);
  }

  /**
   * The variant suffixes the active build context selects: every stock platform
   * name present as a define (each target defines its own name) plus the
   * {@code target.name} value (which a custom target sets to its name). Reads
   * the same define source as conditional compilation, so variant activity and
   * {@code #if} blocks can never disagree. Define changes reparse the project,
   * so the modification-count dependency drops this together with every
   * resolve result built on it.
   */
  @NotNull
  public static Set<String> activeVariants(@NotNull Project project) {
    // TODO: the active build's defines answer for every requester; a per-file answer needs the requesting file's resolve context
    return CachedValuesManager.getManager(project).getCachedValue(project, () -> {
      Map<String, String> defines = HaxeConditionalExpression.projectDefinitions(project);
      Set<String> active = new HashSet<>();

      for (String platform : STOCK_PLATFORM_NAMES) {
        // the tool window lists only the declared defines; the context adds the target's own
        // (and the compiler version's) - see HaxeDefineContextService.definesBeforeOverrides
        if (defines.containsKey(platform)) active.add(platform);
      }

      String targetName = defines.get(TARGET_NAME_DEFINE); // value from CUSTOM_TARGET_DEFINE
      if (targetName != null && !targetName.isBlank()) active.add(targetName.trim());

      return CachedValueProvider.Result.create(Set.copyOf(active), PsiModificationTracker.MODIFICATION_COUNT);
    });
  }
}
