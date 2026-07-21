package com.intellij.plugins.haxe.runner.debugger.hashlink;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeReference;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.HaxeClassNameUnifiedIndex;
import com.intellij.plugins.haxe.model.HaxeFileModel;
import com.intellij.plugins.haxe.model.HaxeImportableModel;
import com.intellij.plugins.haxe.runner.debugger.HaxeDebuggerSupportUtils;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.xdebugger.XSourcePosition;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Rewrites bare class references in an evaluate expression to their
 * fully-qualified names, using the breakpoint file's imports/scope, BEFORE the
 * expression is handed to the adapter.
 *
 * <p>The adapter resolves class-qualified statics only by fully-qualified name
 * ({@code pkg.Cls.member}) — it has no view of the source file's imports. So
 * {@code Point.ORIGIN} typed against a file that {@code import}s the class must
 * become {@code geom.Point.ORIGIN} first. We do it <em>eagerly</em> (rewrite,
 * then send once) rather than letting the adapter reject the unresolved name and
 * retrying with a qualified form: a lazy retry would re-run any side effects
 * already evaluated in a compound expression — {@code increase() + Cls.member}
 * would call {@code increase()} twice. Sending once keeps side effects once.
 *
 * <p>A detached code fragment's own {@code resolve()} does NOT honor the context
 * file's imports, so we resolve each type <em>name</em> against the context via
 * the model layer (the Haxe analogue of Flex's {@code
 * JSImportHandlingUtil.resolveTypeName}) — the same {@code exposeByName} path
 * {@link com.intellij.plugins.haxe.lang.psi.HaxeResolver} itself uses.
 *
 * <p>Limitation: a local variable named exactly like a resolvable class (one the
 * context imports, shares a package with, or the project's single class of that
 * name) would be wrongly qualified. Class names are capitalized by convention and
 * locals are not, so a collision is pathological; the adapter still resolves
 * genuine locals first, so only an identical-name clash is affected.
 */
final class HashLinkExpressionQualifier {
  private static final Logger LOG = Logger.getInstance(HashLinkExpressionQualifier.class);

  private HashLinkExpressionQualifier() {
  }

  /**
   * Returns {@code expression} with each bare class reference replaced by its
   * fully-qualified name; returns it unchanged when there is no usable context or
   * nothing needs qualifying. Never throws — a rewrite failure falls back to the
   * original text (the adapter then reports a clear UNRESOLVED_NAME as before).
   */
  static @NotNull String qualify(@NotNull Project project, @Nullable XSourcePosition position,
                                 @NotNull String expression) {
    if (position == null || position.getFile() == null) {
      return expression;
    }
    try {
      return ReadAction.nonBlocking(() -> {
        PsiElement context = HaxeDebuggerSupportUtils.getContextElement(
          position.getFile(), position.getOffset(), project);
        return context == null ? expression : rewrite(project, context, expression);
      }).executeSynchronously();
    }
    catch (RuntimeException e) {
      LOG.warn("Could not qualify evaluate expression '" + expression + "'", e);
      return expression;
    }
  }

  /**
   * Qualifies bare class references in {@code expression} against {@code context}
   * (a PSI element at the breakpoint). Package-visible for direct testing without
   * a live debug session.
   */
  static @NotNull String rewrite(@NotNull Project project, @NotNull PsiElement context,
                                 @NotNull String expression) {
    PsiFile fragment = HaxeElementGenerator.createExpressionCodeFragment(project, expression, context, false);

    // Each leftmost identifier of a reference chain (`Cls` in `Cls.member`) is a
    // candidate class name; when it resolves against the context's imports to a
    // class, record a text replacement -> its fully-qualified name.
    List<Replacement> replacements = new ArrayList<>();
    for (HaxeReferenceExpression ref : PsiTreeUtil.findChildrenOfType(fragment, HaxeReferenceExpression.class)) {
      if (ref.getQualifier() != null) {
        continue; // not the leftmost segment of its chain
      }
      if (!(ref.getParent() instanceof HaxeReference)) {
        continue; // a standalone identifier, not a member-access qualifier
      }
      String simpleName = ref.getText();
      String fqn = resolveTypeName(simpleName, context);
      if (fqn != null && !fqn.equals(simpleName)) {
        replacements.add(new Replacement(ref.getTextRange().getStartOffset(),
                                         ref.getTextRange().getEndOffset(), fqn));
      }
    }
    if (replacements.isEmpty()) {
      return expression;
    }
    // apply back-to-front so each replacement's offsets stay valid
    replacements.sort(Comparator.comparingInt(Replacement::start).reversed());
    StringBuilder rewritten = new StringBuilder(expression);
    for (Replacement r : replacements) {
      rewritten.replace(r.start(), r.end(), r.fqn());
    }
    return rewritten.toString();
  }

  /**
   * Resolves a simple class name against the context file's imports/usings and
   * its own package, returning the class's fully-qualified name or {@code null}.
   * The same {@code exposeByName} resolution {@code HaxeResolver} uses — the Haxe
   * analogue of Flex's {@code resolveTypeName}.
   */
  static @Nullable String resolveTypeName(@NotNull String simpleName, @NotNull PsiElement context) {
    HaxeFileModel fileModel = HaxeFileModel.fromElement(context);
    if (fileModel != null) {
      for (HaxeImportableModel importModel : fileModel.getOrderedImportAndUsingModels()) {
        if (importModel.exposeByName(simpleName) instanceof HaxeClass exposed) {
          return exposed.getQualifiedName();
        }
      }
      // a class in the same package needs no import
      String pkg = fileModel.getPackageName();
      String candidate = pkg == null || pkg.isEmpty() ? simpleName : pkg + "." + simpleName;
      HaxeClass samePackage = HaxeResolveUtil.findClassByQName(candidate, context);
      if (samePackage != null) {
        return samePackage.getQualifiedName();
      }
    }
    // Fallback: a class the breakpoint file neither imports nor shares a package
    // with is still unambiguous when the project holds exactly ONE class of that
    // short name — qualify to it so the expression evaluates without the user
    // adding an import. Ambiguous names (two+ classes) are left for the adapter.
    return uniqueProjectClassFqn(simpleName, context);
  }

  private static @Nullable String uniqueProjectClassFqn(@NotNull String simpleName, @NotNull PsiElement context) {
    GlobalSearchScope scope = HaxeResolveUtil.getScopeForElement(context);
    String fqn = null;
    for (HaxeClass candidate : HaxeClassNameUnifiedIndex.getByNameFiltered(simpleName, context.getProject(), scope)) {
      String candidateFqn = candidate.getQualifiedName();
      if (candidateFqn == null) {
        continue;
      }
      if (fqn == null) {
        fqn = candidateFqn;
      }
      else if (!fqn.equals(candidateFqn)) {
        return null; // ambiguous — don't guess which class the user meant
      }
    }
    return fqn;
  }

  private record Replacement(int start, int end, String fqn) {
  }
}
