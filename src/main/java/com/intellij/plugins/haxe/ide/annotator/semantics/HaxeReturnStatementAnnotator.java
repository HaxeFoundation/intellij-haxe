package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeDocumentModel;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
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
        if (element instanceof HaxeMethod method) {
            checkForMissingReturnStatement(method, holder);
        }
    }

    private void checkForMissingReturnStatement(HaxeMethod method, @NotNull AnnotationHolder holder) {
        // skip interfaces  etc.
        if(method.getBody() == null) return;

        HaxeTypeTag typeTag = getTypeTagForMethodOrFunction(method);
        if(typeTag == null) return;

        ResultHolder typeTagType = HaxeTypeResolver.getTypeFromTypeTag(typeTag, method);
        if(typeTagType.isVoid()) return;

        //TODO traverse tree and find branches without return statement
        HaxeReturnStatement[] childrenOfType = PsiTreeUtil.getChildrenOfType(method.getBody(), HaxeReturnStatement.class);

        if(childrenOfType== null  || childrenOfType.length == 0) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Missing return statement")
                    .range(method.getBody().getLastChild())
                    .create();
        }

    }

    private void checkReturnStatement(HaxeReturnStatement returnStatement, @NotNull AnnotationHolder holder) {
        HaxePsiCompositeElement compositeElement = PsiTreeUtil.getParentOfType(returnStatement, HaxeMethod.class, HaxeFunctionLiteral.class);
        HaxeTypeTag typeTag = getTypeTagForMethodOrFunction(compositeElement);
        if(typeTag == null) return;


        ResultHolder expectedType = HaxeTypeResolver.getTypeFromTypeTag(typeTag, compositeElement);
        ResultHolder returnedType = HaxeExpressionEvaluator.evaluate(returnStatement).result;

        PsiElement highlightElement = returnStatement.getChildren().length != 0 ? returnStatement.getChildren()[0] : returnStatement;

        if(!expectedType.canAssign(returnedType)) {
            String message = HaxeBundle.message("haxe.semantic.incompatible.type.0.should.be.1",
                    returnedType.toPresentationString(),
                    expectedType.toPresentationString());

            holder.newAnnotation(HighlightSeverity.ERROR, message)
                    .range(highlightElement)
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
                                    .range(highlightElement)
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

            @Override
            public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
                int startOffset = typeTag.getTextRange().getStartOffset();
                int endOffset = typeTag.getTextRange().getEndOffset();
                editor.getDocument().replaceString(startOffset,endOffset , ":" + newValue);

                return IntentionPreviewInfo.DIFF;
            }
        };
    }
}
