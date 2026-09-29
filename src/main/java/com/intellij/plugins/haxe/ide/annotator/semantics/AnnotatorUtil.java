package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.parser.HaxePsiDocCommentImpl;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeInactiveBody;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeClassReferenceModel;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.plugins.haxe.v2.display.HaxeGeneratedCodePreview;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

public class AnnotatorUtil {

  /**
   * Common entry guard for semantic annotators: skips elements that are no
   * longer valid, everything while the compiler is the only analysis (see
   * {@link #isStaticAnalysisSuppressed}), elements in generated-code preview
   * files (see {@link #isInGeneratedPreview} for why the preview gets no
   * semantic analysis) and elements in doc-comment code fragments (see
   * {@link #isInDocCodeFragment}). Validity is checked first — an
   * invalidated element cannot be asked for its containing file.
   */
  public static boolean shouldSkip(@NotNull PsiElement element) {
    return !element.isValid()
           || isStaticAnalysisSuppressed(element)
           || isInGeneratedPreview(element)
           || isInAnalysisExemptCode(element);
  }

  /** The "compiler diagnostics only" setting: the plugin's own analysis stands down project-wide. */
  public static boolean isStaticAnalysisSuppressed(@NotNull PsiElement element) {
    return HaxeCompilerSettings.getInstance(element.getProject()).isStaticAnalysisSuppressed();
  }

  /**
   * Common entry guard for the color annotators. Validity is checked first —
   * an invalidated element cannot be asked about its tree. Inactive branches
   * are skipped because they get DIMMED colors from
   * HaxeInactiveCodeDimAnnotator; full-strength colors would paint over the
   * dimming.
   */
  public static boolean shouldSkipColorAnnotation(@NotNull PsiElement element) {
    return !element.isValid() || element instanceof PsiWhiteSpace || isInInactiveBranch(element);
  }

  /**
   * Code that exists for DISPLAY, not compilation: doc-comment code fences
   * and inactive conditional-compilation branches. Semantic analysis there
   * is noise by definition - the code does not participate in the build.
   */
  public static boolean isInAnalysisExemptCode(@NotNull PsiElement element) {
    return isInDocCodeFragment(element) || isInInactiveBranch(element);
  }

  /** Inside a lazily parsed inactive conditional branch (strict - the branch element itself is not "inside"). */
  public static boolean isInInactiveBranch(@NotNull PsiElement element) {
    return PsiTreeUtil.getParentOfType(element, HaxeInactiveBody.class) != null;
  }

  /**
   * A doc comment's markdown code fences are language-injected for
   * KDoc-style highlighting only: sample snippets resolve nothing, so
   * semantic errors there would be pure noise.
   */
  public static boolean isInDocCodeFragment(@NotNull PsiElement element) {
    PsiFile file = element.getContainingFile();
    if (file == null) return false;
    InjectedLanguageManager manager = InjectedLanguageManager.getInstance(file.getProject());
    if (!manager.isInjectedFragment(file)) return false;
    return manager.getInjectionHost(file) instanceof HaxePsiDocCommentImpl;
  }

  /**
   * Semantic annotators (errors/warnings) skip generated-code preview files:
   * the preview is a post-macro reconstruction where unresolvable names are
   * expected, so semantic analysis both wastes work and paints noise. The
   * color annotators do NOT consult this — the preview keeps highlighting.
   */
  public static boolean isInGeneratedPreview(@NotNull PsiElement element) {
    PsiFile file = element.getContainingFile();
    VirtualFile virtualFile = file != null ? file.getVirtualFile() : null;
    return virtualFile != null && virtualFile.getUserData(HaxeGeneratedCodePreview.PREVIEW_KEY) != null;
  }

  public static boolean hasMacroForCodeGeneration(@NotNull HaxeClassModel clazz) {
    if (clazz.hasCompileTimeMeta(HaxeMeta.BUILD)) return true;

    List<HaxeClassModel> classModels = clazz.getExtendingTypes().stream()
      .map(HaxeClassReferenceModel::getHaxeClassModel)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());


    for (int i = 0; i < classModels.size(); i++) {
      HaxeClassModel model = classModels.get(i);
      HaxeClass aClass = model.haxeClass;
      if (aClass != null) {
        if (aClass.hasCompileTimeMeta(HaxeMeta.AUTO_BUILD)) {
          return true;
        }
        List<HaxeClassModel> list =
          model.getExtendingTypes().stream()
            .map(HaxeClassReferenceModel::getHaxeClassModel)
            .filter(not(classModels::contains))
            .toList();

        classModels.addAll(list);
      }
    }

    return false;
  }


}
