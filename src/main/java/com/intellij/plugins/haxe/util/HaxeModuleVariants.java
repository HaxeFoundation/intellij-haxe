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

  private HaxeModuleVariants() {
  }

  /**
   * The variant suffix of a module file name (given without its {@code .hx}):
   * {@code "String.go"} → {@code "go"}; null for a plain name. The suffix must
   * be a lowercase-start identifier — platform and custom-target names are
   * lowercase, and accepting an uppercase segment would swallow a dotted
   * plain file name.
   */
  public static String variantOf(@NotNull String moduleFileName) {
    int dot = moduleFileName.lastIndexOf('.');
    if (dot <= 0 || dot == moduleFileName.length() - 1) return null;
    String variant = moduleFileName.substring(dot + 1);
    return isVariantSegment(variant) ? variant : null;
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

  private static boolean isVariantSegment(@NotNull String segment) {
    char first = segment.charAt(0);
    if (first != '_' && !(first >= 'a' && first <= 'z')) return false;
    for (int i = 1; i < segment.length(); i++) {
      char c = segment.charAt(i);
      if (c != '_' && !Character.isLetterOrDigit(c)) return false;
    }
    return true;
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
    return CachedValuesManager.getManager(project).getCachedValue(project, () -> {
      Map<String, String> defines = HaxeConditionalExpression.projectDefinitions(project);
      Set<String> active = new HashSet<>();

      for (String platform : STOCK_PLATFORM_NAMES) {
        // the tool window lists only the declared defines; the context adds the target's own
        // (and the compiler version's) - see HaxeDefineContextService.baseDefines
        if (defines.containsKey(platform)) active.add(platform);
      }

      String targetName = defines.get(TARGET_NAME_DEFINE); // value from CUSTOM_TARGET_DEFINE
      if (targetName != null && !targetName.isBlank()) active.add(targetName.trim());

      return CachedValueProvider.Result.create(Set.copyOf(active), PsiModificationTracker.MODIFICATION_COUNT);
    });
  }
}
