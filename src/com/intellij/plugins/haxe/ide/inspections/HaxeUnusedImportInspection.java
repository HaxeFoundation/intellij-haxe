package com.intellij.plugins.haxe.ide.inspections;

import com.intellij.codeInspection.*;
import com.intellij.lang.ImportOptimizer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.HaxeImportOptimizer;
import com.intellij.plugins.haxe.lang.psi.HaxeImportStatement;
import com.intellij.plugins.haxe.util.HaxeImportUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fedorkorotkov.
 */
public class HaxeUnusedImportInspection extends LocalInspectionTool {
  @NotNull
  public String getGroupDisplayName() {
    return HaxeBundle.message("inspections.group.name");
  }

  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return HaxeBundle.message("haxe.inspection.unused.import.name");
  }

  @Override
  public boolean isEnabledByDefault() {
    return true;
  }

  @NotNull
  @Override
  public String getShortName() {
    return "HaxeUnusedImport";
  }

  @Nullable
  @Override
  public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
    List<HaxeImportStatement> unusedImports = HaxeImportUtil.findUnusedImports(file);
    if (unusedImports.isEmpty()) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    final List<ProblemDescriptor> result = new ArrayList<ProblemDescriptor>();
    for (HaxeImportStatement haxeImportStatement : unusedImports) {
      result.add(manager.createProblemDescriptor(
        haxeImportStatement,
        TextRange.from(0, haxeImportStatement.getTextLength()),
        getDisplayName(),
        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
        isOnTheFly,
        OPTIMIZE_IMPORTS_FIX
      ));
    }
    return ArrayUtil.toObjectArray(result, ProblemDescriptor.class);
  }

  private static final LocalQuickFix OPTIMIZE_IMPORTS_FIX = new LocalQuickFix() {
    @NotNull
    @Override
    public String getName() {
      return HaxeBundle.message("haxe.fix.optimize.imports");
    }

    @NotNull
    public String getFamilyName() {
      return getName();
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      PsiElement psiElement = descriptor.getPsiElement();
      invoke(project, psiElement.getContainingFile());
    }

    public void invoke(@NotNull final Project project, PsiFile file) {
      ImportOptimizer optimizer = new HaxeImportOptimizer();
      final Runnable runnable = optimizer.processFile(file);
      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        public void run() {
          CommandProcessor.getInstance().executeCommand(project, runnable, getFamilyName(), this);
        }
      });
    }
  };
}
