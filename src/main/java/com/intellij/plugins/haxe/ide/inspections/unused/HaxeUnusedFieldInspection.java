package com.intellij.plugins.haxe.ide.inspections.unused;

import com.intellij.codeInspection.*;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.annotator.HaxeAnnotatingVisitor;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeFieldDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.v2.display.HaxeUsageSearch;
import com.intellij.psi.PsiFile;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.plugins.haxe.ide.inspections.unused.HaxeUnusedDeclarationsFixes.createAddKeepMetaFix;
import static com.intellij.plugins.haxe.ide.inspections.unused.HaxeUnusedDeclarationsFixes.createRemoveFieldFix;

public class HaxeUnusedFieldInspection extends LocalInspectionTool {

    @Nullable
    @Override
    public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
        if (!(file instanceof HaxeFile)) return null;
        List<HaxeFieldDeclaration> unusedFieldDeclarations = new ArrayList<>();
        new HaxeAnnotatingVisitor() {


            @Override
            public void visitFieldDeclaration(@NotNull HaxeFieldDeclaration fieldDeclaration) {
                if (fieldDeclaration.isPublic()) return;
                if (fieldDeclaration.isOverride()) return;
                // registry-known metadata may be consumed invisibly (subsumes the
                // old @:keep check); unknown names are likely typos and do not
                // exempt - see HaxeUsageSearch.metadataKeepsAlive
                if (HaxeUsageSearch.metadataKeepsAlive(fieldDeclaration)) return;

                if (!HaxeUsageSearch.isConsideredUsed(fieldDeclaration)) {
                    unusedFieldDeclarations.add(fieldDeclaration);
                }
            }

        }.visitFile(file);


        final List<ProblemDescriptor> result = new ArrayList<>();
        for (HaxeFieldDeclaration unusedField : unusedFieldDeclarations) {
            HaxeComponentName componentName = unusedField.getComponentName();
            String nameText = componentName.getText();
            result.add(manager.createProblemDescriptor(
                    componentName,
                    HaxeBundle.message("haxe.inspections.unused.field.description", nameText),
                    new LocalQuickFix[]{
                            createAddKeepMetaFix(nameText),
                            createRemoveFieldFix(nameText)
                    },
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    isOnTheFly,
                    false
            ));
        }

        return result.isEmpty() ? ProblemDescriptor.EMPTY_ARRAY : ArrayUtil.toObjectArray(result, ProblemDescriptor.class);
    }

}
