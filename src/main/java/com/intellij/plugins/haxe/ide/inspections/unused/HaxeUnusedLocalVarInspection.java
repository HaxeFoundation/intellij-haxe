package com.intellij.plugins.haxe.ide.inspections.unused;

import com.intellij.codeInspection.*;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.annotator.HaxeAnnotatingVisitor;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeLocalVarDeclaration;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluatorSearchUtil;
import com.intellij.plugins.haxe.v2.display.HaxeUsageSearch;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.plugins.haxe.ide.inspections.unused.HaxeUnusedDeclarationsFixes.createRemoveVarFix;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;

public class HaxeUnusedLocalVarInspection extends LocalInspectionTool {

    @Nullable
    @Override
    public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
        if (!(file instanceof HaxeFile)) return null;
        HaxeCompilerSettings settings = HaxeCompilerSettings.getInstance(file.getProject());
        // the compiler's removable-code annotator owns unused-code analysis while its toggle is on
        if (settings.isCompilerDiagnosticsEnabled() && settings.isDiagnosticsRemovableCodeEnabled()) return null;
        List<HaxeLocalVarDeclaration> unusedVarDeclarations = new ArrayList<>();
        new HaxeAnnotatingVisitor() {

            @Override
            public void visitLocalVarDeclaration(@NotNull HaxeLocalVarDeclaration varDeclaration) {
                SearchScope searchScope = HaxeExpressionEvaluatorSearchUtil.getSmallestPossibleSearchScope(varDeclaration, null);
                if (!HaxeUsageSearch.isConsideredUsed(varDeclaration, searchScope)) {
                    unusedVarDeclarations.add(varDeclaration);
                }
            }
        }.visitFile(file);


        final List<ProblemDescriptor> result = new ArrayList<>();
        for (HaxeLocalVarDeclaration unusedVar : unusedVarDeclarations) {
            HaxeComponentName componentName = unusedVar.getComponentName();
            String nameText = componentName.getText();
            result.add(manager.createProblemDescriptor(
                    componentName,
                    HaxeBundle.message("haxe.inspections.unused.var.description", nameText),
                    new LocalQuickFix[]{createRemoveVarFix(componentName.getText())},
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    isOnTheFly,
                    false
            ));
        }


        return result.isEmpty() ? ProblemDescriptor.EMPTY_ARRAY : ArrayUtil.toObjectArray(result, ProblemDescriptor.class);
    }

}
