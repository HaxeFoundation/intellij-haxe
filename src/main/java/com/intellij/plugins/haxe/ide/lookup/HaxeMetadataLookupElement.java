package com.intellij.plugins.haxe.ide.lookup;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.Strings;
import com.intellij.plugins.haxe.ide.documentation.providers.HaxeMetadataDocumentations;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class HaxeMetadataLookupElement extends LookupElement {

    public final String name;

    private String presentableText;
    private String tailText;
    private String typeText;

    private boolean insertParentheses = false;
    private Icon icon;


    public HaxeMetadataLookupElement(HaxeMetadataDocumentations.MetadataInfo docs) {
        this.name = docs.getMetadata();

        this.presentableText = ":" + docs.getMetadata();
        this.tailText = docs.getArguments().isEmpty() ? "" : "(" + String.join(",", docs.getArguments()) + ")";
        this.typeText = Strings.join(docs.getPlatforms(), ", ");

        this.icon = AllIcons.Nodes.Annotationtype;
        this.insertParentheses = !docs.getArguments().isEmpty();
    }

    public HaxeMetadataLookupElement(String presentableText, String tailText, String typeText) {
        this.name = presentableText;

        this.presentableText = presentableText;
        this.tailText = tailText;
        this.typeText = typeText;

        this.icon = AllIcons.Nodes.Annotationtype;
    }

    @Override
    public @NotNull String getLookupString() {
        return presentableText;
    }

    @Override
    public void renderElement(@NotNull LookupElementPresentation presentation) {
        presentation.setItemText(presentableText);
        presentation.setTypeText(typeText);
        presentation.setTailText(tailText);
        presentation.setIcon(icon);
    }

    @Override
    public void handleInsert(@NotNull InsertionContext context) {
        super.handleInsert(context);
        WriteCommandAction.writeCommandAction(context.getProject(), context.getFile()).run(() -> {
            int startOffset = context.getStartOffset();
            int tailOffset = context.getTailOffset();
            Document document = context.getEditor().getDocument();
            // We allow completion for both "@" and "@:", to avoid double colon we delete it if it exists
            TextRange range = new TextRange(startOffset - 1, startOffset);
            if (insertParentheses) {

                document.insertString(tailOffset, "()");
                context.getEditor().getCaretModel().moveToOffset(tailOffset+1);
            }
            if (document.getText(range).equals(":")) {
                document.deleteString(startOffset - 1, startOffset);

                context.commitDocument();
            }
        });
    }
}
