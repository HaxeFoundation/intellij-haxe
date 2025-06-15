package com.intellij.plugins.haxe.ide.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.HaxeFileTemplateUtil;
import com.intellij.plugins.haxe.ide.completion.HaxeCompletionUtil;
import com.intellij.plugins.haxe.ide.quickfix.typedialog.HaxeCreateTypeDialogBuilder;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeIdentifier;
import com.intellij.plugins.haxe.model.HaxeFileModel;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

import static com.intellij.plugins.haxe.ide.quickfix.HaxeIntroduceTypeUtil.countGenerics;
import static com.intellij.plugins.haxe.ide.quickfix.HaxeIntroduceTypeUtil.requireImport;
import static com.intellij.psi.SmartPointerManager.createPointer;

public class HaxeIntroduceTypeInModuleQuickFix implements LocalQuickFix {

    private final static List<String> TYPE_TEMPLATE_NAMES = List.of("HaxeClass", "HaxeInterface", "HaxeEnum", "HaxeAbstract");

    protected final @NotNull SmartPsiElementPointer<HaxeIdentifier> myPsiTargetPointer;

    public HaxeIntroduceTypeInModuleQuickFix(@NotNull HaxeIdentifier identifier) {
        myPsiTargetPointer = createPointer(identifier);
    }

    @Override
    public @IntentionFamilyName @NotNull String getFamilyName() {
        return "Create Haxe Type (in module)";
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        CommandProcessor.getInstance().markCurrentCommandAsGlobal(project);
        CommandProcessor.getInstance().allowMergeGlobalCommands(() -> {
            VirtualFile virtualFile = myPsiTargetPointer.getVirtualFile();
            Module module = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(virtualFile);
            VirtualFile sourceRoot = ProjectRootManager.getInstance(module.getProject()).getFileIndex().getSourceRootForFile(virtualFile);

            HaxeCreateTypeDialogBuilder builder = HaxeCreateTypeDialogBuilder.createDialog(module, sourceRoot);

            builder.setTitle(HaxeBundle.message("action.create.new.type"));
            List<FileTemplate> templates = HaxeFileTemplateUtil.getApplicableTemplates(project);
            List<FileTemplate> typeTemplates = templates.stream().filter(fileTemplate -> TYPE_TEMPLATE_NAMES.contains(fileTemplate.getName())).toList();
            for (FileTemplate fileTemplate : typeTemplates) {
                final String templateName = fileTemplate.getName();
                final String shortName = HaxeFileTemplateUtil.getTemplateShortName(templateName);
                final Icon icon = HaxeFileTemplateUtil.getTemplateIcon(templateName);
                builder.addKind(shortName, icon, templateName);
            }

            builder.setDefaultText(createDefaultModuleQname());
            builder.setGenericsCount(countGenerics(myPsiTargetPointer.getElement()));
            builder.show("Unable to create haxe type", "HaxeClass", this::updateElement);
        });
    }


    private String createDefaultModuleQname() {
        String typeName = myPsiTargetPointer.getElement().getText();
        if (myPsiTargetPointer.getContainingFile() instanceof HaxeFile haxeFile) {
            HaxeFileModel model = haxeFile.getModel();
            if (model != null) {
                String packageName = model.getPackageName();
                String fileName = model.getFileName();
                String moduleName = fileName.replace(".hx", "");
                return packageName + "." + moduleName + "." + typeName;
            }
        }
        return typeName;
    }

    private void updateElement(PsiElement psiElement, String qname) {
        Pair<String, String> splitQName = HaxeResolveUtil.splitQName(qname);
        String packageName = splitQName.first;
        String typeName = splitQName.second;

        HaxeIdentifier orgElement = myPsiTargetPointer.getElement();
        PsiFile containingFile = myPsiTargetPointer.getContainingFile();
        TextRange range = orgElement.getTextRange();
        // update  identifier if type name changed
        if (!orgElement.textMatches(typeName)) {
            CommandProcessor.getInstance().runUndoTransparentAction(() -> {
                WriteCommandAction.writeCommandAction(containingFile)
                        .withName("Rename Identifier")
                        .withGlobalUndo()
                        .run(() -> {
                            Document fileDocument = containingFile.getFileDocument();
                            fileDocument.replaceString(range.getStartOffset(), range.getEndOffset(), typeName);
                            HaxeCompletionUtil.flushChanges(orgElement.getProject(), fileDocument);
                        });
            });
        }

        if (requireImport(orgElement, packageName)) {
            if (containingFile instanceof HaxeFile haxeFile) {
                CommandProcessor.getInstance().runUndoTransparentAction(() -> {
                    WriteCommandAction.writeCommandAction(containingFile)
                            .withName("Add Import")
                            .withGlobalUndo()
                            .run(() -> {
                                haxeFile.getModel().addImport(qname);
                            });
                });
            }
        }
    }
}
