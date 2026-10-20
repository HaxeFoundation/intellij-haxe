package com.intellij.plugins.haxe.v2.compiler;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildSettingsListener;
import com.intellij.plugins.haxe.v2.buildtools.HaxeContainers;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolPathResolver;
import com.intellij.plugins.haxe.v2.buildtools.context.HaxeResolveContext;
import com.intellij.plugins.haxe.v2.buildtools.context.HaxeResolveContexts;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeDefineContextService;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.text.SemVer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The language level editor features assume for a PSI element. An element
 * gets the effective level of its container, usually its module, as set in
 * Settings | Compiler | Haxe Compiler or in the tool window's Language level
 * row. Library and SDK elements use the level of the container of the build
 * they are analysed under (their resolve context), else of the active build.
 * Project files outside every module use the project-root container, which
 * follows the project default unless it has an override. Call in a read
 * action.
 */
public final class HaxeLanguageLevelUtil {

  private HaxeLanguageLevelUtil() {
  }

  /**
   * The level implied by the compiler the container actually compiles with
   * (the container's Environment SDK, else the Build Tools SDK - the same
   * chain tool invocations use). A version between known levels resolves to
   * the closest LOWER level, so a future 4.4 compiler reads as 4.3, not 5.0.
   * Null when no SDK is registered or its version is unparsable; pass a null
   * containerId for the project-wide SDK.
   */
  @Nullable
  public static HaxeLanguageLevel fromCompiler(@NotNull Project project, @Nullable String containerId) {
    Sdk sdk = sdkFor(project, containerId);
    return sdk == null ? null : HaxeLanguageLevel.fromVersionString(sdk.getVersionString());
  }

  /**
   * The level implied by the named SDK's compiler, the configured SDK for a
   * null name, as {@link #fromCompiler} reads it. Null when no SDK is
   * registered or its version is unparsable.
   */
  @Nullable
  public static HaxeLanguageLevel fromSdkName(@NotNull Project project, @Nullable String sdkName) {
    Sdk sdk = HaxeToolPathResolver.findSdk(project, sdkName);
    return sdk == null ? null : HaxeLanguageLevel.fromVersionString(sdk.getVersionString());
  }

  /** The container's SDK, or the project-wide one for a null container. */
  @Nullable
  private static Sdk sdkFor(@NotNull Project project, @Nullable String containerId) {
    return containerId != null ? HaxeToolPathResolver.resolveSdk(project, containerId)
                               : HaxeToolPathResolver.findConfiguredSdk(project);
  }

  /**
   * The version conditional compilation sees as {@code haxe_ver}/{@code haxe}
   * for a container: the effective LANGUAGE LEVEL when the compiler settings
   * say to use it, otherwise the container's actual compiler version. The
   * level is also the fallback when no SDK is registered. A null containerId
   * means the project default.
   */
  @NotNull
  public static String getHaxeVersion(@NotNull Project project, @Nullable String containerId) {
    HaxeCompilerSettings settings = HaxeCompilerSettings.getInstance(project);
    if (!settings.isUseLanguageLevelForConditionals()) {
      Sdk sdk = sdkFor(project, containerId);
      String version = sdk != null ? sdk.getVersionString() : null;
      if (version != null) return version;
    }
    HaxeLanguageLevel level = containerId != null ? settings.getEffectiveLanguageLevel(containerId)
                                                  : settings.getDefaultLanguageLevel();
    return level.getVersionString() + ".0";
  }

  /**
   * The compiler's {@code haxe_ver} value for a dotted version: the version
   * number {@code major*1000 + minor*100 + patch} divided by 1000, so 4.3.7
   * reads 4.307 and 4.0.2 reads 4.002. {@code haxe} keeps the dotted form. A
   * version that does not parse is returned as it is.
   */
  @NotNull
  public static String haxeVerDefineValue(@NotNull String version) {
    SemVer semVer = SemVer.parseFromText(version);
    if (semVer == null) return version;
    int number = semVer.getMajor() * 1000 + semVer.getMinor() * 100 + semVer.getPatch();
    return String.format(Locale.ROOT, "%d.%03d", number / 1000, number % 1000);
  }

