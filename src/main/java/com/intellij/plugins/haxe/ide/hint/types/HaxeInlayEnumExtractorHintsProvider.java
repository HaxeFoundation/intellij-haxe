package com.intellij.plugins.haxe.ide.hint.types;

import com.intellij.codeInsight.hints.InlayHintsCollector;
import com.intellij.codeInsight.hints.InlayHintsSink;
import com.intellij.codeInsight.hints.NoSettings;
import com.intellij.codeInsight.hints.SettingsKey;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.HaxeHintBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeEnumArgumentExtractor;
import com.intellij.plugins.haxe.lang.psi.HaxeEnumExtractedValue;
import com.intellij.plugins.haxe.lang.psi.HaxeEnumValueDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeParameterList;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HaxeInlayEnumExtractorHintsProvider extends HaxeInlayHintProvider {


  @Nullable
  @Override
  public InlayHintsCollector getCollectorFor(@NotNull PsiFile file,
                                             @NotNull Editor editor,
                                             @NotNull NoSettings settings,
                                             @NotNull InlayHintsSink sink) {
    return new InlayCollector(editor);
  }



  @Nls(capitalization = Nls.Capitalization.Sentence)
  @NotNull
  @Override
  public String getName() {
    return HaxeHintBundle.message("haxe.enum.extractor.hint.name");
  }

  private static class InlayCollector extends HaxeInlayHintsFactory {

    public InlayCollector(@NotNull Editor editor) {
      super(editor);
    }

    @Override
    public boolean collect(@NotNull PsiElement element, @NotNull Editor editor, @NotNull InlayHintsSink sink) {
      if (element instanceof HaxeEnumArgumentExtractor extractor) {
        handleEnumArgumentExtractorHints(sink, extractor);
        return false;
      }
      return true;
    }


    private void handleEnumArgumentExtractorHints(@NotNull InlayHintsSink sink, HaxeEnumArgumentExtractor extractor) {
      PsiElement resolve = extractor.getEnumValueReference().getReferenceExpression().resolve();
      if (resolve instanceof HaxeEnumValueDeclaration enumValueDeclaration) {

        HaxeParameterList parameterList = enumValueDeclaration.getParameterList();
        if (parameterList != null) {
          List<HaxeParameterModel> parameters = MapParametersToModel(parameterList);
          @NotNull PsiElement[] children = extractor.getEnumExtractorArgumentList().getChildren();
          for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof HaxeEnumExtractedValue enumExtractedValue) {
              int offset = enumExtractedValue.getTextRange().getEndOffset();


              if (parameters.size() > i) {
                ResultHolder type = parameters.get(i).getType();
                addInsert(offset, sink ,  ":" + type.toPresentationString());
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
  }
}
