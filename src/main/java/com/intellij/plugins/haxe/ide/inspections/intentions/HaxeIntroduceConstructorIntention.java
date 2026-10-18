package com.intellij.plugins.haxe.ide.inspections.intentions;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeConstructorDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeNewExpression;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.plugins.haxe.ide.inspections.intentions.HaxeIntroduceUtil.findInsertAfterElementForMethod;
import static com.intellij.plugins.haxe.ide.inspections.intentions.HaxeIntroduceUtil.findTypesRequiringImportsForMethodAndAddToFile;

/** Creates the constructor an unresolved {@code new} names, its parameters derived from the call's arguments. */
public class HaxeIntroduceConstructorIntention
        extends HaxeUnresolvedSymbolIntentionBase<HaxeNewExpression>
        implements HighPriorityAction {

    protected final @NotNull SmartPsiElementPointer<HaxeClass> myPsiTargetPointer;
    private final String typeName;

    public HaxeIntroduceConstructorIntention(@NotNull HaxeNewExpression newExpression, HaxeClass targetClass) {
        super(newExpression);
        typeName = newExpression.getType().getReferenceExpression().getText();
        myPsiTargetPointer = createPointer(targetClass);
    }

    @Override
    public @IntentionName @NotNull String getText() {
        return HaxeBundle.message("haxe.intention.create.constructor", typeName);
    }

    @Override
    protected String getPreviewName() {
        HaxeClass aClass = myPsiTargetPointer.getElement();
        return aClass == null ? null : aClass.getQualifiedName();
    }

    @Override
    protected PsiElement getTargetPsi() {
        return findInsertAfterElementForMethod(myPsiElementPointer.getElement(), myPsiTargetPointer.getElement(), false);
    }

    @Override
    protected PsiFile perform(@NotNull Project project, @NotNull PsiElement element, @NotNull Editor editor, boolean preview) {
        PsiElement anchor = findInsertAfterElementForMethod(element, myPsiTargetPointer.getElement(), preview);

        PsiElement constructorDeclaration = generateConstructorDeclaration(project).copy();
        constructorDeclaration = anchor.getParent().addAfter(constructorDeclaration, anchor);
        anchor.getParent().addBefore(createNewLine(project), constructorDeclaration);
        constructorDeclaration = CodeStyleManager.getInstance(project).reformat(constructorDeclaration);

        if (!preview && constructorDeclaration instanceof HaxeConstructorDeclaration declaration) {
            HaxeMethodModel newModel = declaration.getModel();
            List<HaxeParameterModel> parameters = newModel.getParameters();
            ResultHolder returnType = newModel.getReturnType(null);
            ResultHolder knownReturnType = guessElementType(myPsiElementPointer.getElement());
            if (knownReturnType.isDynamic() || knownReturnType.isUnknown()) knownReturnType = null;
            findTypesRequiringImportsForMethodAndAddToFile(parameters, argumentTypes(arguments()), returnType, knownReturnType, anchor.getContainingFile());
        }
        return anchor.getContainingFile();
    }

    private PsiElement generateConstructorDeclaration(@NotNull Project project) {
        String function = """
                public function new (%s) {

                }
                """
                .formatted(parameterListFromArguments(arguments(), false));
        return HaxeElementGenerator.createConstructorDeclaration(project, function);
    }

    private List<HaxeExpression> arguments() {
        HaxeNewExpression element = myPsiElementPointer.getElement();
        return element == null ? List.of() : element.getExpressionList();
    }
}
