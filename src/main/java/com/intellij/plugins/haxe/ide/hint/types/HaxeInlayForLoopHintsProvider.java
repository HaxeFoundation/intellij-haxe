package com.intellij.plugins.haxe.ide.hint.types;

import com.intellij.codeInsight.hints.InlayHintsCollector;
import com.intellij.codeInsight.hints.InlayHintsSink;
import com.intellij.codeInsight.hints.NoSettings;
import com.intellij.codeInsight.hints.SettingsKey;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.HaxeHintBundle;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeInlayForLoopHintsProvider extends HaxeInlayHintProvider {


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
    return HaxeHintBundle.message("haxe.for.loop.type.hint.name");
  }

  private static class InlayCollector extends HaxeInlayHintsFactory {
    public InlayCollector(@NotNull Editor editor) {
      super(editor);
    }

    @Override
    public boolean collect(@NotNull PsiElement element, @NotNull Editor editor, @NotNull InlayHintsSink sink) {
      if (element instanceof HaxeForStatement forStatement) {
        handleForEachHints(element, sink, forStatement);
        return false;
      }
      return true;
    }



    private void handleForEachHints(@NotNull PsiElement element,
                                    @NotNull InlayHintsSink sink,
                                    HaxeForStatement forStatement) {


      HaxeIterable iterable = forStatement.getIterable();
      HaxeKeyValueIterator keyValueIterator = forStatement.getKeyValueIterator();
      HaxeGenericResolver resolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(forStatement);
      if (iterable != null && keyValueIterator == null) {
        HaxeComponentName componentName = forStatement.getComponentName();
        ResultHolder type = extractIteratorElementType(HaxeTypeResolver.getPsiElementType(iterable, element, resolver));
        createInlayHint(componentName, sink, type);
      }
      if (keyValueIterator != null) {
        HaxeIteratorkey iteratorKey = keyValueIterator.getIteratorkey();
        HaxeIteratorValue iteratorValue = keyValueIterator.getIteratorValue();

        ResultHolder keyValueIteratorType = HaxeTypeResolver.getPsiElementType(iterable, element, resolver);
        ResultHolder keyType = extractKeyValueType(keyValueIteratorType, true);
        ResultHolder valueType = extractKeyValueType(keyValueIteratorType, false);

        createInlayHint(iteratorKey.getComponentName(), sink, keyType);
        createInlayHint(iteratorValue.getComponentName(), sink, valueType);
      }
    }

    private static ResultHolder extractKeyValueType(ResultHolder type, boolean key) {
      if (!type.isUnknown() && type.getClassType() != null) {
        @NotNull ResultHolder[] specifics = type.getClassType().getSpecifics();
        if (specifics.length == 2) {
          return specifics[key ? 0 : 1];
        }
      }
      return SpecificHaxeClassReference.getUnknown(type.getOrigin()).createHolder();
    }

    private static ResultHolder extractIteratorElementType(ResultHolder type) {
      if (!type.isUnknown() && type.getClassType() != null) {
        @NotNull ResultHolder[] specifics = type.getClassType().getSpecifics();
        if (specifics.length == 1) {
          return specifics[0];
        }
      }
      return type;
    }

    private void createInlayHint(@NotNull HaxeComponentName componentName, @NotNull InlayHintsSink sink, ResultHolder type) {
      if (!type.isUnknown() && !type.getType().isInvalid()) {
        int offset = componentName.getTextRange().getEndOffset();

        addInsert(offset, sink ,  ":" + type.toPresentationString());
      }
    }
  }
}