package com.intellij.plugins.haxe.lang.parser;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeDocumentationUtil;
import com.intellij.psi.PsiDocCommentBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.PsiCommentImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;



public class haxePsiDocCommentImpl extends PsiCommentImpl  implements PsiDocCommentBase {

    private String extractedDocs;

    public haxePsiDocCommentImpl(@NotNull IElementType type, @NotNull CharSequence text) {
        super(type, text);
    }

    @Override
    public @Nullable PsiElement getOwner() {
        return CachedValuesManager.getProjectPsiDependentCache(this, haxePsiDocCommentImpl::_getOwner);
    }

    private static @Nullable PsiElement _getOwner(PsiElement element) {
        HaxeNamedComponent namedComponent = PsiTreeUtil.getNextSiblingOfType(element, HaxeNamedComponent.class);
        if(namedComponent != null) return namedComponent;
        // if not member of a class, try to see if its a module member (can be  more than just class, ex. field, method)
        HaxeModule module = PsiTreeUtil.getNextSiblingOfType(element, HaxeModule.class);
        if(module != null) {
            PsiElement firstChild = module.getFirstChild();
            if(firstChild instanceof HaxeNamedComponent namedComponent2 ) return namedComponent2;
            return PsiTreeUtil.getNextSiblingOfType(firstChild, HaxeNamedComponent.class);
        }
        return null;
    }

    public @NotNull String getDocsWithoutIndents() {
        if(extractedDocs == null) {
            String rawText = this.getText();
            String unwrapped = HaxeDocumentationUtil.unwrapCommentDelimiters(rawText);
            String trimmed = HaxeDocumentationUtil.removeExcessLines(unwrapped);

            boolean javaDocStyle = HaxeDocumentationUtil.docIsJavadocStyle(trimmed);
            extractedDocs = HaxeDocumentationUtil.stripIndents(trimmed, javaDocStyle);
            // hack for rendering tags (@param, @event) as empty lines will cause content to be parsed as indented code
            extractedDocs = HaxeDocumentationUtil.tryFixIndents(extractedDocs);

        }
        return extractedDocs;
    }


}
