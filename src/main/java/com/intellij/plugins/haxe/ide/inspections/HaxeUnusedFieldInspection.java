package com.intellij.plugins.haxe.ide.inspections;

import com.intellij.codeInspection.*;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.annotator.HaxeAnnotatingVisitor;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeFieldDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataCompileTimeMeta;
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

import static com.intellij.plugins.haxe.ide.inspections.HaxeUnusedDeclarationsFixes.createAddKeepMetaFix;
import static com.intellij.plugins.haxe.ide.inspections.HaxeUnusedDeclarationsFixes.createRemoveFieldFix;
import static com.intellij.plugins.haxe.metadata.psi.HaxeMeta.KEEP;

public class HaxeUnusedFieldInspection extends LocalInspectionTool {
    @NotNull
    public String getGroupDisplayName() {
        return HaxeBundle.message("inspections.group.name");
    }

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return HaxeBundle.message("haxe.inspections.unused.field.name");
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }


    @Nullable
    @Override
    public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
        if (!(file instanceof HaxeFile)) return null;
        List<HaxeFieldDeclaration> unusedFieldDeclarations = new ArrayList<>();
        new HaxeAnnotatingVisitor() {


            @Override
            public void visitFieldDeclaration(@NotNull HaxeFieldDeclaration fieldDeclaration) {
                //Skipping  fields that are public or have  keep metadata
                if (fieldDeclaration.isPublic()) return;
                if (fieldDeclaration.isOverride()) return;
                if (fieldDeclaration.hasMetadata(KEEP, HaxeMetadataCompileTimeMeta.class)) return;

                SearchScope searchScope = GlobalSearchScope.projectScope(fieldDeclaration.getProject());
                Collection<PsiReference> references = ReferencesSearch.search(fieldDeclaration, searchScope, false).findAll();
                if (references.isEmpty()) {
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
