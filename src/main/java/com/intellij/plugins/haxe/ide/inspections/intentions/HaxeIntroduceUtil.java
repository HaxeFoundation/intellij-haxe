package com.intellij.plugins.haxe.ide.inspections.intentions;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeAnonymousTypeModel;
import com.intellij.plugins.haxe.model.HaxeBaseMemberModel;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.HaxeAddImportHelper;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HaxeIntroduceUtil {
    public static @NotNull PsiElement findInsertAfterElementForMethod(@NotNull PsiElement startElement, HaxeClass aClass, boolean readOnly) {
        // try to add after method if element is inside a method declaration and belongs to same class
        if(PsiTreeUtil.isAncestor(aClass, startElement, true)) {
            HaxeMethodDeclaration parentMethod = PsiTreeUtil.getParentOfType(startElement, HaxeMethodDeclaration.class, HaxeModuleMethodDeclaration.class);
            if(parentMethod != null) return parentMethod;
        }
        // else if in class, find where to put it
        if (aClass != null) {
            if (readOnly) aClass = copyFileAndReturnClonedPsiElement(aClass);
            // method after the last method in class
            List<HaxeMethod> methodList = aClass.getHaxeMethodsSelf(null);
            if (!methodList.isEmpty()) return methodList.getLast();

            // else add method at the very end of class body
            PsiElement rBrace = aClass.getRBrace();
            if (rBrace != null) {
                PsiElement prevSibling = UsefulPsiTreeUtil.getPrevSiblingSkipWhiteSpaces(rBrace, true);
                if(prevSibling != null)return prevSibling;
            }
        }
        // if no success on any of the above, append as last item in the module
        HaxeModule module = PsiTreeUtil.getParentOfType(startElement, HaxeModule.class);
        return module.getLastChild();
    }



    public static @NotNull PsiElement findInsertBeforeElementFunction(@NotNull PsiElement startElement, HaxeBlockStatement block) {
        PsiElement insertBeforeElement = startElement;
        PsiElement parent = startElement.getParent();

        while (parent != null && parent != block) {
            insertBeforeElement = parent;
            parent = parent.getParent();
        }
        return insertBeforeElement;
    }

    public static @NotNull PsiElement findInsertBeforeElementForVariable(@NotNull PsiElement startElement, HaxeBlockStatement block) {
        PsiElement insertBeforeElement = startElement;
        PsiElement parent = startElement.getParent();

        while (parent != null && parent != block) {
            insertBeforeElement = parent;
            parent = parent.getParent();
        }
        return insertBeforeElement;
    }


    public static <T extends PsiElement> T copyFileAndReturnClonedPsiElement(T psiElement) {
        PsiFile originalFile = psiElement.getContainingFile();
        PsiFile fileCopy = (PsiFile) originalFile.copy();
        PsiElement element = fileCopy.findElementAt(psiElement.getTextOffset());
        while (element != null
               && (!element.getTextRange().equals(psiElement.getTextRange())
                   || element.getClass() != psiElement.getClass())
        ) {
            element = element.getParent();
        }
        return (T) element;
    }

    public static void findTypesRequiringImportsAndAddToFile(List<HaxeParameterModel> parameters, List<ResultHolder> knownParameterTypes, ResultHolder returnType, ResultHolder knownReturnType, PsiFile containingFile) {
            Set<String> qNamesToImport = new HashSet<>();
        // when generating constructors we might add parameters for fields and they wont be in the known list.
        int size = Math.min(parameters.size(), knownParameterTypes.size());
        for (int i = 0; i < size; i++) {
                HaxeParameterModel parameter = parameters.get(i);
                ResultHolder newType = parameter.getType();
                ResultHolder orgType = knownParameterTypes.get(i);
                List<HaxeClass> typesInOriginal = HaxeIntroduceUtil.collectHaxeClasses(orgType);
                List<HaxeClass> typesInGenerated = HaxeIntroduceUtil.collectHaxeClasses(newType);

                for (int j = 0; j < typesInGenerated.size(); j++) {
                    HaxeClass newHaxeClass = typesInGenerated.get(j);
                    HaxeClass orgHaxeClass = typesInOriginal.get(j);
                    if (newHaxeClass == null && orgHaxeClass != null) {
                        qNamesToImport.add(orgHaxeClass.getQualifiedName());
                    }
                }
            }
            if(returnType != null && knownReturnType != null ) {
                List<HaxeClass> typesInOriginal = HaxeIntroduceUtil.collectHaxeClasses(knownReturnType);
                List<HaxeClass> typesInGenerated = HaxeIntroduceUtil.collectHaxeClasses(returnType);
                for (int j = 0; j < typesInGenerated.size(); j++) {
                    HaxeClass newHaxeClass = typesInGenerated.get(j);
                    HaxeClass orgHaxeClass = typesInOriginal.get(j);
                    if (newHaxeClass == null && orgHaxeClass != null) {
                        qNamesToImport.add(orgHaxeClass.getQualifiedName());
                    }
                }
            }

            for (String qNames : qNamesToImport) {
                HaxeAddImportHelper.addImport(qNames, containingFile);
            }
    }


    /**
     * NOTE return null-value for unknowns in returned array
     * This is so that different imports in different classes don't result in different positions in array.
     */
    public static List<HaxeClass> collectHaxeClasses(ResultHolder resultHolder) {
        ArrayList<HaxeClass> haxeClasses = new ArrayList<>();
        collectHaxeClasses(resultHolder, haxeClasses);
        return haxeClasses;
    }
    public static void collectHaxeClasses(ResultHolder resultHolder, List<HaxeClass> haxeClasses) {
        if(resultHolder.isUnknown()){
            haxeClasses.add(null);
            return;
        }
        if(resultHolder.getType() instanceof SpecificEnumValueReference enumValueReference) {
            HaxeClass haxeClass = enumValueReference.getEnumClass().getHaxeClass();
            haxeClasses.add(haxeClass);
        }
        if(resultHolder.getType() instanceof SpecificFunctionReference functionReference) {
            for (HaxeArgument argument : functionReference.getArguments()) {
                ResultHolder type = argument.getType();
                collectHaxeClasses(type, haxeClasses);
            }
            ResultHolder returnType = functionReference.getReturnType();
            collectHaxeClasses(returnType, haxeClasses);

        }
        if(resultHolder.getType() instanceof SpecificHaxeClassReference classReference) {
            if(!classReference.isAnonymousType()) {
                HaxeClass haxeClass = classReference.getHaxeClass();
                haxeClasses.add(haxeClass);
                for (ResultHolder specific : classReference.getSpecifics()) {
                    if(classReference.isDynamic() && specific.isUnknown()) continue;
                    collectHaxeClasses(specific, haxeClasses);
                }
            }else {
                HaxeClassModel haxeClassModel = classReference.getHaxeClassModel();
                if(haxeClassModel instanceof HaxeAnonymousTypeModel anonymousTypeModel) {
                    for (ResultHolder compositeType : anonymousTypeModel.getCompositeTypes()) {
                        collectHaxeClasses(compositeType, haxeClasses);
                    }
                    for (ResultHolder extendsType : anonymousTypeModel.getExtendsTypes()) {
                        collectHaxeClasses(extendsType, haxeClasses);
                    }
                    for (HaxeBaseMemberModel member : anonymousTypeModel.getMembers(null)) {
                        ResultHolder resultType = member.getResultType(null);
                        collectHaxeClasses(resultType, haxeClasses);
                    }
                }
            }
        }
    }

}
