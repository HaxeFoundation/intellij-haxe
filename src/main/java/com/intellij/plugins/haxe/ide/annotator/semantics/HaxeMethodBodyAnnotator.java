package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.plugins.haxe.ide.annotator.HaxeSemanticAnnotatorConfig;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.model.HaxeGenericParamModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.plugins.haxe.model.type.resolver.ResolveSource;
import com.intellij.psi.PsiElement;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

@CustomLog
public class HaxeMethodBodyAnnotator implements Annotator {
  /**
   * Very slow annotator that evaluates all parts of a method block to catches code that sets variables type parameters and annotate
   * if any other part of the code tries to reassign or use a diffrent type parameter
   *
   * @param element to annotate.
   * @param holder  the container which receives annotations created by the plugin.
   */
  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (element instanceof HaxeMethod haxeMethod) {
      if (HaxeSemanticAnnotatorConfig.ENABLE_EXPERIMENTAL_BODY_CHECK) {
        checkBody(haxeMethod, holder);
      }
    }
  }


  public static void checkBody(HaxeMethod psi, AnnotationHolder holder) {
    final HaxeMethodModel method = psi.getModel();
    // Note: getPsiElementType runs a number of checks while determining the type.
    HaxeTypeResolver.getPsiElementType(method.getBodyPsi(), holder, generateConstraintResolver(method));
  }

  @NotNull
  private static HaxeGenericResolver generateConstraintResolver(HaxeMethodModel method) {
    HaxeGenericResolver resolver = new HaxeGenericResolver();
    for (HaxeGenericParamModel param : method.getGenericParams()) {
      ResultHolder constraint = param.getConstraint(resolver);
      if (null == constraint) {
        constraint = new ResultHolder(SpecificHaxeClassReference.getDynamic(param.getPsi()));
      }
      resolver.addConstraint(param.getName(), constraint, ResolveSource.METHOD_TYPE_PARAMETER);
    }
    return resolver;
  }
}