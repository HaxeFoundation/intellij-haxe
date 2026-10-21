package com.intellij.plugins.haxe.ide.inspections.unused;

import com.intellij.codeInspection.*;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.annotator.HaxeAnnotatingVisitor;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluatorSearchUtil;
import com.intellij.plugins.haxe.v2.display.HaxeUsageSearch;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.plugins.haxe.ide.inspections.unused.HaxeUnusedDeclarationsFixes.*;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;

public class HaxeUnusedFunctionInspection extends LocalInspectionTool {

    @Nullable
    @Override
    public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
        if (!(file instanceof HaxeFile)) return null;
        HaxeCompilerSettings settings = HaxeCompilerSettings.getInstance(file.getProject());
        // the compiler's removable-code annotator owns unused-code analysis while its toggle is on
        if (settings.isCompilerDiagnosticsEnabled() && settings.isDiagnosticsRemovableCodeEnabled()) return null;
        List<HaxeLocalFunctionDeclaration> localFunctionDeclarations = new ArrayList<>();
        new HaxeAnnotatingVisitor() {


            @Override
            public void visitLocalFunctionDeclaration(@NotNull HaxeLocalFunctionDeclaration functionDeclaration) {
                SearchScope searchScope = HaxeExpressionEvaluatorSearchUtil.getSmallestPossibleSearchScope(functionDeclaration, null);
                if (!HaxeUsageSearch.isConsideredUsed(functionDeclaration, searchScope)) {
                    localFunctionDeclarations.add(functionDeclaration);
                }
            }

        }.visitFile(file);


        final List<ProblemDescriptor> result = new ArrayList<>();
        for (HaxeLocalFunctionDeclaration unusedFunction : localFunctionDeclarations) {
            HaxeComponentName componentName = unusedFunction.getComponentName();
            // note: @:overload(...) may contain unnamed functions
            if (componentName != null) {
                String nameText = componentName.getText();
                result.add(manager.createProblemDescriptor(
                        componentName,
                        HaxeBundle.message("haxe.inspections.unused.function.description", nameText),
                        new LocalQuickFix[]{
                                createRemoveFunctionFix(nameText)
                        },
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                        isOnTheFly,
                        false
                ));
            }
        }

        return result.isEmpty() ? ProblemDescriptor.EMPTY_ARRAY : ArrayUtil.toObjectArray(result, ProblemDescriptor.class);
    }

}
