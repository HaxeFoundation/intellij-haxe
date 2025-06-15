package com.intellij.plugins.haxe.ide.actions.editor;

import com.intellij.codeInsight.editorActions.CopyPasteReferenceProcessor;
import com.intellij.codeInsight.editorActions.ReferenceData;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeImportStatement;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.model.HaxeImportModel;
import com.intellij.plugins.haxe.util.HaxeAddImportHelper;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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
        if (!imports.isEmpty()) {
            if (file instanceof HaxeFile haxeFile) {
                List<HaxeImportStatement> toDelete = new ArrayList<>();
                for (String anImport : imports) {
                    List<HaxeImportModel> importModels = haxeFile.getModel().getImportModels();
                    for (HaxeImportModel importModel : importModels) {
                        HaxeReferenceExpression referenceExpression = importModel.getReferenceExpression();
                        if (referenceExpression != null && referenceExpression.textMatches(anImport)) {
                            toDelete.add(importModel.getBasePsi());
                            break;
                        }
                    }
                }
                if(!toDelete.isEmpty()) {
                    toDelete.forEach(PsiElement::delete);
                }
            }
        }
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

    // 2025.2 signature
    protected void restoreReferences(ReferenceData @NotNull [] referenceData, List<HaxeReferenceExpression> referenceExpressions, @NotNull Set<? super String> imported) {
        Set<QNameAndFile> importData = new HashSet<>();
        for (int i = 0; i < referenceExpressions.size(); i++) {

            HaxeReferenceExpression referenceExpression = referenceExpressions.get(i);
            ReferenceData referenceDatum = referenceData[i];

            if (referenceExpression != null && referenceExpression.resolve() == null) {
                importData.add(new QNameAndFile(referenceDatum.qClassName, referenceExpression.getContainingFile()));
            }
        }

        for (QNameAndFile data : importData) {
            // ignoring top-level classes when adding imports
            if(data.qname.contains(".")) {
                HaxeAddImportHelper.addImport(data.qname, data.containingFile);
                imported.add(data.qname);
            }
        }
    }

    record QNameAndFile(String qname, PsiFile containingFile) {
    }
    // 2025.1 signature
    protected void restoreReferences(ReferenceData @NotNull [] referenceData, HaxeReferenceExpression @NotNull [] referenceExpressions, @NotNull Set<? super String> imported) {
        restoreReferences(referenceData, Arrays.stream(referenceExpressions).toList(), imported);
    }
}
