package com.intellij.plugins.haxe.lang.psi;

import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeReferenceUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


class HaxeResolverScopeProcessor implements PsiScopeProcessor {
    private final boolean collectAll;
    private final List<PsiElement> result;
    private final PsiElement target;
    final String name;

    HaxeResolverScopeProcessor(List<PsiElement> result, String name, PsiElement target, boolean collectAll) {
        this.target = target;
        this.result = result;
        this.name = name;
        this.collectAll = collectAll;
    }

    @Override
    public boolean execute(@NotNull PsiElement element, ResolveState state) {
        //TODO: should probably make a better solution for this using a HaxeComponentName
        if (element.getParent() instanceof HaxeEnumObjectLiteralElement || element.getParent() instanceof HaxeEnumExtractArrayLiteral) {
            // avoids adding target to list
            if (element == target) return true;
            // do not resolve a reference to elements later in the code
            if(element.getTextOffset() > target.getTextOffset())  return true;
            if (element.textMatches(name)) {
                result.add(element);
                return collectAll;
            }
        }

        // hackish workaround for captureVariables in array (HaxeSwitchCaseExprArray)
        if(element.textMatches(name) && !PsiTreeUtil.isAncestor(target, element, false)) {
            if (element instanceof HaxeReferenceExpression referenceExpression) {
                if (element.getParent() instanceof HaxeSwitchCaseExprArray || element.getParent() instanceof HaxeSwitchCaseExpr) {
                    if (HaxeReferenceUtil.isCaptureVar(referenceExpression)) {
                        result.add(element);
                        return collectAll;
                    }else if(HaxeReferenceUtil.isExtractorMatchReference(element)) {
                        result.add(element);
                        return collectAll;
                    }
                }
            }
        }



        HaxeComponentName componentName = null;
        if (element instanceof HaxeComponentName) {
            componentName = (HaxeComponentName)element;
        }
        else if (element instanceof HaxeNamedComponent) {
            componentName = ((HaxeNamedComponent)element).getComponentName();
        }
        else if (element instanceof HaxeEnumExtractedValueReference reference) {
            componentName = reference.getComponentName();
        }
        else if (element instanceof HaxeOpenParameterList parameterList) {
            componentName = parameterList.getUntypedParameter().getComponentName();
        }
        else if (element instanceof HaxeSwitchCaseExpr expr) {
            if (!executeForSwitchCase(expr)) return false;
        }
        else if (element instanceof HaxeExtractorMatchAssignExpression assignExpression) {
            if (assignExpression.getReferenceExpression() == target) return true;
            if (assignExpression.getReferenceExpression().textMatches(name)) {
                result.add(assignExpression.getReferenceExpression());
                return collectAll;
            }
        }

        if (componentName != null
                &&  componentName.textMatches(name)
                && !PsiTreeUtil.isAncestor(target, element, false))
        {
            result.add(componentName);
            return collectAll;
        }
        return true;
    }

    private boolean executeForSwitchCase(HaxeSwitchCaseExpr expr) {
        if (expr.getSwitchCaseCaptureVar() != null) {
            HaxeComponentName componentName = expr.getSwitchCaseCaptureVar().getComponentName();
            if (name.equals(componentName.getText())) {
                result.add(componentName);
                return false;
            }
        }
        else {
            HaxeExpression expression = expr.getExpression();
            if (expression instanceof HaxeReference reference) {
                if (name.equals(reference.getText())) {
                    //TODO mlo: figure out of non HaxeComponentName elements are OK in Result list
                    result.add(expr);
                    return false;
                }
            }
            else if (expression instanceof HaxeEnumArgumentExtractor extractor) {
                HaxeEnumExtractorArgumentList argumentList = extractor.getEnumExtractorArgumentList();

                List<HaxeEnumExtractedValue> list = argumentList.getEnumExtractedValueList();
                for (HaxeEnumExtractedValue extractedValue : list) {
                    HaxeEnumExtractedValueReference valueReference = extractedValue.getEnumExtractedValueReference();
                    if (valueReference != null) {
                        HaxeComponentName componentName = valueReference.getComponentName();
                        if (name.equals(componentName.getText())) {
                            result.add(componentName);
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    @Override
    public <T> T getHint(@NotNull Key<T> hintKey) {
        return null;
    }

    @Override
    public void handleEvent(Event event, @Nullable Object associated) {
    }



}