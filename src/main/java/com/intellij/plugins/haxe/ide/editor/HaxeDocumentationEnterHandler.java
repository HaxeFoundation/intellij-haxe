package com.intellij.plugins.haxe.ide.editor;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.util.Ref;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.ide.HaxeCommenter;
import com.intellij.plugins.haxe.lang.parser.HaxePsiDocCommentImpl;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;

import static com.intellij.plugins.haxe.ide.HaxeCommenter.DOC_COMMENT_PREFIX;

/// Custom logic to correctly handle completion of haxe documentation block (the default javadoc stuff does not work correctly for haxedocs)
/// if block is incomplete then this handler will add the end "tag" and make sure we get correct indentation
public class HaxeDocumentationEnterHandler extends EnterHandlerDelegateAdapter {

    @Override
    public Result preprocessEnter(
            @NotNull PsiFile file,
            @NotNull Editor editor,
            @NotNull Ref<Integer> caretOffsetRef,
            @NotNull Ref<Integer> caretAdvance,
            @NotNull DataContext dataContext,
            EditorActionHandler originalHandler
    ) {
        if (file instanceof HaxeFile) {
            int caretOffset = caretOffsetRef.get();

            if (isInsideDocsWithoutCloseTag(file, caretOffset)) {

                Document document = editor.getDocument();

                int lineNumber = document.getLineNumber(caretOffset);
                int lineStart = document.getLineStartOffset(lineNumber);

                // TODO mlo: make use of codeStyle line indent instead of manual workaround.
                // probably need to either create a LineIndentProvider or change the formatter logic
                // so  FormatterBasedLineIndentProvider can handle the indentation.
                //String indent = CodeStyleManager.getInstance(file.getProject()).getLineIndent(file, caretOffset);

                String text = document.getText();
                String lineText = text.substring(lineStart, caretOffset);

                if(!lineText.trim().endsWith(DOC_COMMENT_PREFIX)){
                    return Result.Continue;
                }
                String indent = getLineIndent(lineText);

                CodeStyleSettings settings = CodeStyle.getSettings(file.getProject());
                CodeStyleSettings.IndentOptions indentOptions = settings.getIndentOptions(HaxeFileType.INSTANCE);

                String docIndent = indentOptions.USE_TAB_CHARACTER ? "\t" : "   ";
                String docNewLine = indent + docIndent;

                String toInsert = "\n" + docNewLine + "\n" + indent + HaxeCommenter.DOC_COMMENT_SUFFIX;
                document.insertString(caretOffset, toInsert);

                editor.getCaretModel().moveToOffset(caretOffset + 1 + docNewLine.length());

                return Result.Stop;
            }
        }
        return Result.Continue;

    }


    private static boolean isInsideDocsWithoutCloseTag(@NotNull PsiFile file, int caretOffset) {
        PsiElement elementAtOffset = PsiUtilCore.getElementAtOffset(file, caretOffset);
        if (elementAtOffset instanceof HaxePsiDocCommentImpl docComment) {
            String text = docComment.getText();
            if (caretOffset < elementAtOffset.getTextOffset() + DOC_COMMENT_PREFIX.length()) {
                return false;
            }
            if(text.endsWith(HaxeCommenter.BLOCK_COMMENT_SUFFIX) || text.endsWith(HaxeCommenter.DOC_COMMENT_SUFFIX)) {
                return false;
            }
            return true;
        }
        return false;
    }


    /**
     * Temp solution to find line indentation (should probably be handled by formatter)
     */
    @NotNull
    private static String getLineIndent(@NotNull String lineText) {
        int indentLength = lineText.stripLeading().length();
        return lineText.substring(0, lineText.length() - indentLength);
    }
}

