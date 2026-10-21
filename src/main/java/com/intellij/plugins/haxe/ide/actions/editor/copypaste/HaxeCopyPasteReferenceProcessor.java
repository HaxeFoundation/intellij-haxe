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

    @Override
    protected void addReferenceData(PsiFile file, int startOffset, List<PsiElement> elements,
                                    ArrayList<HaxeReferenceData> to) {
        HaxeImportCandidates candidates = HaxeImportCandidates.of((HaxeFile)file);
        for (PsiElement element : elements) {
            if (element instanceof HaxeReferenceExpression reference && candidates.mayNeedImport(reference)) {
                addResolvedReference(startOffset, reference, to);
            }
        }
    }

    /** A class or static method the reference resolves to becomes a restorable import; anything else needs none. */
    private void addResolvedReference(int startOffset, HaxeReferenceExpression reference,
                                      ArrayList<HaxeReferenceData> to) {
        switch (reference.resolve()) {
            case HaxeClass haxeClass -> addClassReference(startOffset, reference, haxeClass, to);
            case HaxeMethod method when method.isStatic() -> addStaticMethodReference(startOffset, reference, method, to);
            case null, default -> {
            }
        }
    }

    private void addClassReference(int startOffset, HaxeReferenceExpression reference, HaxeClass haxeClass,
                                   ArrayList<HaxeReferenceData> to) {
        String qualifiedName = haxeClass.getQualifiedName();
        if (qualifiedName == null) return;
        addHaxeReferenceData(reference, to, startOffset, qualifiedName, false, false);
    }

    private void addStaticMethodReference(int startOffset, HaxeReferenceExpression reference, HaxeMethod method,
                                          ArrayList<HaxeReferenceData> to) {
        FullyQualifiedInfo qualifiedInfo = method.getModel().getQualifiedInfo();
        if (qualifiedInfo == null) return;
        boolean isExtensionMethod = reference.getParent() instanceof HaxeCallExpression callExpression
                                    && callExpression.resolveIsStaticExtension();
        addHaxeReferenceData(reference, to, startOffset, importPathOf(qualifiedInfo), true, isExtensionMethod);
    }

    /**
     * The path an import names for a static member: package, module, the
     * class only when it is not the module's main class, then the member.
     * FullyQualifiedInfo.toString() prints module AND class even when they
     * are the same name, which no import accepts.
     */
    private static String importPathOf(FullyQualifiedInfo info) {
        List<String> parts = new ArrayList<>();
        if (info.packageName != null && !info.packageName.isEmpty()) parts.add(info.packageName);
        if (info.moduleName != null) parts.add(info.moduleName);
        if (info.className != null && !info.className.equals(info.moduleName)) parts.add(info.className);
        if (info.memberName != null) parts.add(info.memberName);
        return String.join(".", parts);
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

    @Override
    protected void restoreReferences(HaxeReferenceData @NotNull [] referenceData,
                                     HaxeReferenceExpression @NotNull [] referenceExpressions,
                                     @NotNull Set<? super String> imported) {
        Set<QNameAndFile> importData = new HashSet<>();
        for (int i = 0; i < referenceExpressions.length; i++) {
            HaxeReferenceExpression referenceExpression = referenceExpressions[i];
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

    record QNameAndFile(String qname, PsiFile containingFile, boolean extensionMethod) {
    }
}
