package com.intellij.plugins.haxe.ide.hint.types;

import com.intellij.codeInsight.hints.declarative.*;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class HaxeSharedBypassCollector implements SharedBypassCollector {

  @NotNull
  protected static Function1<PresentationTreeBuilder, Unit> appendTypeTextToBuilder(ResultHolder type) {
    return builder -> {
      InlayActionData inlayActionData = createInlayActionData(type);
      builder.text(":" + getPresentationText(type), inlayActionData);
      return null;
    };
  }

    private static @Nullable InlayActionData createInlayActionData(ResultHolder type) {
        if(type.isUnknown()) return null;
        SpecificTypeReference typeReference = type.getType();
        PsiElement element = typeReference.getTypePsi();
        if(element != null) {
            return new InlayActionData(new PsiPointerInlayActionPayload(createSmartPointer(element)), PsiPointerInlayActionNavigationHandler.HANDLER_ID);
        }
        return null;
    }

  private static @NotNull SmartPsiElementPointer<PsiElement> createSmartPointer(PsiElement context) {
    return SmartPointerManager.getInstance(context.getProject()).createSmartPsiElementPointer(context);
  }


  protected static String getPresentationText(ResultHolder returnType) {
    // we dont want to show  enumValues as type info in inlays as its not an assignable type.
    if (returnType.getType() instanceof  SpecificEnumValueReference enumValueReference) {
      return enumValueReference.getEnumClass().toPresentationString();
    }else {
      return returnType.getType().toPresentationString();
    }
  }

}
