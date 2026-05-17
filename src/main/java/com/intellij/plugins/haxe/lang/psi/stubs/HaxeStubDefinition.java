package com.intellij.plugins.haxe.lang.psi.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeFileStub;
import com.intellij.psi.PsiFile;
import com.intellij.psi.StubBuilder;
import com.intellij.psi.stubs.DefaultStubBuilder;
import com.intellij.psi.stubs.LanguageStubDefinition;
import com.intellij.psi.stubs.PsiFileStub;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.IOException;

import static com.intellij.plugins.haxe.lang.psi.stubs.HaxeStubFilterUtil.docsConditionalOrSpacing;
import static com.intellij.plugins.haxe.lang.psi.stubs.HaxeStubFilterUtil.isElementTypeToSkip;

public class HaxeStubDefinition  implements LanguageStubDefinition {


    @Override
    public boolean shouldBuildStubFor(@NonNull VirtualFile file) {
        //TODO
        // we have to skip anything that is part of  Conditional compilation as different projects will
        // can get different stub trees, and intellij currently only supports shared / applicationwide indexes.
        // there is an issue for this, but Jetbrains has stated that they wont work on this anytime soon.
        //https://youtrack.jetbrains.com/issue/IJPL-155859/Introduce-project-scoped-view-of-app-wide-indexes

        //TODO  try to make our own logic for project local stubs
        return false;

    }



    @Override
    public int getStubVersion() {
        return HaxeStubVersions.STUB_VERSION;
    }

    @Override
    public @NonNull StubBuilder getBuilder() {
        return new DefaultStubBuilder() {
            @NotNull
            @Override
            protected PsiFileStub<?> createStubForFile(@NotNull PsiFile file) {
                String name = file.getName();

                if (file instanceof HaxeFile haxeFile) {
                    return new HaxeFileStub(haxeFile, name);
                }
                return new HaxeFileStub(null, name);
            }

            /**
             * attempt at reducing stub creation for anything inside a method/function body.
             */
            @Override
            public boolean skipChildProcessingWhenBuildingStubs(@NotNull ASTNode parent, @NotNull ASTNode node) {
                IElementType currentType = node.getElementType();
                IElementType parentType = parent.getElementType();

                if(docsConditionalOrSpacing(currentType)) return true;

                // skip method blocks (Note that methods can be created without a block as body)
                if (isElementTypeToSkip(parentType) || isElementTypeToSkip(currentType)) return true;

                return false;
            }
        };
    }
}
