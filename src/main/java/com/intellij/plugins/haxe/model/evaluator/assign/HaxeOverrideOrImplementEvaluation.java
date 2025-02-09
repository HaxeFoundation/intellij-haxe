package com.intellij.plugins.haxe.model.evaluator.assign;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.intellij.plugins.haxe.model.evaluator.assign.HaxeTypeCompatible.canAssignToFromContravariance;
import static com.intellij.plugins.haxe.model.evaluator.assign.HaxeTypeCompatible.canAssignToFromReference;

public class HaxeOverrideOrImplementEvaluation {
    @NotNull
    private final HaxeMethodModel sourceModel;
    @NotNull
    private final HaxeMethodModel targetModel;

    private final boolean makeAnnotations;

    public boolean result = true;
    public boolean complete = false;

    private List<Annotation> annotations = new ArrayList<>();


    public HaxeOverrideOrImplementEvaluation(@NotNull HaxeMethodModel sourceModel, @NotNull HaxeMethodModel targetModel, boolean makeAnnotations) {
        this.sourceModel = sourceModel;
        this.targetModel = targetModel;
        this.makeAnnotations = makeAnnotations;
    }


    // When overriding a method:
    // - The new implementation must accept the same parameter types as the original but can be stricter and  accept super types instead.
    // - new implementation must have the same number of parameters no matter if they are optional or not.
    // - The return type must be the same type or a subtype
    public HaxeOverrideOrImplementEvaluation evaluateOverride() {
        List<HaxeParameterModel> sourceParameters = sourceModel.getParameters();
        List<HaxeParameterModel> targetParameters = targetModel.getParameters();

        HaxeGenericResolver genericResolver = createOverrideResolver(sourceModel);

        checkParameterCount(sourceParameters, targetParameters);
        checkParameterTypes(sourceParameters, targetParameters, genericResolver);
        checkOptionalTag(sourceParameters, targetParameters, genericResolver, true);
        checkReturnTypes(genericResolver);

        complete = true;
        return this;
    }

    // When implementing an interface:
    // - The implementing method must have parameter types that matches the interface or
    // are super types of the interface types, abstracts that can cast to these types are also allowed.
    //
    // - The return type must be the same type or a subtype
    //
    // Implementation and interface must have the same number of parameters no matter if they are optional or not.
    //
    public HaxeOverrideOrImplementEvaluation evaluateImplementation() {
        List<HaxeParameterModel> sourceParameters = sourceModel.getParameters();
        List<HaxeParameterModel> targetParameters = targetModel.getParameters();

        HaxeGenericResolver resolver = createImplementResolver(sourceModel);

        checkParameterCount(sourceParameters, targetParameters);
        checkParameterTypes(sourceParameters, targetParameters, resolver);
        checkOptionalTag(sourceParameters, targetParameters, resolver, false);
        checkReturnTypes(resolver);

        complete = true;
        return this;
    }

    private @NotNull HaxeGenericResolver createOverrideResolver(@NotNull HaxeMethodModel methodModel) {
        HaxeGenericResolver resolver = new HaxeGenericResolver();
        HaxeClassModel declaringClass = methodModel.getDeclaringClass();
        if (declaringClass != null) {
            declaringClass.getExtendingTypes().stream()
                    .map(HaxeClassReferenceModel::getSpecificHaxeClassReference)
                    .filter(Objects::nonNull)
                    .forEach(model -> resolver.addAll(model.getGenericResolver()));

        }
        HaxeGenericResolver methodResolver = methodModel.getGenericResolver(declaringClass.getGenericResolver(null));
        resolver.addAll(methodResolver);

        return resolver;
    }
    private @NotNull HaxeGenericResolver createImplementResolver(@NotNull HaxeMethodModel methodModel) {
        HaxeGenericResolver resolver = new HaxeGenericResolver();
        HaxeClassModel declaringClass = methodModel.getDeclaringClass();
        if (declaringClass != null) {
            declaringClass.getImplementingInterfaces().stream()
                    .map(HaxeClassReferenceModel::getSpecificHaxeClassReference)
                    .filter(Objects::nonNull)
                    .forEach(model -> resolver.addAll(model.getGenericResolver()));

        }
        HaxeGenericResolver methodResolver = methodModel.getGenericResolver(declaringClass.getGenericResolver(null));
        resolver.addAll(methodResolver);

        return resolver;
    }

    private void checkReturnTypes(HaxeGenericResolver resolver) {
        ResultHolder sourceReturnType = sourceModel.getReturnType(resolver);
        ResultHolder targetReturnType = targetModel.getReturnType(resolver);

        if (!canAssignToFromReference(targetReturnType, sourceReturnType)) {
            if(makeAnnotations) {
                PsiElement returnTypeTag = sourceModel.getReturnTypeTagOrNameOrBasePsi();
                if (!targetReturnType.isUnknown()) {
                    createReturnTypeMismatchAnnotation(sourceReturnType, targetReturnType, returnTypeTag);
                } else {
                    if (targetReturnType.getType() instanceof SpecificHaxeClassReference classReference) {
                        if (classReference.getHaxeClassModel() == null) {
                            annotations.add(new Annotation(HighlightSeverity.WEAK_WARNING, HaxeBundle.message("haxe.unresolved.type"), returnTypeTag.getTextRange(), null));
                        }
                    }
                }
            }
            finish(false);
        }
    }

