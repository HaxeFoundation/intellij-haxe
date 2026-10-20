package com.intellij.plugins.haxe.ide.quickfix;

import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.completion.HaxeCompletionUtil;
import com.intellij.plugins.haxe.ide.quickfix.typedialog.HaxeCreateTypeDialogBuilder;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeIdentifier;
import com.intellij.plugins.haxe.lang.psi.HaxeType;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** The steps the create-type quick fixes share: the dialog over the identifier's source root, and the follow-up edits at the identifier. */
public class HaxeIntroduceTypeUtil {

    /** The create-type dialog for the module and source root holding the identifier's file. */
    static HaxeCreateTypeDialogBuilder createTypeDialog(@NotNull Project project, @NotNull SmartPsiElementPointer<HaxeIdentifier> target) {
        VirtualFile virtualFile = target.getVirtualFile();
        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        Module module = fileIndex.getModuleForFile(virtualFile);
        VirtualFile sourceRoot = fileIndex.getSourceRootForFile(virtualFile);
        HaxeCreateTypeDialogBuilder builder = HaxeCreateTypeDialogBuilder.createDialog(module, sourceRoot);
        builder.setTitle(HaxeBundle.message("action.create.new.type"));
        builder.setGenericsCount(countGenerics(target.getElement()));
        return builder;
    }

    /**
     * After the type was created under {@code qname}: renames the identifier when the
     * chosen type name differs from it, and imports the type when it lives in another package.
     */
    static void updateElement(@NotNull SmartPsiElementPointer<HaxeIdentifier> target, @NotNull String qname) {
        Pair<String, String> splitQName = HaxeResolveUtil.splitQName(qname);
        String packageName = splitQName.first;
        String typeName = splitQName.second;

        HaxeIdentifier orgElement = target.getElement();
        PsiFile containingFile = target.getContainingFile();
        if (orgElement == null || containingFile == null) return;
        TextRange range = orgElement.getTextRange();
        if (!orgElement.textMatches(typeName)) {
            CommandProcessor.getInstance().runUndoTransparentAction(() -> {
                WriteCommandAction.writeCommandAction(containingFile)
                        .withName(HaxeBundle.message("haxe.quickfix.create.type.rename.identifier"))
                        .withGlobalUndo()
                        .run(() -> {
                            Document fileDocument = containingFile.getFileDocument();
                            fileDocument.replaceString(range.getStartOffset(), range.getEndOffset(), typeName);
                            HaxeCompletionUtil.flushChanges(orgElement.getProject(), fileDocument);
                        });
            });
        }

        if (requireImport(orgElement, packageName) && containingFile instanceof HaxeFile haxeFile) {
            CommandProcessor.getInstance().runUndoTransparentAction(() -> {
                WriteCommandAction.writeCommandAction(containingFile)
                        .withName(HaxeBundle.message("haxe.quickfix.create.type.add.import"))
                        .withGlobalUndo()
                        .run(() -> haxeFile.getModel().addImport(qname));
            });
        }
    }

    protected static int countGenerics(@Nullable HaxeIdentifier element) {
        HaxeType parentOfType = PsiTreeUtil.getParentOfType(element, HaxeType.class);
        if (parentOfType != null && parentOfType.getTypeParam() != null) {
            return parentOfType.getTypeParam().getTypeList().size();
        }
        return 0;
    }

    protected static boolean requireImport(HaxeIdentifier sourceElement, String targetQname) {
        FullyQualifiedInfo qualifiedInfo = new FullyQualifiedInfo(targetQname); // make sure we filter out
        if(sourceElement.getContainingFile() instanceof HaxeFile haxeFile) {
            return !haxeFile.getPackageName().equals(qualifiedInfo.packageName);
        }
        return true;
    }
}
