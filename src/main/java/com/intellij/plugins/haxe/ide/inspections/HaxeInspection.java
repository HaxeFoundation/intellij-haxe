package com.intellij.plugins.haxe.ide.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.LocalInspectionEP;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.plugins.haxe.ide.annotator.semantics.AnnotatorUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

/**
 * Base for the plugin's inspections: registration and metadata (display
 * name, group, default level, enabled state, description) live in the
 * {@code <localInspection>} entry in plugin.xml and the standard
 * {@code inspectionDescriptions/<shortName>.html} file — the inspection
 * framework owns toggling and severity. This base carries only the shared
 * visitor plumbing.
 */
public abstract class HaxeInspection extends LocalInspectionTool {

  private volatile HighlightDisplayLevel registeredLevel;

  /**
   * The plugin.xml {@code level} attribute, resolved through the tool's own
   * registration so it is declared in ONE place. The platform reads the
   * attribute only via the registration bean and never hands it to the tool
   * instance, while {@link #checkVisitor} needs the tool-side value to
   * decide which reported severity follows the profile's choice. A tool
   * instantiated without a registration keeps the platform default.
   */
  @Override
  public @NotNull HighlightDisplayLevel getDefaultLevel() {
    HighlightDisplayLevel level = registeredLevel;
    if (level == null) {
      String className = getClass().getName();
      level = LocalInspectionEP.LOCAL_INSPECTION.getExtensionList().stream()
        .filter(ep -> className.equals(ep.implementationClass))
        .findFirst()
        .map(LocalInspectionEP::getDefaultLevel)
        .orElseGet(super::getDefaultLevel);
      registeredLevel = level;
    }
    return level;
  }

  /**
   * The visitor shape most checks share: one element type, one check method
   * reporting through {@link HaxeProblemReporter}. A problem at the severity
   * of {@link #getDefaultLevel()} — the registered default — follows the
   * profile's severity choice; other severities render verbatim.
   */
  protected final <T extends PsiElement> PsiElementVisitor checkVisitor(@NotNull ProblemsHolder holder,
                                                                        @NotNull Class<T> elementType,
                                                                        @NotNull BiConsumer<T, HaxeProblemReporter> check) {
    HaxeProblemReporter reporter = HaxeProblemReporter.of(holder, getDefaultLevel().getSeverity());
    return new PsiElementVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        if (AnnotatorUtil.shouldSkip(element)) return;
        if (elementType.isInstance(element)) {
          check.accept(elementType.cast(element), reporter);
        }
      }
    };
  }
}
