package com.intellij.plugins.haxe.ide.editor;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.ide.documentation.settings.HaxeDocSettings;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.plugins.haxe.lang.parser.HaxePsiDocCommentImpl;
import com.intellij.plugins.haxe.lang.psi.HaxeEnumValueDeclarationConstructor;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.intellij.plugins.haxe.ide.HaxeCommenter.DOC_COMMENT_PREFIX;
import static com.intellij.plugins.haxe.ide.HaxeCommenter.DOC_COMMENT_SUFFIX;
import static com.intellij.plugins.haxe.ide.documentation.HaxeDocTags.TAG_PARAM;
import static com.intellij.plugins.haxe.ide.documentation.HaxeDocTags.TAG_RETURN;
import static com.intellij.plugins.haxe.lang.lexer.HaxeDocTokenTypes.DOC_END;
import static com.intellij.plugins.haxe.lang.lexer.HaxeDocTokenTypes.DOC_START;
import static com.intellij.plugins.haxe.util.HaxeDocumentationUtil.leadingWhitespace;

/// Enter inside a Haxe doc comment, in either of its two styles: haxedoc, with bare lines and usually closed by
/// `**/`, and javadoc, with a `*` opening each line and closed by `*/`. A comment is javadoc style when every line
/// between its opener's line and its closer's line starts with a star.
///
/// Enter right after the opener of a comment that is not closed yet adds the closer and, for a method, the tag stub,
/// in the style the code style picks for new comments. Inside a closed comment the new line follows the comment's
/// own style, so it gets a star exactly when the comment uses them; a comment without lines of its own yet takes the
/// code style's pick.
public class HaxeDocumentationEnterHandler extends EnterHandlerDelegateAdapter {

    // a javadoc-style line: its indent, the star, and the spacing before the text
    private static final Pattern STAR_LINE = Pattern.compile("^(?<indent>[ \\t]*)\\*(?<spacing>[ \\t]*)");

    @Override
    public Result preprocessEnter(
            @NotNull PsiFile file,
            @NotNull Editor editor,
            @NotNull Ref<Integer> caretOffsetRef,
            @NotNull Ref<Integer> caretAdvance,
            @NotNull DataContext dataContext,
            EditorActionHandler originalHandler
    ) {
        if (!(file instanceof HaxeFile)) return Result.Continue;
        int caretOffset = caretOffsetRef.get();
        Document document = editor.getDocument();
        PsiDocumentManager.getInstance(file.getProject()).commitDocument(document);
        HaxePsiDocCommentImpl docComment =
            PsiTreeUtil.getParentOfType(PsiUtilCore.getElementAtOffset(file, caretOffset), HaxePsiDocCommentImpl.class, false);
        if (docComment == null || caretOffset < bodyStart(docComment)) return Result.Continue;

        if (isUnclosed(docComment)) {
            if (!isRightAfterOpener(document, caretOffset)) return Result.Continue;
            completeNewComment(file, editor, document, caretOffset);
            return Result.Stop;
        }
        if (caretOffset > closerStart(docComment)) return Result.Continue;
        insertBodyLine(file, editor, document, docComment, caretOffset);
        return Result.Stop;
    }

    /**
     * A new line inside a doc comment continues at the PREVIOUS line's
     * indentation, preserving the hand-aligned haxedoc layout (tag
     * description columns) instead of falling back to the body indent.
     */
    @Override
    public Result postProcessEnter(@NotNull PsiFile file, @NotNull Editor editor, @NotNull DataContext dataContext) {
        if (!(file instanceof HaxeFile)) return Result.Continue;
        if (!HaxeDocSettings.getInstance().getState().enterKeepsIndentation) return Result.Continue;
        Document document = editor.getDocument();
        int offset = editor.getCaretModel().getOffset();
        int line = document.getLineNumber(offset);
        if (line == 0) return Result.Continue;

        PsiDocumentManager.getInstance(file.getProject()).commitDocument(document);
        HaxePsiDocCommentImpl docComment =
            PsiTreeUtil.getParentOfType(file.findElementAt(offset), HaxePsiDocCommentImpl.class, false);
        if (docComment == null) return Result.Continue;
        int commentLine = document.getLineNumber(docComment.getTextRange().getStartOffset());
        if (line <= commentLine) return Result.Continue;

        CharSequence text = document.getCharsSequence();
        int previousLineStart = document.getLineStartOffset(line - 1);
        int previousIndentEnd = CharArrayUtil.shiftForward(text, previousLineStart, document.getLineEndOffset(line - 1), " \t");
        String previousIndent = text.subSequence(previousLineStart, previousIndentEnd).toString();
        int lineStart = document.getLineStartOffset(line);
        int indentEnd = CharArrayUtil.shiftForward(text, lineStart, document.getLineEndOffset(line), " \t");
        // only replace leading whitespace the enter produced - never text the caret sits past
        if (offset > indentEnd) return Result.Continue;

        document.replaceString(lineStart, indentEnd, previousIndent);
        editor.getCaretModel().moveToOffset(lineStart + previousIndent.length());
        return Result.Continue;
    }

