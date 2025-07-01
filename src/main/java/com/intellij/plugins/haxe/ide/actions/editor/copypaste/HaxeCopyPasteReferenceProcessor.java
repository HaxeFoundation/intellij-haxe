package com.intellij.plugins.haxe.ide.actions.editor.copypaste;

import com.intellij.codeInsight.editorActions.ReferenceData;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeImportModel;
import com.intellij.plugins.haxe.model.HaxeUsingModel;
import com.intellij.plugins.haxe.util.HaxeAddImportHelper;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class HaxeCopyPasteReferenceProcessor extends HaxeBaseCopyPasteReferenceProcessor<HaxeReferenceExpression> {

    protected void addReferenceData(PsiFile file, int startOffset, PsiElement element, ArrayList<HaxeReferenceData> to) {
        if (element instanceof HaxeReferenceExpression referenceExpression) {
            PsiElement resolve = referenceExpression.resolve();
            if (resolve instanceof HaxeClass haxeClass) {
                String qualifiedName = haxeClass.getQualifiedName();
                if (qualifiedName != null) {
                    addHaxeReferenceData(element, to, startOffset, qualifiedName, false, false);
                }
            } else if (resolve instanceof HaxeMethod method && method.isStatic()) {
                FullyQualifiedInfo qualifiedInfo = method.getModel().getQualifiedInfo();

                boolean isExtensionMethod = false;
                if (element.getParent() instanceof HaxeCallExpression callExpression) {
                    isExtensionMethod = callExpression.resolveIsStaticExtension();
                }

                if (qualifiedInfo != null) {
                    String qualifiedName = qualifiedInfo.toString();
                    addHaxeReferenceData(element, to, startOffset, qualifiedName, true, isExtensionMethod);
                }
            }
        }
    }

    private void addHaxeReferenceData(PsiElement element,
                                      ArrayList<HaxeReferenceData> elementList,
                                      int offset,
                                      String qualifiedName,
                                      boolean isStatic,
                                      boolean isExtensionMethod) {

        TextRange range = element.getTextRange();
        int startOffset = range.getStartOffset() - offset;
        int emdOffset = range.getEndOffset() - offset;
        elementList.add(
                new HaxeReferenceData(
                        startOffset,
                        emdOffset,
                        qualifiedName,
                        isStatic,
                        isExtensionMethod)
        );
    }

    protected void removeImports(@NotNull PsiFile file, @NotNull Set<String> imports) {
        if (!imports.isEmpty()) {
            if (file instanceof HaxeFile haxeFile) {
                List<PsiElement> toDelete = new ArrayList<>();
                for (String anImport : imports) {
                    List<HaxeImportModel> importModels = haxeFile.getModel().getImportModels();
                    List<HaxeUsingModel> usingModels = haxeFile.getModel().getUsingModels();
                    for (HaxeImportModel importModel : importModels) {
                        HaxeReferenceExpression referenceExpression = importModel.getReferenceExpression();
                        if (referenceExpression != null && referenceExpression.textMatches(anImport)) {
                            toDelete.add(importModel.getBasePsi());
                            break;
                        }
                    }
                    for (HaxeUsingModel usingModel : usingModels) {
                        HaxeReferenceExpression referenceExpression = usingModel.getReferenceExpression();
                        if (referenceExpression != null && referenceExpression.textMatches(anImport)) {
                            toDelete.add(usingModel.getBasePsi());
                            break;
                        }
                    }
                }
                if (!toDelete.isEmpty()) {
                    toDelete.forEach(PsiElement::delete);
                }
            }
        }
    }

    protected HaxeReferenceExpression @NotNull [] findReferencesToRestore(@NotNull PsiFile file, @NotNull RangeMarker bounds, HaxeReferenceData @NotNull [] referenceData) {
        HaxeReferenceExpression[] referenceExpressions = new HaxeReferenceExpression[referenceData.length];
        if (file instanceof HaxeFile haxeFile) {
            for (int i = 0; i < referenceData.length; i++) {
                ReferenceData referenceDatum = referenceData[i];
                if (referenceDatum instanceof HaxeReferenceData haxeReferenceData) {
                    PsiElement element = PsiUtilCore.getElementAtOffset(haxeFile, bounds.getStartOffset() + haxeReferenceData.startOffset);
                    if (element instanceof HaxeReferenceExpression referenceExpression) {
                        referenceExpressions[i] = referenceExpression;
                    } else {
                        HaxeReferenceExpression referenceExpression = PsiTreeUtil.getParentOfType(element, HaxeReferenceExpression.class);
                        referenceExpressions[i] = referenceExpression;
                    }
                }
            }
        }

        return referenceExpressions;
    }

    // 2025.2 signature
    protected void restoreReferences(HaxeReferenceData @NotNull [] referenceData, List<HaxeReferenceExpression> referenceExpressions, @NotNull Set<? super String> imported) {
        Set<QNameAndFile> importData = new HashSet<>();
        for (int i = 0; i < referenceExpressions.size(); i++) {

            HaxeReferenceExpression referenceExpression = referenceExpressions.get(i);
            ReferenceData referenceDatum = referenceData[i];
            if(referenceDatum instanceof  HaxeReferenceData haxeReferenceData) {
                if (referenceExpression != null && referenceExpression.resolve() == null) {
                    importData.add(new QNameAndFile(haxeReferenceData.qClassName, referenceExpression.getContainingFile(), haxeReferenceData.isExtensionMethod));
                }
            }
        }

        for (QNameAndFile data : importData) {
            // ignoring top-level classes when adding imports
            if (data.qname.contains(".")) {
                PsiElement classOrMemberByQName = HaxeResolveUtil.findClassOrMemberByQName(data.qname, data.containingFile);
                if (classOrMemberByQName  instanceof HaxeClass) {
                    HaxeAddImportHelper.addImport(data.qname, data.containingFile);
                    imported.add(data.qname);
                }else if (classOrMemberByQName instanceof HaxeMethod haxeMethod) {
                    if(data.extensionMethod) {
                        PsiClass containingClass = haxeMethod.getContainingClass();
                        if(containingClass != null) {
                            String extensionMethodClass = containingClass.getQualifiedName();
                            HaxeAddImportHelper.addUsing(extensionMethodClass, data.containingFile);
                            imported.add(extensionMethodClass);
                        }
                    }else {
                        HaxeAddImportHelper.addImport(data.qname, data.containingFile);
                        imported.add(data.qname);
                    }
                }
            }
        }
    }


    // 2025.1 signature
    protected void restoreReferences(HaxeReferenceData @NotNull [] referenceData, HaxeReferenceExpression @NotNull [] referenceExpressions, @NotNull Set<? super String> imported) {
        restoreReferences(referenceData, Arrays.stream(referenceExpressions).toList(), imported);
    }


    record QNameAndFile(String qname, PsiFile containingFile, boolean extensionMethod) {
    }
}
