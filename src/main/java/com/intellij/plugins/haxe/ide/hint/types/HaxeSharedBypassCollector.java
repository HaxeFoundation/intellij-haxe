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

import java.util.List;

public abstract class HaxeSharedBypassCollector implements SharedBypassCollector {

  @NotNull
  protected static Function1<PresentationTreeBuilder, Unit> appendTypeTextToBuilder(ResultHolder type) {
    return builder -> {
        createClickableInlayText(type, builder);
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



    private static void createClickableInlayText(ResultHolder type, PresentationTreeBuilder builder) {
        builder.text(":", null);
        buildTypeTexts(type, builder);
    }

    private static void buildTypeTexts(ResultHolder type, PresentationTreeBuilder builder) {
        if(type.getType() instanceof  SpecificHaxeClassReference classReference) {
            String className = classReference.getHaxeClassReference().getName();
            if(className != null) {
                InlayActionData inlayActionData = createInlayActionData(type);
                builder.text(className, inlayActionData);
            }
            // hide Unknown generics if Dynamic
            if(type.isDynamic() && type.containsUnknownTypeParameters()) return;;

            @NotNull ResultHolder[] specifics = classReference.getSpecifics();
            if(specifics.length > 0 ) {
                builder.text("<", null);
                for (int i = 0; i < specifics.length; i++) {
                    ResultHolder specific = specifics[i];
                    buildTypeTexts(specific, builder);
                    if(i+1 < specifics.length) {
                        builder.text(", ", null);
                    }
                }
                builder.text(">", null);
            }
            return;
        } else if(type.getType() instanceof  SpecificEnumValueReference enumValueReference) {
            buildTypeTexts(enumValueReference.getEnumClass().createHolder(), builder);
        } else if(type.getType() instanceof  SpecificFunctionReference functionReference) {
            List<HaxeArgument> arguments = functionReference.getArguments();
            builder.text("(", null);
            int argSize = arguments.size();
            for (int i = 0; i < argSize; i++) {
                HaxeArgument argument = arguments.get(i);
                ResultHolder argumentType = argument.getType();
                buildTypeTexts(argumentType, builder);
                if (i + 1 < argSize) {
                    builder.text(", ", null);
                }
            }
            builder.text(")", null);
            ResultHolder returnType = functionReference.getReturnType();
            builder.text("->", null);
            buildTypeTexts(returnType, builder);
        }else {
            InlayActionData inlayActionData = createInlayActionData(type);
            builder.text(type.toPresentationString(), inlayActionData);
        }
    }
}
