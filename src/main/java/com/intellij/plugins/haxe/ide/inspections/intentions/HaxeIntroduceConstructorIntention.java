package com.intellij.plugins.haxe.ide.inspections.intentions;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeConstructorDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeNewExpression;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.plugins.haxe.util.HaxeNameSuggesterUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.intellij.plugins.haxe.ide.inspections.intentions.HaxeIntroduceUtil.findInsertAfterElementForMethod;
import static com.intellij.plugins.haxe.ide.inspections.intentions.HaxeIntroduceUtil.findTypesRequiringImportsForMethodAndAddToFile;

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
        return "Create constructor for '" + typeName + "'";
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
//TODO
//        generateMissingImports()

        constructorDeclaration = CodeStyleManager.getInstance(project).reformat(constructorDeclaration);
        if (!preview) {
            if (constructorDeclaration instanceof HaxeConstructorDeclaration declaration) {
                HaxeMethodModel newModel = declaration.getModel();
                List<HaxeParameterModel> parameters = newModel.getParameters();
                ResultHolder returnType = newModel.getReturnType(null);

                ResultHolder knownReturnType = guessElementType(myPsiElementPointer.getElement());
                if (knownReturnType.isDynamic() || knownReturnType.isUnknown()) knownReturnType = null;
                findTypesRequiringImportsForMethodAndAddToFile(parameters, getKnownParameterTypeList(), returnType, knownReturnType, anchor.getContainingFile());
            }
        }
        return anchor.getContainingFile();
    }

    private PsiElement generateConstructorDeclaration(@NotNull Project project) {
        String function = """
                public function new (%s) {
                
                }
                """
                .formatted(generateParameterList());
        return HaxeElementGenerator.createConstructorDeclaration(project, function);
    }

    private List<ResultHolder> getKnownParameterTypeList() {
        HaxeNewExpression element = myPsiElementPointer.getElement();
        if (element == null) return List.of();
        List<ResultHolder> parameterTypes = new ArrayList<>();
        @NotNull List<HaxeExpression> list = element.getExpressionList();
        for (HaxeExpression expression : list) {
            ResultHolder type = HaxeExpressionEvaluator.evaluate(expression, null).result;
            parameterTypes.add(type);
        }
        return parameterTypes;
    }

    private String generateParameterList() {
        StringBuilder builder = new StringBuilder();
        HaxeNewExpression element = myPsiElementPointer.getElement();
        if (element != null) {
            Set<String> used = new HashSet<>();
            @NotNull List<HaxeExpression> list = element.getExpressionList();
            for (int i = 0; i < list.size(); i++) {
                HaxeExpression expression = list.get(i);
                ResultHolder type = HaxeExpressionEvaluator.evaluate(expression, null).result;
                String paramName = "p" + i;
                String typeTag = "";
                if (!type.isUnknown()) {
                    List<String> names = HaxeNameSuggesterUtil.getSuggestedNames(expression, false, false, used);
                    if (!names.isEmpty()) {
                        String name = names.getFirst();
                        used.add(name);
                        paramName = name;
                    }
                    typeTag = ":" + getTypeName(type);
                }
                builder.append(paramName).append(typeTag);
                if (i + 1 != list.size()) {
                    builder.append(",");
                }
            }
        }
        return builder.toString();
    }
}