    /** Adds the closer below the opener, with an empty body line for the caret and the tag stub under it. */
    private static void completeNewComment(@NotNull PsiFile file, @NotNull Editor editor, @NotNull Document document, int caretOffset) {
        String indent = leadingWhitespace(lineBeforeCaret(document, caretOffset));
        boolean stars = newCommentsUseStars(file);
        String bodyLineStart = stars ? indent + " * " : indent + indentUnit(file);
        String closer = stars ? indent + " */" : indent + DOC_COMMENT_SUFFIX;
        document.insertString(caretOffset, "\n" + bodyLineStart + "\n" + closer);

        int caretLineEnd = caretOffset + 1 + bodyLineStart.length();
        insertTagStub(file, document, caretLineEnd, bodyLineStart);
        editor.getCaretModel().moveToOffset(caretLineEnd);
    }

    /**
     * Breaks the line at the caret, starting the new line the way the comment's lines start. With the caret in a
     * line's leading whitespace the line moves down as it is and an empty line takes its place.
     */
    private static void insertBodyLine(@NotNull PsiFile file,
                                       @NotNull Editor editor,
                                       @NotNull Document document,
                                       @NotNull HaxePsiDocCommentImpl docComment,
                                       int caretOffset) {
        boolean stars = docComment.hasInteriorLines() ? docComment.isStarStyle() : newCommentsUseStars(file);
        int lineStart = document.getLineStartOffset(document.getLineNumber(caretOffset));
        String line = document.getText(new TextRange(lineStart, document.getLineEndOffset(document.getLineNumber(caretOffset))));
        boolean openerLine = lineStart <= docComment.getTextRange().getStartOffset();

        if (!openerLine && lineBeforeCaret(document, caretOffset).isBlank()) {
            String emptyLine = stars ? starLineIndent(line) + "*" : "";
            document.insertString(lineStart, emptyLine + "\n");
            editor.getCaretModel().moveToOffset(caretOffset + emptyLine.length() + 1);
            return;
        }

        String openerIndent = leadingWhitespace(lineAt(document, docComment.getTextRange().getStartOffset()));
        String lineStartText = stars ? starLineStart(line, openerLine, openerIndent) : bareLineStart(file, line, openerLine, openerIndent);
        int spacingEnd = CharArrayUtil.shiftForward(document.getCharsSequence(), caretOffset, " \t");
        document.replaceString(caretOffset, spacingEnd, "\n" + lineStartText);
        editor.getCaretModel().moveToOffset(caretOffset + 1 + lineStartText.length());
    }

    /** A javadoc-style line starts under the opener's first star, or the way the caret's line starts. */
    private static @NotNull String starLineStart(@NotNull String line, boolean openerLine, @NotNull String openerIndent) {
        if (openerLine) return openerIndent + " * ";
        Matcher matcher = STAR_LINE.matcher(line);
        if (!matcher.lookingAt()) return leadingWhitespace(line) + "* ";
        String spacing = matcher.group("spacing");
        return matcher.group("indent") + "*" + (spacing.isEmpty() ? " " : spacing);
    }

    private static @NotNull String starLineIndent(@NotNull String line) {
        Matcher matcher = STAR_LINE.matcher(line);
        return matcher.lookingAt() ? matcher.group("indent") : leadingWhitespace(line);
    }

    /** A haxedoc-style line sits one indent inside the comment, or keeps the caret line's indent when the doc settings say so. */
    private static @NotNull String bareLineStart(@NotNull PsiFile file, @NotNull String line, boolean openerLine, @NotNull String openerIndent) {
        if (!openerLine && HaxeDocSettings.getInstance().getState().enterKeepsIndentation) return leadingWhitespace(line);
        return openerIndent + indentUnit(file);
    }

