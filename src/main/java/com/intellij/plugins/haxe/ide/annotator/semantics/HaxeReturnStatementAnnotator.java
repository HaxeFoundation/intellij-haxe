package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.annotator.HaxeStandardAnnotation;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeDocumentModel;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.evaluator.assign.AssignExplanation;
import com.intellij.plugins.haxe.model.evaluator.assign.HaxeAssignEvaluation;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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

        @NotNull PsiElement[] children = method.getBody().getChildren();
        boolean hasAllPathsCovered = hasReturnPathsCovered(children);

        if(!hasAllPathsCovered) {
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

        boolean hasReturnValue = returnStatement.getChildren().length != 0;
        PsiElement highlightElement = hasReturnValue ? returnStatement.getChildren()[0] : returnStatement;

        if(expectedType.isVoid() && returnedType.getConstant() != null) {
            holder.newAnnotation(HighlightSeverity.ERROR,  HaxeBundle.message("haxe.semantic.incompatible.return.void",returnedType.getConstant().toString()))
                    .range(highlightElement)
                    .withFix(ReplaceReturnTypeFix(returnedType.toTypeString(), typeTag))
                    .create();
        }

        else {
            HaxeAssignEvaluation haxeAssignEvaluation = expectedType.canAssignEvaluation(returnedType);
            if(!haxeAssignEvaluation.result) {
                AssignExplanation messages = haxeAssignEvaluation.explanations;
                if(messages.hasMissingMembers() || messages.hasWrongTypeMembers()) {
                    if(messages.hasMissingMembers()) {
                        HaxeStandardAnnotation.typeMismatchMissingMembers(holder, returnStatement, messages)
                                .create();
                    }
                    if(messages.hasWrongTypeMembers()) {
                        HaxeStandardAnnotation.addtypeMismatchWrongTypeMembersAnnotations(holder, returnStatement, messages);
                    }
                }
                else {
                    String message = HaxeBundle.message("haxe.semantic.incompatible.type.0.should.be.1",
                            returnedType.toPresentationString(),
                            expectedType.toPresentationString());

                    holder.newAnnotation(HighlightSeverity.ERROR, message)
                            .range(highlightElement)
                            .withFix(ReplaceReturnTypeFix(returnedType.toTypeString(), typeTag))
                            .create();
                }

            }

            else if (returnedType.getConstant() instanceof HaxeNull) {
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


    private static boolean hasReturnPathsCovered(@NotNull PsiElement[] children) {
        boolean hasReturnPaths = false;
        for (PsiElement child : children) {
            // TODO mlo: would this work if we only check last statement ?
            // would have to make sure last element is not comment,conditional compilation or something like that

            if(child instanceof PsiComment) continue;
            hasReturnPaths = hasReturnPathsCovered(child);
        }
        return hasReturnPaths;
    }

    private static boolean hasReturnPathsCovered(@Nullable PsiElement child) {
        // blocks to check
        // if else, switch-case, try-catch, for-loop, while-loop, block/scope

        if(child == null) return true;

        if(child instanceof HaxeIfStatement ifStatement) {
            HaxeGuardedStatement guardedStatement = ifStatement.getGuardedStatement();
            HaxeElseStatement elseStatement = ifStatement.getElseStatement();

            if(guardedStatement!= null && !hasReturnPathsCovered(guardedStatement.getChildren())) {
                return false;
            }
            if(elseStatement != null && !hasReturnPathsCovered(elseStatement.getChildren())) {
                return false;
            }
            return true;
        }
        if(child instanceof HaxeSwitchStatement switchStatement) {
            HaxeSwitchBlock switchBlock = switchStatement.getSwitchBlock();
            if(switchBlock!= null) {
                boolean allCasesHaveReturn = true;
                List<HaxeSwitchCase> switchCaseList = switchBlock.getSwitchCaseList();
                for (HaxeSwitchCase haxeSwitchCase : switchCaseList) {
                    HaxeSwitchCaseBlock switchCaseBlock = haxeSwitchCase.getSwitchCaseBlock();
                    if(switchCaseBlock != null) {
                        allCasesHaveReturn = allCasesHaveReturn && hasReturnPathsCovered(switchCaseBlock.getChildren());
                    }
                }
                return allCasesHaveReturn;
            }
            return false;
        }
        if(child instanceof HaxeTryStatement tryStatement) {
            return hasReturnPathsCovered(tryStatement.getChildren());
        }
        if(child instanceof HaxeCatchStatement catchStatement) {
            return hasReturnPathsCovered(catchStatement.getChildren());
        }
        if(child instanceof HaxeForStatement forStatement) {
            HaxeBlockStatement blockStatement = forStatement.getBlockStatement();
            if(blockStatement != null) {
                return hasReturnPathsCovered(blockStatement.getChildren());
            }
            return false;
        }
        if (child instanceof HaxeDoWhileStatement doWhileStatement) {
            HaxeDoWhileBody body = doWhileStatement.getBody();
            if(body != null) {
                return hasReturnPathsCovered(body.getChildren());
            }
            return false;
        }
        if (child instanceof HaxeWhileStatement whileStatement) {
            HaxeDoWhileBody body = whileStatement.getBody();
            if(body != null) {
                return hasReturnPathsCovered(body.getChildren());
            }
            return false;
        }

        if(child instanceof HaxeBlockStatement blockStatement) {
            return hasReturnPathsCovered(blockStatement.getChildren());
        }

        if(child instanceof HaxeReturnStatement) {
            return true;
        }
        if(child instanceof HaxeThrowStatement) {
            return true;
        }
        // ignore comments
        if(child instanceof PsiComment) return true;

        return false;
    }


}