    private void checkOptionalTag(List<HaxeParameterModel> sourceParameters, List<HaxeParameterModel> targetParameters, HaxeGenericResolver targetResolver, boolean isOverride) {
        int sourceParameterCount = sourceParameters.size();
        int targetParameterCount = targetParameters.size();

        int minParameters = Math.min(sourceParameterCount, targetParameterCount);

        if (minParameters > 0) {

            HaxeClassModel declaringClass = targetModel.getDeclaringClass();

            for (int n = 0; n < minParameters; n++) {
                final HaxeParameterModel sourceParam = sourceParameters.get(n);
                final HaxeParameterModel targetParam = targetParameters.get(n);

                if (sourceParam.isOptional() != targetParam.isOptional()) {
                    result = false;
                    if (makeAnnotations) {
                        if (declaringClass != null) {
                            final boolean removeOptional = sourceParam.hasOptionalPsi();
                            String targetParamText = targetParam.getPresentableText(targetResolver);
                            if (removeOptional) {
                                createRemoveOptionalAnnotation(targetParamText, declaringClass, sourceParam, isOverride);
                            } else {
                                createAddOptionalTagAnnotation(targetParamText, declaringClass, sourceParam, isOverride);
                            }
                        }
                    }
                }
            }
        }
    }


    private void checkParameterTypesInterface(List<HaxeParameterModel> sourceParameters, List<HaxeParameterModel> targetParameters, HaxeGenericResolver resolver) {


        final HaxeDocumentModel document = sourceModel.getDocument();

        int sourceParameterCount = sourceParameters.size();
        int targetParameterCount = targetParameters.size();
        int minParameters = Math.min(sourceParameterCount, targetParameterCount);

        for (int n = 0; n < minParameters; n++) {
            final HaxeParameterModel sourceParam = sourceParameters.get(n);
            final HaxeParameterModel targetParam = targetParameters.get(n);

            ResultHolder sourceParamType = sourceParam.getType(resolver);
            ResultHolder targetParamType = targetParam.getType(resolver);

            if (!canAssignToFromReference(targetParamType, sourceParamType)) {
                if (makeAnnotations) {
                    String message = HaxeBundle.message("haxe.semantic.incompatible.type.0.should.be.1", sourceParamType, targetParamType);
                    HaxeFixer fix = createReplaceTypeFix(document, sourceParam, targetParam);
                    TextRange textRange = sourceParam.getBasePsi().getTextRange();
                    annotations.add(new Annotation(HighlightSeverity.ERROR, message, textRange, fix));
                }
                finish(false);
            }
        }
    }

    private void checkParameterTypes(List<HaxeParameterModel> sourceParameters, List<HaxeParameterModel> targetParameters, HaxeGenericResolver resolver) {

        final HaxeDocumentModel document = sourceModel.getDocument();

        int sourceParameterCount = sourceParameters.size();
        int targetParameterCount = targetParameters.size();
        int minParameters = Math.min(sourceParameterCount, targetParameterCount);

        for (int n = 0; n < minParameters; n++) {
            final HaxeParameterModel sourceParam = sourceParameters.get(n);
            final HaxeParameterModel targetParam = targetParameters.get(n);

            ResultHolder sourceParamType = sourceParam.getType(resolver);
            ResultHolder targetParamType = targetParam.getType(resolver);

            //
            if (!canAssignToFromContravariance(targetParamType,sourceParamType)) {
                if (makeAnnotations) {
                    String message = HaxeBundle.message("haxe.semantic.incompatible.type.0.should.be.1", sourceParamType, targetParamType);
                    HaxeFixer fix = createReplaceTypeFix(document, sourceParam, targetParam);
                    TextRange textRange = sourceParam.getBasePsi().getTextRange();
                    annotations.add(new Annotation(HighlightSeverity.ERROR, message, textRange, fix));
                }
                finish(false);
            }
        }
    }

    private static @NotNull HaxeFixer createReplaceTypeFix(HaxeDocumentModel document, HaxeParameterModel sourceParam, HaxeParameterModel targetParam) {
        return HaxeFixer.create(HaxeBundle.message("haxe.semantic.change.type"), () -> {
            document.replaceElementText(sourceParam.getTypeTagPsi(), targetParam.getTypeTagPsi().getText());
        });
    }

