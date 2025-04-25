package com.intellij.plugins.haxe.ide.inspections;

import com.intellij.codeInspection.*;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.annotator.HaxeAnnotatingVisitor;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataCompileTimeMeta;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluatorSearchUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.intellij.plugins.haxe.ide.inspections.HaxeUnusedDeclarationsFixes.*;
import static com.intellij.plugins.haxe.metadata.psi.HaxeMeta.KEEP;

public class HaxeUnusedFunctionInspection extends LocalInspectionTool {
    @NotNull
    public String getGroupDisplayName() {
        return HaxeBundle.message("inspections.group.name");
    }

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return HaxeBundle.message("haxe.inspections.unused.function.name");
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }


    @Nullable
    @Override
    public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
        if (!(file instanceof HaxeFile)) return null;
        List<HaxeLocalFunctionDeclaration> LocalFunctionDeclarations = new ArrayList<>();
        new HaxeAnnotatingVisitor() {


            @Override
            public void visitLocalFunctionDeclaration(@NotNull HaxeLocalFunctionDeclaration functionDeclaration) {
                SearchScope searchScope = HaxeExpressionEvaluatorSearchUtil.getSmallestPossibleSearchScope(functionDeclaration, null);
                Collection<PsiReference> references = ReferencesSearch.search(functionDeclaration, searchScope, false).findAll();
                if (references.isEmpty()) {
                    LocalFunctionDeclarations.add(functionDeclaration);
                }
            }

        }.visitFile(file);


        final List<ProblemDescriptor> result = new ArrayList<>();
        for (HaxeLocalFunctionDeclaration unusedFunction : LocalFunctionDeclarations) {
            HaxeComponentName componentName = unusedFunction.getComponentName();
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

        return result.isEmpty() ? ProblemDescriptor.EMPTY_ARRAY : ArrayUtil.toObjectArray(result, ProblemDescriptor.class);
    }

}
