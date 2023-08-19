package com.intellij.plugins.haxe.ide.hint.types;

import com.intellij.codeInsight.hints.*;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.ScaleAwarePresentationFactory;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;

public abstract class HaxeInlayHintsFactory extends FactoryInlayHintsCollector {

  ScaleAwarePresentationFactory PresentationFoactory;

  public HaxeInlayHintsFactory(@NotNull Editor editor) {
    super(editor);
    PresentationFoactory = new ScaleAwarePresentationFactory(editor, getFactory());
  }

  protected void addInsert(int offset, InlayHintsSink sink, String text) {
    InlayPresentation inlayPresentation =  PresentationFoactory.lineCentered(getFactory().text(text));
    sink.addInlineElement(offset, true, getFactory().textSpacePlaceholder(1, false), false);
    sink.addInlineElement(offset, true, inlayPresentation, false);
  }


}