    private static boolean newCommentsUseStars(@NotNull PsiFile file) {
        return CodeStyle.getCustomSettings(file, HaxeCodeStyleSettings.class).DOC_COMMENT_STYLE == HaxeCodeStyleSettings.DOC_COMMENT_STYLE_JAVADOC;
    }

    /** One indent level, as the formatter indents a haxedoc body line. */
    private static @NotNull String indentUnit(@NotNull PsiFile file) {
        CommonCodeStyleSettings.IndentOptions indentOptions = CodeStyle.getIndentOptions(file);
        return indentOptions.USE_TAB_CHARACTER ? "\t" : " ".repeat(indentOptions.INDENT_SIZE);
    }

    /** Adds the owner's tag lines below the caret line, the "Insert documentation comment stub" smart key permitting. */
    private static void insertTagStub(@NotNull PsiFile file, @NotNull Document document, int caretLineEnd, @NotNull String lineStart) {
        if (!CodeInsightSettings.getInstance().JAVADOC_STUB_ON_ENTER || DumbService.isDumb(file.getProject())) return;
        PsiDocumentManager.getInstance(file.getProject()).commitDocument(document);
        HaxePsiDocCommentImpl docComment =
            PsiTreeUtil.getParentOfType(file.findElementAt(caretLineEnd), HaxePsiDocCommentImpl.class, false);
        if (docComment == null) return;

        StringBuilder stub = new StringBuilder();
        for (String tag : stubTags(docComment)) {
            stub.append('\n').append(lineStart).append(tag);
        }
        document.insertString(caretLineEnd, stub);
    }

    /**
     * The tags a method's new doc comment starts with: one `@param` per parameter, and `@return` when the method
     * returns a value. Empty for other owners, and for a comment that is not the one its owner reads.
     */
    private static @NotNull List<String> stubTags(@NotNull HaxePsiDocCommentImpl docComment) {
        if (!(docComment.getOwner() instanceof HaxeMethod method) || HaxeResolveUtil.findDocumentation(method) != docComment) {
            return List.of();
        }
        List<String> tags = new ArrayList<>();
        for (HaxeParameterModel parameter : method.getModel().getParameters()) {
            tags.add(TAG_PARAM + " " + parameter.getName());
        }
        if (returnsValue(method)) {
            tags.add(TAG_RETURN);
        }
        return tags;
    }

    private static boolean returnsValue(@NotNull HaxeMethod method) {
        if (method instanceof HaxeEnumValueDeclarationConstructor || method.getModel().isConstructor()) return false;
        ResultHolder returnType = method.getModel().getReturnType(null);
        return !returnType.isVoid() && !returnType.isUnknown();
    }

    /**
     * Not closed yet: no closer at all, or the closer it ends with belongs to a later comment. Comments do not nest,
     * so an unclosed opener runs up to the next closer, taking in that comment's opener on the way.
     */
    private static boolean isUnclosed(@NotNull HaxePsiDocCommentImpl docComment) {
        if (docComment.getNode().findChildByType(DOC_END) == null) return true;
        String text = docComment.getText();
        int bodyStart = bodyStart(docComment) - docComment.getTextRange().getStartOffset();
        int bodyEnd = closerStart(docComment) - docComment.getTextRange().getStartOffset();
        return text.substring(bodyStart, bodyEnd).contains("/*");
    }

    private static boolean isRightAfterOpener(@NotNull Document document, int caretOffset) {
        return lineBeforeCaret(document, caretOffset).trim().endsWith(DOC_COMMENT_PREFIX);
    }

    private static int bodyStart(@NotNull HaxePsiDocCommentImpl docComment) {
        ASTNode opener = docComment.getNode().findChildByType(DOC_START);
        return opener != null ? opener.getTextRange().getEndOffset() : docComment.getTextRange().getStartOffset();
    }

    private static int closerStart(@NotNull HaxePsiDocCommentImpl docComment) {
        ASTNode closer = docComment.getNode().findChildByType(DOC_END);
        return closer != null ? closer.getTextRange().getStartOffset() : docComment.getTextRange().getEndOffset();
    }

    private static @NotNull String lineBeforeCaret(@NotNull Document document, int caretOffset) {
        int lineStart = document.getLineStartOffset(document.getLineNumber(caretOffset));
        return document.getText(new TextRange(lineStart, caretOffset));
    }

    private static @NotNull String lineAt(@NotNull Document document, int offset) {
        int line = document.getLineNumber(offset);
        return document.getText(new TextRange(document.getLineStartOffset(line), document.getLineEndOffset(line)));
    }
}
