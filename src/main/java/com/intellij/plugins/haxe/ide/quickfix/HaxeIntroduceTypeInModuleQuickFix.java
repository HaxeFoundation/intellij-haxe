package com.intellij.plugins.haxe.ide.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.HaxeFileTemplateUtil;
import com.intellij.plugins.haxe.ide.quickfix.typedialog.HaxeCreateTypeDialogBuilder;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeIdentifier;
import com.intellij.plugins.haxe.model.HaxeFileModel;
import com.intellij.plugins.haxe.util.HaxeModuleVariants;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.plugins.haxe.ide.HaxeFileTemplateUtil.CLASS_TEMPLATE;
import static com.intellij.plugins.haxe.ide.HaxeFileTemplateUtil.TYPE_TEMPLATE_NAMES;
import static com.intellij.psi.SmartPointerManager.createPointer;

/** Creates the unresolved type inside the identifier's own module (only the type templates apply: a module holds types, not files). */
public class HaxeIntroduceTypeInModuleQuickFix implements LocalQuickFix {

    protected final @NotNull SmartPsiElementPointer<HaxeIdentifier> myPsiTargetPointer;

    public HaxeIntroduceTypeInModuleQuickFix(@NotNull HaxeIdentifier identifier) {
        myPsiTargetPointer = createPointer(identifier);
    }

    @Override
    public @IntentionFamilyName @NotNull String getFamilyName() {
        return HaxeBundle.message("haxe.quickfix.create.type.in.module");
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        CommandProcessor.getInstance().markCurrentCommandAsGlobal(project);
        CommandProcessor.getInstance().allowMergeGlobalCommands(() -> {
            HaxeCreateTypeDialogBuilder builder = HaxeIntroduceTypeUtil.createTypeDialog(project, myPsiTargetPointer);
            List<FileTemplate> typeTemplates =
              HaxeFileTemplateUtil.getApplicableTemplates(project, fileTemplate -> TYPE_TEMPLATE_NAMES.contains(fileTemplate.getName()));
            HaxeFileTemplateUtil.addTemplateKinds(typeTemplates, builder::addKind);
            builder.setDefaultText(createDefaultModuleQname());
            builder.show(HaxeBundle.message("haxe.quickfix.create.type.failed"), CLASS_TEMPLATE, this::updateElement);
        });
    }

    private void updateElement(PsiElement created, String qname) {
        HaxeIntroduceTypeUtil.updateElement(myPsiTargetPointer, qname);
    }

    /** {@code pack.Module.TypeName}: the identifier as a new type of its own module. */
    private String createDefaultModuleQname() {
        String typeName = myPsiTargetPointer.getElement().getText();
        if (myPsiTargetPointer.getContainingFile() instanceof HaxeFile haxeFile) {
            HaxeFileModel model = haxeFile.getModel();
            if (model != null) {
                String moduleName = HaxeModuleVariants.moduleNameOf(model.getFileName());
                return model.getPackageName() + "." + moduleName + "." + typeName;
            }
        }
        return typeName;
    }
}
