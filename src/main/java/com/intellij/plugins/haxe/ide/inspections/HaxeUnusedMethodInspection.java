package com.intellij.plugins.haxe.ide.inspections;

import com.intellij.codeInspection.*;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.annotator.HaxeAnnotatingVisitor;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataCompileTimeMeta;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.psi.*;
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
import static com.intellij.plugins.haxe.ide.inspections.HaxeUnusedDeclarationsFixes.createRemoveMethodFix;
import static com.intellij.plugins.haxe.metadata.psi.HaxeMeta.KEEP;
import static com.intellij.plugins.haxe.metadata.psi.HaxeMeta.OP;
import static com.intellij.plugins.haxe.metadata.psi.HaxeMeta.POST_CONSTRUCT;

public class HaxeUnusedMethodInspection extends LocalInspectionTool {
    @NotNull
    public String getGroupDisplayName() {
        return HaxeBundle.message("inspections.group.name");
    }

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return HaxeBundle.message("haxe.inspections.unused.method.name");
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Nullable
    @Override
    public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
        if (!(file instanceof HaxeFile)) return null;
        List<HaxeMethodDeclaration> unusedMethodDeclarations = new ArrayList<>();
        new HaxeAnnotatingVisitor() {

            @Override
            public void visitMethodDeclaration(@NotNull HaxeMethodDeclaration methodDeclaration) {
                //TODO
                if (methodDeclaration.isPublic()) return;
                if (methodDeclaration.isOverride()) return;
                if (implementsAbstractParentMethod(methodDeclaration)) return;
                if (methodDeclaration.hasMetadata(OP, HaxeMetadataCompileTimeMeta.class)) return;
                if (methodDeclaration.hasMetadata(KEEP, HaxeMetadataCompileTimeMeta.class)) return;
                // DI frameworks invoke @:postConstruct / @postConstruct methods reflectively, so no references exist
                if (methodDeclaration.hasMetadata(POST_CONSTRUCT, null)) return;
                if (isGetterOrSetter(methodDeclaration)) return;
                Collection<PsiReference> references;
                //  if constructor also check new expressions
                //  NOTE: new expressions are HaxeReference so maybe we could add them
                //        to a reference index and avoid this complexity.
                if(methodDeclaration.isConstructor()) {
                    HaxeClassModel declaringClass = methodDeclaration.getModel().getDeclaringClass();
                    if(declaringClass == null) return;
                    SearchScope searchScope = GlobalSearchScope.projectScope(methodDeclaration.getProject());
                    references = ReferencesSearch.search(declaringClass.haxeClass, searchScope, false)
                            .filtering(psiReference ->
                                    psiReference.getElement().getParent() instanceof HaxeType type
                                                   && type.getParent() instanceof HaxeNewExpression)
                            .findAll();
                    // if new expression(s) found then we stop here, no need to do another search
                    if (!references.isEmpty()) return;
                }

                SearchScope searchScope = GlobalSearchScope.projectScope(methodDeclaration.getProject());
                references = ReferencesSearch.search(methodDeclaration, searchScope, false).findAll();
                if (references.isEmpty()) {
                    unusedMethodDeclarations.add(methodDeclaration);
                }

                super.visitMethodDeclaration(methodDeclaration);
            }

        }.visitFile(file);


        final List<ProblemDescriptor> result = new ArrayList<>();
        for (HaxeMethodDeclaration unusedMethod : unusedMethodDeclarations) {
            HaxeComponentName componentName = unusedMethod.getComponentName();
            String nameText = componentName.getText();
            result.add(manager.createProblemDescriptor(
                    componentName,
//                    getDisplayName(),
                    HaxeBundle.message("haxe.inspections.unused.method.description", nameText),
                    new LocalQuickFix[]{
                            createAddKeepMetaFix(nameText),
                            createRemoveMethodFix(nameText)
                    },
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    isOnTheFly,
                    false
            ));
        }

        return result.isEmpty() ? ProblemDescriptor.EMPTY_ARRAY : ArrayUtil.toObjectArray(result, ProblemDescriptor.class);
    }

    // Haxe does not require the `override` keyword when implementing an `abstract function`
    // from an abstract class parent, so such impls slip past the isOverride() guard. The call
    // site in the parent resolves only to the abstract declaration, making ReferencesSearch
    // return no hits for the concrete impl.
    private static boolean implementsAbstractParentMethod(@NotNull HaxeMethodDeclaration methodDeclaration) {
        HaxeMethodModel parent = methodDeclaration.getModel().getParentMethod(null);
        return parent != null && parent.isAbstract();
    }

    private static boolean isGetterOrSetter(@NotNull HaxeMethodDeclaration methodDeclaration) {
        String name = methodDeclaration.getModel().getName();
        if(name.startsWith("get_") || name.startsWith("set_")) {
            String propertyName = name.substring(4);
            PsiClass containingClass = methodDeclaration.getContainingClass();
            if(containingClass != null) {
                PsiField fieldByName = containingClass.findFieldByName(propertyName, true);
                return fieldByName != null;
            }
        }
        return false;
    }

}
