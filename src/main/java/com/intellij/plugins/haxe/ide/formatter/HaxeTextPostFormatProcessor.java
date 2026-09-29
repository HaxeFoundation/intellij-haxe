package com.intellij.plugins.haxe.ide.formatter;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.SyntaxTraverser;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A post-format pass that rewrites the text of selected tokens after block
 * formatting. The block formatter only manages the whitespace between
 * tokens, so the text inside a token (a comment's interior, an inactive
 * branch's lines) needs a pass of its own.
 * <p>
 * A subclass names its setting, the tokens it handles and the edits it
 * wants. The base class checks the file type and the setting, collects the
 * tokens in file order and applies the edits to the document. A whole file
 * is processed as it is. Any other element, such as a declaration an
 * intention just inserted, is processed as its range in the containing file
 * and is looked up again after the edits.
 */
public abstract class HaxeTextPostFormatProcessor implements PostFormatProcessor {

  @Override
  public final @NotNull PsiElement processElement(@NotNull PsiElement source, @NotNull CodeStyleSettings settings) {
    if (source instanceof HaxeFile file) {
      processText(file, file.getTextRange(), settings);
      return source;
    }
    if (!(source.getContainingFile() instanceof HaxeFile file)) return source;

    SmartPsiElementPointer<PsiElement> pointer = SmartPointerManager.createPointer(source);
    processText(file, source.getTextRange(), settings);
    PsiElement edited = pointer.getElement();
    return edited != null ? edited : source;
  }

  @Override
  public final @NotNull TextRange processText(@NotNull PsiFile source, @NotNull TextRange rangeToReformat, @NotNull CodeStyleSettings settings) {
    if (!(source instanceof HaxeFile)) return rangeToReformat;
    HaxeCodeStyleSettings haxeSettings = settings.getCustomSettings(HaxeCodeStyleSettings.class);
    if (!enabled(haxeSettings)) return rangeToReformat;
    Document document = source.getViewProvider().getDocument();
    if (document == null) return rangeToReformat;

    List<ASTNode> nodes = SyntaxTraverser.astTraverser(source.getNode())
      .filter(this::handles)
      .toList();
    if (nodes.isEmpty()) return rangeToReformat;

    Pass pass = new Pass(document.getText(), rangeToReformat, settings, haxeSettings);
    List<Replacement> replacements = replacements(nodes, pass);
    if (replacements.isEmpty()) return rangeToReformat;

    // The edits apply from the last to the first, so the offsets of the edits
    // still to come stay valid. Replacing only the touched tokens, rather than
    // the whole text, keeps range markers, folding and undo intact elsewhere.
    int shift = 0;
    for (Replacement replacement : replacements.reversed()) {
      document.replaceString(replacement.start(), replacement.start() + replacement.length(), replacement.text());
      shift += replacement.text().length() - replacement.length();
    }
    PsiDocumentManager.getInstance(source.getProject()).commitDocument(document);
    int start = rangeToReformat.getStartOffset();
    int end = Math.min(rangeToReformat.getEndOffset() + shift, document.getTextLength());
    return new TextRange(start, Math.max(start, end));
  }

  /** Whether the pass is switched on. */
  protected abstract boolean enabled(@NotNull HaxeCodeStyleSettings settings);

  /** Whether the pass may rewrite this node's text. */
  protected abstract boolean handles(@NotNull ASTNode node);

  /**
   * The edits that bring the handled nodes into shape. The nodes arrive in
   * file order; the edits must be in file order too, with offsets in the
   * original document. Empty when nothing needs to change.
   */
  @NotNull
  protected abstract List<Replacement> replacements(@NotNull List<ASTNode> nodes, @NotNull Pass pass);

  /** The inputs of one run: the document text before any edit, the range being reformatted and the settings in force. */
  protected record Pass(@NotNull String text,
                        @NotNull TextRange range,
                        @NotNull CodeStyleSettings settings,
                        @NotNull HaxeCodeStyleSettings haxeSettings) {

    CommonCodeStyleSettings.IndentOptions indentOptions() {
      return settings.getIndentOptions(HaxeFileType.INSTANCE);
    }

    /** Whether the node lies at least partly inside the range being reformatted. */
    boolean covers(@NotNull ASTNode node) {
      return range.intersects(node.getTextRange());
    }

    /** Whether the node lies in the range and outside every inactive branch that stays as written. */
    boolean editable(@NotNull ASTNode node) {
      return covers(node) && !HaxeInactiveBranches.isInsidePreservedBranch(node, haxeSettings);
    }
  }

  /** A document edit, with its offset in the original (pre-edit) document. */
  protected record Replacement(int start, int length, @NotNull String text) {

    /** The edit that swaps the node's whole text for {@code text}. */
    Replacement(@NotNull ASTNode node, @NotNull String text) {
      this(node.getStartOffset(), node.getTextLength(), text);
    }
  }
}
