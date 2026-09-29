package com.intellij.plugins.haxe.lang.parser;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeDocumentationUtil;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.LiteralTextEscaper;
import com.intellij.psi.PsiDocCommentBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * A doc comment as a lazily parsed composite: consumers reading only the text
 * never trigger the sub-tree; the formatter parses it to manage the interior
 * line indentation. The token type stays DOC_COMMENT. As an injection host it
 * carries the markdown code fences' injected fragments (see HaxeDocFenceInjector).
 */
public class HaxePsiDocCommentImpl extends LazyParseablePsiElement
  implements PsiDocCommentBase, HaxeLazyWithOwner, PsiLanguageInjectionHost {

    private String extractedDocs;

    public HaxePsiDocCommentImpl(@NotNull IElementType type, @Nullable CharSequence text) {
        super(type, text);
    }

    @Override
    public @NotNull IElementType getTokenType() {
        return getElementType();
    }

    // the label PsiCommentImpl used - keeps parse-tree dumps (and their test goldens) stable
    @Override
    public String toString() {
        return "PsiComment(" + getElementType() + ")";
    }

    @Override
    public boolean isValidHost() {
        return true;
    }

    @Override
    public PsiLanguageInjectionHost updateText(@NotNull String text) {
        return ElementManipulators.handleContentChange(this, text);
    }

    @Override
    public @NotNull LiteralTextEscaper<? extends PsiLanguageInjectionHost> createLiteralTextEscaper() {
        return LiteralTextEscaper.createSimple(this);
    }

    @Override
    public @Nullable PsiElement getOwner() {
        return CachedValuesManager.getProjectPsiDependentCache(this, HaxePsiDocCommentImpl::_getOwner);
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
            extractedDocs = HaxeDocumentationUtil.stripForRendering(unwrapped);
        }
        return extractedDocs;
    }


}
