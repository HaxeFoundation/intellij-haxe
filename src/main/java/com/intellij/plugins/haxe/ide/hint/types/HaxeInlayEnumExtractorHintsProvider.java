package com.intellij.plugins.haxe.ide.hint.types;

import com.intellij.codeInsight.hints.declarative.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.lang.psi.HaxeEnumArgumentExtractor;
import com.intellij.plugins.haxe.lang.psi.HaxeEnumExtractedValue;
import com.intellij.plugins.haxe.lang.psi.HaxeEnumValueDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeParameterList;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HaxeInlayEnumExtractorHintsProvider implements InlayHintsProvider {

  @Nullable
  @Override
  public InlayHintsCollector createCollector(@NotNull PsiFile file, @NotNull Editor editor) {
    return new TypeCollector();
  }

  private static class TypeCollector implements SharedBypassCollector {

    @Override
    public void collectFromElement(@NotNull PsiElement element, @NotNull InlayTreeSink sink) {
      if (element instanceof HaxeEnumArgumentExtractor extractor) {
        handleEnumArgumentExtractorHints(sink, extractor);
      }
    }

    private static void handleEnumArgumentExtractorHints(@NotNull InlayTreeSink sink, HaxeEnumArgumentExtractor extractor) {
      PsiElement resolve = extractor.getEnumValueReference().getReferenceExpression().resolve();
      if (resolve instanceof HaxeEnumValueDeclaration enumValueDeclaration) {


        List<HaxeEnumExtractedValue> extractedValueList = extractor.getEnumExtractorArgumentList().getEnumExtractedValueList();

        HaxeParameterList parameterList = enumValueDeclaration.getParameterList();
        if (parameterList != null) {
          List<HaxeParameterModel> parameters = MapParametersToModel(parameterList);
          @NotNull PsiElement[] children = extractor.getEnumExtractorArgumentList().getChildren();
          for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof HaxeEnumExtractedValue enumExtractedValue) {
              int offset = enumExtractedValue.getTextRange().getEndOffset();

              InlineInlayPosition position = new InlineInlayPosition(offset, false, 0);
              if (parameters.size() > i) {
                ResultHolder type = HaxeExpressionEvaluator.evaluate(extractedValueList.get(i), null).result;
                sink.addPresentation(position, null, null, false, appendTypeTextToBuilder(type));
              }
            }
          }
        }
      }
    }


    @NotNull
    private static List<HaxeParameterModel> MapParametersToModel(HaxeParameterList parameterList) {
      return parameterList.getParameterList().stream().map(HaxeParameterModel::new).toList();
    }


    @NotNull
    private static Function1<PresentationTreeBuilder, Unit> appendTypeTextToBuilder(ResultHolder type) {
      return builder -> {
        builder.text(":" + type.toPresentationString(), null);
        return null;
      };
    }
  }
}
