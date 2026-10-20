package com.intellij.plugins.haxe.v2.display;

import com.intellij.codeInsight.hints.declarative.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.ide.hint.types.HaxeTypeHintAnchors;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.v2.display.HaxeInlayTypeText.Part;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Type inlays from the compilation server only, for the declarations the
 * IDE's own type-hint providers cover: variables ({@code case var} captures
 * among them), extracted values,
 * loop variables, untyped parameters and fields, and the return types of
 * methods and local functions. The texts come from
 * {@link HaxeCompilerInlayService}; a declaration without an answer shows
 * nothing rather than the IDE's inference. A type name in a hint navigates
 * to its declaration, found through the class indexes by the name the
 * compiler gave. Active while the Haxe Compiler settings make type inlays a
 * compiler feature, when the IDE providers stand down.
 */
public final class HaxeCompilerTypeInlayProvider implements InlayHintsProvider {

  @Override
  @Nullable
  public InlayHintsCollector createCollector(@NotNull PsiFile file, @NotNull Editor editor) {
    HaxeCompilerInlayService service = HaxeCompilerInlayService.getInstance(file.getProject());
    return service.providesInlays() ? new Collector(service) : null;
  }

  private record Collector(@NotNull HaxeCompilerInlayService service) implements SharedBypassCollector {

    @Override
    public void collectFromElement(@NotNull PsiElement element, @NotNull InlayTreeSink sink) {
      int offset = HaxeTypeHintAnchors.anchorOf(element);
      if (offset < 0 || !hinted(element)) return;
      HaxeInlayTypeText text = service.typeTextOf(element);
      if (text == null) return;
      HaxeTypeHintAnchors.addTypeHint(sink, offset, builder -> {
        for (Part part : text.parts()) {
          builder.text(part.text(), navigationTo(part, element));
        }
        return Unit.INSTANCE;
      });
    }
  }

  /** Whether the element gets a hint; as with the IDE's hints, a parameter with a default (its type comes from the default) and a case pattern's capture get none. */
  private static boolean hinted(@NotNull PsiElement element) {
    return switch (element) {
      case HaxeParameter parameter -> parameter.getVarInit() == null;
      case HaxeSwitchCaseCapture _ -> false;
      default -> true;
    };
  }

  /** The click target of a type-name part: its declaration; null for punctuation and for a name the indexes do not know. */
  @Nullable
  private static InlayActionData navigationTo(@NotNull Part part, @NotNull PsiElement context) {
    if (part.namedType() == null) return null;
    HaxeClass target = HaxeCompilerTypeConverter.findClass(part.namedType(), context);
    if (target == null) return null;
    SmartPsiElementPointer<PsiElement> pointer = SmartPointerManager.getInstance(context.getProject()).createSmartPsiElementPointer(target);
    return new InlayActionData(new PsiPointerInlayActionPayload(pointer), PsiPointerInlayActionNavigationHandler.HANDLER_ID);
  }
}
