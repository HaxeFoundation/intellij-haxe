package com.intellij.plugins.haxe.ide.hint.types;

import com.intellij.codeInsight.hints.declarative.PresentationTreeBuilder;
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificEnumValueReference;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;

public abstract class HaxeSharedBypassCollector implements SharedBypassCollector {

  @NotNull
  protected static Function1<PresentationTreeBuilder, Unit> appendTypeTextToBuilder(ResultHolder type) {
    return builder -> {
      builder.text(":" + getPresentationText(type), null);
      return null;
    };
  }


  protected static String getPresentationText(ResultHolder returnType) {
    // we dont want to show  enumValues as type info in inlays as its not an assignable type.
    if (returnType.getType() instanceof  SpecificEnumValueReference enumValueReference) {
      return enumValueReference.getEnumClass().toPresentationString(true);
    }else {
      return returnType.getType().toPresentationString(true);
    }
  }

}