  /**
   * The label of the level a container takes without an override of its own:
   * the project's explicit default, else its SDK's level, else the newest
   * level while no SDK is registered.
   */
  @NotNull
  public static String defaultLevelLabel(@Nullable HaxeLanguageLevel explicitDefault, @Nullable HaxeLanguageLevel sdkLevel) {
    if (explicitDefault != null) return HaxeBundle.message("haxe.compiler.project.default.level", explicitDefault.getPresentableText());
    if (sdkLevel != null) return HaxeBundle.message("haxe.compiler.sdk.level", sdkLevel.getPresentableText());
    return HaxeBundle.message("haxe.compiler.no.sdk.level", HaxeLanguageLevel.latest().getPresentableText());
  }

  /** {@link #defaultLevelLabel(HaxeLanguageLevel, HaxeLanguageLevel)} for a container under the project's settings. */
  @NotNull
  public static String defaultLevelLabel(@NotNull Project project, @NotNull String containerId) {
    HaxeLanguageLevel explicitDefault = HaxeCompilerSettings.getInstance(project).getExplicitDefaultLanguageLevel();
    return defaultLevelLabel(explicitDefault, fromCompiler(project, containerId));
  }

  @NotNull
  public static HaxeLanguageLevel getLanguageLevel(@NotNull PsiElement element) {
    HaxeCompilerSettings settings = HaxeCompilerSettings.getInstance(element.getProject());
    String containerId = containerIdOf(element);
    return containerId != null ? settings.getEffectiveLanguageLevel(containerId)
                               : settings.getDefaultLanguageLevel();
  }

  /**
   * The element's container id, which is the key level overrides are stored
   * under: a module name, or the project-root container for project files
   * outside every module. A library or SDK file takes the container of the
   * build it is analysed under (its resolve context), else of the active
   * build - the same build its {@code #if haxe_ver} blocks are evaluated
   * with. Null only for non-physical elements.
   */
  @Nullable
  private static String containerIdOf(@NotNull PsiElement element) {
    PsiFile psiFile = element.getContainingFile();
    if (psiFile == null) return null;
    VirtualFile file = psiFile.getOriginalFile().getVirtualFile();
    if (file == null) return null;
    Project project = element.getProject();
    if (!ProjectFileIndex.getInstance(project).isInContent(file)) {
      HaxeResolveContext context = HaxeResolveContexts.getInstance(project).contextFor(file);
      if (context != null) return HaxeContainers.containerIdFor(project, context.buildFile().file());
      String activeContainer = HaxeDefineContextService.getInstance(project).activeContainerId();
      if (activeContainer != null) return activeContainer;
    }
    return HaxeContainers.containerIdFor(project, file);
  }

  public static boolean isAtLeast(@NotNull PsiElement element, @NotNull HaxeLanguageLevel level) {
    return getLanguageLevel(element).isAtLeast(level);
  }

  /**
   * Applies the level as the element's CONTAINER override (the project
   * default when the element has no container) and refreshes highlighting
   * plus the tool window's Language level row. Backs the "set language
   * level" quickfix.
   */
  public static void setLanguageLevel(@NotNull PsiElement context, @NotNull HaxeLanguageLevel level) {
    Project project = context.getProject();
    HaxeCompilerSettings settings = HaxeCompilerSettings.getInstance(project);
    String containerId = containerIdOf(context);
    if (containerId != null) {
      settings.setContainerLanguageLevelOverride(containerId, level);
    }
    else {
      settings.setDefaultLanguageLevel(level);
    }
    notifyLanguageLevelChanged(project);
  }

  /**
   * Refreshes everything that depends on language levels: the tool window's
   * Language level rows and the define context read the settings again, and
   * highlighting restarts. Every path that changes a level setting calls this.
   */
  public static void notifyLanguageLevelChanged(@NotNull Project project) {
    HaxeBuildSettingsListener.publisher(project).compilerSettingsChanged();
    DaemonCodeAnalyzer.getInstance(project).restart("haxe: language level changed");
  }
}
