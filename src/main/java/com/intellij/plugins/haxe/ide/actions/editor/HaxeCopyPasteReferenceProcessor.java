package com.intellij.plugins.haxe.ide.actions.editor;

import com.intellij.codeInsight.editorActions.CopyPasteReferenceProcessor;
import com.intellij.codeInsight.editorActions.ReferenceData;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.util.HaxeAddImportHelper;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class HaxeCopyPasteReferenceProcessor extends CopyPasteReferenceProcessor<HaxeReferenceExpression> {

    @Override
    protected void addReferenceData(PsiFile file, int startOffset, PsiElement element, ArrayList<ReferenceData> to) {
        if(element instanceof HaxeReferenceExpression referenceExpression) {
            PsiElement resolve = referenceExpression.resolve();
            if(resolve instanceof HaxeClass haxeClass) {
                String qualifiedName = haxeClass.getQualifiedName();
                if(qualifiedName != null) {
                    addReferenceData(element, to, startOffset, qualifiedName, null);
                }
            }
        }
    }

    @Override
    protected void removeImports(@NotNull PsiFile file, @NotNull Set<String> imports) {
        // TODO organize imports?
    }

    @Override
    protected HaxeReferenceExpression @NotNull [] findReferencesToRestore(@NotNull PsiFile file, @NotNull RangeMarker bounds, ReferenceData @NotNull [] referenceData) {
        HaxeReferenceExpression[] referenceExpressions = new HaxeReferenceExpression[referenceData.length];
        if(file instanceof HaxeFile haxeFile) {
            for (int i = 0; i < referenceData.length; i++) {
                ReferenceData referenceDatum = referenceData[i];
                PsiElement element = PsiUtilCore.getElementAtOffset(haxeFile, bounds.getStartOffset() + referenceDatum.startOffset);
                if (element instanceof HaxeReferenceExpression referenceExpression) {
                    referenceExpressions[i] = referenceExpression;
                } else {
                    HaxeReferenceExpression referenceExpression = PsiTreeUtil.getParentOfType(element, HaxeReferenceExpression.class);
                    referenceExpressions[i] = referenceExpression;
                }
            }
        }

        return referenceExpressions;
    }

    record QNameAndFile(String qname, PsiFile containingFile) {
    }
    @Override
    protected void restoreReferences(ReferenceData @NotNull [] referenceData, HaxeReferenceExpression @NotNull [] referenceExpressions, @NotNull Set<? super String> imported) {
        List<QNameAndFile> importData = new ArrayList<>();
        for (int i = 0; i < referenceExpressions.length; i++) {

            HaxeReferenceExpression referenceExpression = referenceExpressions[i];
            ReferenceData referenceDatum = referenceData[i];

            if (referenceExpression != null && referenceExpression.resolve() == null) {
                importData.add(new QNameAndFile(referenceDatum.qClassName, referenceExpression.getContainingFile()));
            }
        }

        for (QNameAndFile data : importData) {
            HaxeAddImportHelper.addImport(data.qname, data.containingFile);
            imported.add(data.qname);
        }
    }
}
