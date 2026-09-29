package com.intellij.plugins.haxe.ide.inspections.resolve;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.actions.HaxeStaticMemberAddImportIntentionAction;
import com.intellij.plugins.haxe.ide.actions.HaxeTypeAddImportIntentionAction;
import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.plugins.haxe.ide.annotator.semantics.AnnotatorUtil;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.*;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataCompileTimeMeta;
import com.intellij.plugins.haxe.metadata.psi.impl.HaxeMetadataTypeName;
import com.intellij.plugins.haxe.model.HaxeBaseMemberModel;
import com.intellij.plugins.haxe.model.HaxeMemberModel;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import com.intellij.plugins.haxe.ide.inspections.HaxeInspection;

/**
 * Type or reference names that do not resolve but DO match a known class or
 * public static member elsewhere in the project — the add-import cases.
 * As an inspection its findings carry a profile toggle/severity and land
 * under the Haxe group in batch runs;
 * expression-level unresolved references are {@link HaxeUnresolvedSymbolInspection}.
 */
public class HaxeUnresolvedTypeInspection extends HaxeInspection {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    HaxeProblemReporter reporter = HaxeProblemReporter.of(holder, getDefaultLevel().getSeverity());
    List<TextRange> reportedRanges = new ArrayList<>();
    return new PsiElementVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        if (AnnotatorUtil.shouldSkip(element)) return;

        if (element instanceof HaxeType type) {
          HaxeReferenceExpression expression = type.getReferenceExpression();
          if (expression.resolve() == null) {
            report(expression, reporter, reportedRanges);
          }
        }
        else if (element instanceof HaxeReferenceExpression expression) {
          if (expression.resolve() == null) {
            report(expression, reporter, reportedRanges);
          }
        }
      }
    };
  }

  private static void report(@NotNull HaxeReferenceExpression expression, @NotNull HaxeProblemReporter reporter,
                             @NotNull List<TextRange> reportedRanges) {
    final GlobalSearchScope scope = HaxeResolveUtil.getScopeForElement(expression);
    List<HaxeMemberModel> members = new ArrayList<>();
    List<HaxeComponent> classes = new ArrayList<>(HaxeClassNameUnifiedIndex.getByNameFiltered(expression.getText(), expression.getProject(), scope));
    if (expression.getParent() instanceof HaxeCallExpression) {
      members.addAll(findStaticMembers(expression.getText(), expression.getProject(), scope, true, false));
    } else {
      members.addAll(findStaticMembers(expression.getText(), expression.getProject(), scope, false, true));
    }

    boolean classesFound = !classes.isEmpty();
    boolean membersFound = !members.isEmpty();

    if (classesFound || membersFound) {
      // operator overload metas have no "real" references - the check is skipped
      if (isCompileTimeMeta(expression, HaxeMeta.OP)) return;
      // a HaxeType's reference expression is visited both as the type's child
      // and as a reference in its own right - report the span once
      TextRange textRange = expression.getTextRange();
      if (reportedRanges.contains(textRange)) return;
      reportedRanges.add(textRange);

      HaxeProblemReporter.Problem problem =
        reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.unresolved.type")).range(expression);
      if (classesFound) {
        problem.withFix(new HaxeTypeAddImportIntentionAction(expression, classes));
      }
      if (membersFound) {
        problem.withFix(new HaxeStaticMemberAddImportIntentionAction(expression, members));
      }
      problem.create();
    }
  }

  private static boolean isCompileTimeMeta(HaxeReferenceExpression expression, HaxeMetadataTypeName metadataTypeName) {
    HaxeMetadataCompileTimeMeta type = PsiTreeUtil.getParentOfType(expression, HaxeMetadataCompileTimeMeta.class);
    return type != null && type.isType(metadataTypeName);
  }

  private static List<HaxeMemberModel> findStaticMembers(@NotNull String memberName,
                                                         @NotNull Project project,
                                                         @NotNull GlobalSearchScope scope,
                                                         boolean includeMethods,
                                                         boolean includeFields) {
    List<HaxeMemberModel> results = new ArrayList<>();
    if (includeMethods) {
      for (HaxeMethod method : HaxeStaticMethodNameUnifiedIndex.getByName(memberName, project, scope)) {
        if (method.isPublic()) {
          results.add(method.getModel());
        }
      }
    }
    if (includeFields) {
      for (HaxePsiField field : HaxeStaticFieldNameUnifiedIndex.getByName(memberName, project, scope)) {
        if (field.isPublic()) {
          HaxeBaseMemberModel base = field.getModel();
          if (base instanceof HaxeMemberModel memberModel) {
            results.add(memberModel);
          }
        }
      }
    }
    return results;
  }
}
