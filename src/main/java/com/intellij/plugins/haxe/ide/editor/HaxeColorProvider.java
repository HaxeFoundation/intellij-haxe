package com.intellij.plugins.haxe.ide.editor;

import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ElementColorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxePsiToken;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.regex.Pattern;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.LITHEX;

/** Colour swatches for six-digit hex literals; the marker sits on the literal token, as line markers must. */
public class HaxeColorProvider implements ElementColorProvider {

  // a six-digit hex literal (0xRRGGBB) in either case; shorter literals and ones carrying alpha are not colors
  private static final Pattern COLOR_LITERAL = Pattern.compile("^0x[0-9a-f]{6}$", Pattern.CASE_INSENSITIVE);
  // The colour picker keeps the element it was opened on and hands it to every
  // pick, but replacing a leaf's text replaces the leaf, so from the second pick
  // on that element is dead. A pointer created on the first pick and stored on
  // it follows the literal across replacements.
  private static final Key<SmartPsiElementPointer<PsiElement>> LIVE_LITERAL = Key.create("haxe.color.live.literal");

  @Override
  public @Nullable Color getColorFrom(@NotNull PsiElement element) {
    if (!isColorLiteral(element)) return null;
    return new Color(Integer.parseInt(element.getText().substring(2), 16));
  }

  @Override
  public void setColorTo(@NotNull PsiElement element, @NotNull Color color) {
    PsiElement literal = liveLiteral(element);
    if (!(literal instanceof LeafPsiElement leaf)) return;
    Project project = leaf.getProject();
    String hexColor = String.format("0x%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    Document document = PsiDocumentManager.getInstance(project).getDocument(leaf.getContainingFile());

    Runnable command = () -> leaf.replaceWithText(hexColor);
    CommandProcessor.getInstance()
      .executeCommand(project, command, HaxeBundle.message("haxe.editor.change.color.command"), null, document);
  }

  /** The literal as it is now, reached through the pointer the first pick left on the picker's element. */
  @Nullable
  private static PsiElement liveLiteral(@NotNull PsiElement element) {
    SmartPsiElementPointer<PsiElement> pointer = element.getUserData(LIVE_LITERAL);
    if (pointer == null) {
      if (!element.isValid()) return null;
      pointer = SmartPointerManager.createPointer(element);
      element.putUserData(LIVE_LITERAL, pointer);
    }
    PsiElement live = pointer.getElement();
    return live != null && isColorLiteral(live) ? live : null;
  }

  private static boolean isColorLiteral(@NotNull PsiElement element) {
    return element instanceof HaxePsiToken token
           && token.getTokenType() == LITHEX
           && COLOR_LITERAL.matcher(token.getText()).matches();
  }
}
