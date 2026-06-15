package com.intellij.plugins.haxe.lang.psi.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HaxeStubFilterUtil {



    public static boolean isElementTypeToSkip(IElementType parentType) {
        return parentType == HaxeTokenTypes.VAR_INIT // Skip field inits
                || parentType == HaxeTokenTypes.BLOCK_STATEMENT
                //
                // Skip method "bodies" that are not blocks
                || parentType == HaxeTokenTypes.RETURN_STATEMENT
                || parentType == HaxeTokenTypes.IF_STATEMENT
                || parentType == HaxeTokenTypes.TRY_STATEMENT
                || parentType == HaxeTokenTypes.SWITCH_STATEMENT
                || parentType == HaxeTokenTypes.WHILE_STATEMENT
                || parentType == HaxeTokenTypes.DO_WHILE_STATEMENT
                || parentType == HaxeTokenTypes.FOR_STATEMENT
                || parentType == HaxeTokenTypes.THROW_STATEMENT
                //
                || parentType == HaxeTokenTypes.THIS_EXPRESSION
                || parentType == HaxeTokenTypes.SUPER_EXPRESSION
                || parentType == HaxeTokenTypes.ASSIGN_EXPRESSION;
    }

    public static boolean docsConditionalOrSpacing(IElementType type) {
        if (HaxeTokenTypeSets.WHITESPACES.contains(type)) return true;
        if (HaxeTokenTypeSets.ONLY_COMMENTS.contains(type)) return true;
        if (HaxeTokenTypeSets.CONDITIONALLY_NOT_COMPILED.contains(type)) return true;
        return false;
    }

}
