package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeDocumentModel;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import static com.intellij.plugins.haxe.metadata.psi.HaxeMeta.NOT_NULL;
import static com.intellij.plugins.haxe.util.UsefulPsiTreeUtil.getTypeTagForMethodOrFunction;

public class HaxeReturnStatementAnnotator implements Annotator {
    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {

        if (element instanceof HaxeReturnStatement returnStatement) {
            checkReturnStatement(returnStatement, holder);
        }
    }

    private void checkReturnStatement(HaxeReturnStatement returnStatement, @NotNull AnnotationHolder holder) {
        HaxePsiCompositeElement compositeElement = PsiTreeUtil.getParentOfType(returnStatement, HaxeMethod.class, HaxeFunctionLiteral.class);
        HaxeTypeTag typeTag = getTypeTagForMethodOrFunction(compositeElement);
        if(typeTag == null) return;


        ResultHolder expectedType = HaxeTypeResolver.getTypeFromTypeTag(typeTag, compositeElement);
        ResultHolder returnedType = HaxeExpressionEvaluator.evaluate(returnStatement).result;

        if(!expectedType.canAssign(returnedType)) {
            String message = HaxeBundle.message("haxe.semantic.incompatible.type.0.should.be.1",
                    returnedType.toPresentationString(),
                    expectedType.toPresentationString());

            holder.newAnnotation(HighlightSeverity.ERROR, message)
                    .range(returnStatement.getChildren()[0])
                    .withFix(ReplaceReturnTypeFix(returnedType.toTypeString(), typeTag))
                    .create();
        }

        if (returnedType.getConstant() instanceof HaxeNull) {
            if (!expectedType.isNullWrappedType()) {
                SpecificHaxeClassReference classType = expectedType.getClassType();
                if (classType != null) {
                    HaxeClassModel model = classType.getHaxeClassModel();
                    if (model != null) {
                        if (model.hasCompileTimeMeta(NOT_NULL)) {
                            String message = HaxeBundle.message("haxe.semantic.incompatible.type.null.warning",
                                    expectedType.toPresentationString());

                            String nullWrapped = "Null<" + expectedType.toTypeString() + ">";
                            holder.newAnnotation(HighlightSeverity.WEAK_WARNING, message)
                                    .range(returnStatement.getChildren()[0])
                                    .withFix(ReplaceReturnTypeFix(nullWrapped, typeTag))
                                    .create();
                        }
                    }
                }
            }
        }

    }

    private static @NotNull HaxeFixer ReplaceReturnTypeFix(String newValue, HaxeTypeTag typeTag) {
        return new HaxeFixer("Replace return type with " + newValue) {
            @Override
            public void run() {
                HaxeDocumentModel.fromElement(typeTag).replaceElementText(typeTag, ":" + newValue);
            }
        };
    }
}
