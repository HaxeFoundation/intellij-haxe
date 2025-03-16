package com.intellij.plugins.haxe.lang.parser;

import com.intellij.plugins.haxe.util.HaxeDocumentationUtil;
import com.intellij.psi.PsiDocCommentBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.PsiCommentImpl;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;



public class haxePsiDocCommentImpl extends PsiCommentImpl  implements PsiDocCommentBase {

    private String extractedDocs;

    public haxePsiDocCommentImpl(@NotNull IElementType type, @NotNull CharSequence text) {
        super(type, text);
    }

    @Override
    public @Nullable PsiElement getOwner() {
        return null;
    }

    public @NotNull String getDocsWithoutIndents() {
        if(extractedDocs == null) {
            String rawText = this.getText();
            String unwrapped = HaxeDocumentationUtil.unwrapCommentDelimiters(rawText);
            String trimmed = HaxeDocumentationUtil.removeExcessLines(unwrapped);

            boolean javaDocStyle = HaxeDocumentationUtil.docIsJavadocStyle(trimmed);
            extractedDocs = HaxeDocumentationUtil.stripIndents(trimmed, javaDocStyle);

        }
        return extractedDocs;
    }


}