    private HaxeOverrideOrImplementEvaluation checkParameterCount(List<HaxeParameterModel> sourceParameters,
                                                                            List<HaxeParameterModel> targetParameters) {
        int sourceParameterCount = sourceParameters.size();
        int targetParameterCount = targetParameters.size();

        int minParameters = Math.min(sourceParameterCount, targetParameterCount);

        if (sourceParameterCount > targetParameterCount) {
            if (makeAnnotations) {
                // TODO add errors for all missing
                for (int n = minParameters; n < sourceParameterCount; n++) {
                    final HaxeParameterModel currentParam = sourceParameters.get(n);
                    HaxeFixer fixer = removeArgumentFix(currentParam);
                    TextRange textRange = currentParam.getBasePsi().getTextRange();
                    annotations.add(new Annotation(HighlightSeverity.ERROR, "Unexpected argument", textRange, fixer));
                }
            }
            return finish(false);
        } else if (sourceParameterCount < targetParameterCount) {
            if (makeAnnotations) {
                String message = "Not matching arity expected (expected " + targetParameterCount + " argument(s) but found " + sourceParameterCount+ ")";
                TextRange textRange = sourceModel.getNameOrBasePsi().getTextRange();
                annotations.add(new Annotation(HighlightSeverity.ERROR, message, textRange, null));
            }
            return finish(false);
        }
        return this;
    }


    private static @NotNull HaxeFixer removeArgumentFix(HaxeParameterModel currentParam) {
        return new HaxeFixer("Remove argument") {
            @Override
            public void run() {
                currentParam.remove();
            }
        };
    }

    public void annotate(AnnotationHolder annotationHolder) {
        annotations.forEach(annotation -> annotation.create(annotationHolder));
    }

    private HaxeOverrideOrImplementEvaluation finish(boolean valid) {
        result = valid;
        complete = true;
        return this;
    }

    private void createReturnTypeMismatchAnnotation(ResultHolder sourceReturnType, ResultHolder targetReturnType, PsiElement returnTypeTag) {
        String sourceTypeText = sourceReturnType.toStringWithoutConstant();
        String targetTypeText = targetReturnType.toStringWithoutConstant();
        String message = HaxeBundle.message("haxe.semantic.incompatible.return.type.0.should.be.1", sourceTypeText, targetTypeText);

        final HaxeDocumentModel document = sourceModel.getDocument();

        HaxeFixer.create(HaxeBundle.message("haxe.semantic.change.type"), () -> {
            document.replaceElementText(sourceReturnType.getElementContext(), targetReturnType.toStringWithoutConstant());
        });

        annotations.add(new Annotation(HighlightSeverity.ERROR, message, returnTypeTag.getTextRange(), null));
    }

    private void createAddOptionalTagAnnotation(String targetParamText, HaxeClassModel declaringClass, HaxeParameterModel sourceParam, boolean isOverride) {
        final HaxeDocumentModel document = sourceModel.getDocument();
        String memberQname = declaringClass.getName() + "." + targetModel.getName();
        String errorMessage = HaxeBundle.message( isOverride
                        ? "haxe.semantic.overwritten.method.parameter.optional"
                        : "haxe.semantic.implemented.method.parameter.optional",
                targetParamText,
                memberQname);

        String fixMessage = HaxeBundle.message("haxe.semantic.method.parameter.optional.add");
        HaxeFixer addOptionalTag = new HaxeFixer(fixMessage) {
            @Override
            public void run() {
                PsiElement element = sourceParam.getBasePsi();
                document.addTextBeforeElement(element.getFirstChild(), "?");

            }
        };

        annotations.add(new Annotation(HighlightSeverity.ERROR, errorMessage, sourceParam.getBasePsi().getTextRange(), addOptionalTag));
    }

    private void createRemoveOptionalAnnotation(String targetParamText, HaxeClassModel declaringClass, HaxeParameterModel sourceParam, boolean isOverride) {
        String memberQname = declaringClass.getName() + "." + targetModel.getName();

        String errorMessage = HaxeBundle.message(isOverride
                        ? "haxe.semantic.overwritten.method.parameter.required"
                        : "haxe.semantic.implemented.method.parameter.required",
                targetParamText,
                memberQname);

        String fixMessage = HaxeBundle.message("haxe.semantic.method.parameter.optional.remove");
        HaxeFixer removeOptionalTag = new HaxeFixer(fixMessage) {
            @Override
            public void run() {
                sourceParam.getOptionalPsi().delete();
            }
        };

        annotations.add(new Annotation(HighlightSeverity.ERROR, errorMessage, sourceParam.getBasePsi().getTextRange(), removeOptionalTag));
    }
}

record Annotation(@NotNull HighlightSeverity severity, @NotNull String message, @NotNull TextRange textRange,
                  @Nullable IntentionAction fix) {

    void create(AnnotationHolder annotationHolder) {
        AnnotationBuilder builder = annotationHolder.newAnnotation(severity, message).range(textRange);
        if (fix != null) {
            builder.withFix(fix).create();
        } else {
            builder.create();
        }
    }
}