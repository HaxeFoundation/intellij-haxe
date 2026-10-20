package com.intellij.plugins.haxe.ide.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.HaxeFileTemplateUtil;
import com.intellij.plugins.haxe.ide.quickfix.typedialog.HaxeCreateTypeDialogBuilder;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeIdentifier;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.annotations.NotNull;

import static com.intellij.plugins.haxe.ide.HaxeFileTemplateUtil.CLASS_TEMPLATE;
import static com.intellij.psi.SmartPointerManager.createPointer;

/** Creates the unresolved type as a new file in the identifier's package. */
public class HaxeIntroduceTypeNewFileQuickFix implements LocalQuickFix {

    protected final @NotNull SmartPsiElementPointer<HaxeIdentifier> myPsiTargetPointer;

    public HaxeIntroduceTypeNewFileQuickFix(@NotNull HaxeIdentifier identifier) {
        myPsiTargetPointer = createPointer(identifier);
    }

    @Override
    public @IntentionFamilyName @NotNull String getFamilyName() {
        return HaxeBundle.message("haxe.quickfix.create.type.in.new.file");
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        CommandProcessor commandProcessor = CommandProcessor.getInstance();
        commandProcessor.markCurrentCommandAsGlobal(project);
        commandProcessor.allowMergeGlobalCommands(() -> {
            HaxeCreateTypeDialogBuilder builder = HaxeIntroduceTypeUtil.createTypeDialog(project, myPsiTargetPointer);
            HaxeFileTemplateUtil.addTemplateKinds(HaxeFileTemplateUtil.getApplicableTemplates(project), builder::addKind);
            builder.setDefaultText(createDefaultNewFileQname());
            builder.show(HaxeBundle.message("haxe.quickfix.create.type.failed"), CLASS_TEMPLATE, this::updateElement);
        });
    }

    private void updateElement(PsiElement created, String qname) {
        HaxeIntroduceTypeUtil.updateElement(myPsiTargetPointer, qname);
    }

    /** {@code pack.TypeName}: the identifier as a new module of its own package. */
    private String createDefaultNewFileQname() {
        String typeName = myPsiTargetPointer.getElement().getText();
        if (myPsiTargetPointer.getContainingFile() instanceof HaxeFile haxeFile) {
            return haxeFile.getModel().getPackageName() + "." + typeName;
        }
        return typeName;
    }
}
