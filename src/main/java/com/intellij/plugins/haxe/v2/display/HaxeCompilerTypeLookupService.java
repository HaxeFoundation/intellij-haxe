package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.plugins.haxe.display.protocol.JsonTypeRef;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerHoverTypes.Scope;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The experiment that redirects the expression evaluator to the compiler:
 * for a parameter, variable, field or extracted value without a type tag the
 * evaluator takes the compiler's type ({@link HaxeCompilerHoverTypes}),
 * converted into the plugin's type model, instead of evaluating the
 * declaration itself (see {@link #compilerTypeOf}). On only with the registry
 * key {@code haxe.compiler.types.redirect.evaluator}, since it measures
 * whether such a redirect is affordable. Cache-only under the read lock: a
 * declaration without an answer yet evaluates as before, and the highlighting
 * restart after the hover batch brings the compiler's answer in.
 */
@Service(Service.Level.PROJECT)
public final class HaxeCompilerTypeLookupService {

  private static final String REDIRECT_KEY = "haxe.compiler.types.redirect.evaluator";

  /**
   * A converted answer, valid while the wire answer it came from is the same
   * one and no PSI changed since: the plugin-model type holds the PSI of the
   * classes it names, which a roots change or a file reload replaces.
   */
  private record Converted(@NotNull JsonTypeRef wire, long psiStamp, @Nullable ResultHolder type) {
  }

  private final Project project;
  private final Map<PsiElement, Converted> converted = new ConcurrentHashMap<>();

  public HaxeCompilerTypeLookupService(@NotNull Project project) {
    this.project = project;
  }

  public static HaxeCompilerTypeLookupService getInstance(@NotNull Project project) {
    return project.getService(HaxeCompilerTypeLookupService.class);
  }

  /** The evaluator's entry: null when the redirect is off, the element is not a covered declaration, or no answer is cached. */
  @Nullable
  public static ResultHolder compilerTypeOf(@NotNull PsiElement element) {
    if (!Registry.is(REDIRECT_KEY, false)) return null;
    if (!covers(element)) return null;
    Project project = element.getProject();
    if (!HaxeCompilerSettings.getInstance(project).usesCompilerInlays()) return null;
    return getInstance(project).typeOf(element);
  }

  /** Parameters, variables (locals, captures), fields and enum extracted values; methods and types stay with the IDE. */
  private static boolean covers(@NotNull PsiElement element) {
    return element instanceof HaxeParameter
           || element instanceof HaxeLocalVarDeclaration
           || element instanceof HaxeSwitchCaseCapture
           || element instanceof HaxeFieldDeclaration
           || element instanceof HaxeEnumExtractedValueReference;
  }

  /** The compiler's type of the declaration in the plugin's model, or null without a usable answer. Read action; never waits. */
  @Nullable
  public ResultHolder typeOf(@NotNull PsiElement element) {
    JsonTypeRef wire = HaxeCompilerHoverTypes.getInstance(project).wireTypeOf(element, Scope.ALL);
    if (wire == null) return null;
    long psiStamp = PsiModificationTracker.getInstance(project).getModificationCount();
    Converted known = converted.get(element);
    if (known == null || known.wire() != wire || known.psiStamp() != psiStamp) {
      known = new Converted(wire, psiStamp, HaxeCompilerTypeConverter.toResultHolder(wire, element));
      converted.put(element, known);
    }
    return known.type();
  }

  public void clearCaches() {
    converted.clear();
  }
}
