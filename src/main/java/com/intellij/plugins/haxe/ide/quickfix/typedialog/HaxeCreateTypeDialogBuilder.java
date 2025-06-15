package com.intellij.plugins.haxe.ide.quickfix.typedialog;

import com.intellij.ide.actions.ElementCreator;
import com.intellij.ide.actions.newclass.CreateWithTemplatesDialogPanel;
import com.intellij.ide.ui.newItemPopup.NewItemPopupUtil;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;


public class HaxeCreateTypeDialogBuilder {

    public static HaxeCreateTypeDialogBuilder createDialog(final @NotNull Module module, @NotNull VirtualFile sourceRoot) {
        return new HaxeCreateTypeDialogBuilder(module, sourceRoot);
    }


   @NotNull private final Project myProject;
   @NotNull private final Module myModule;
   @NotNull private final VirtualFile mySourceRoot;

    @NlsContexts.PopupTitle
    private String myTitle = LangBundle.message("popup.title.default.title");
    private int numberOfGenerics = 0;
    private String myDefaultText = null;
    private final List<CreateWithTemplatesDialogPanel.TemplatePresentation> myTemplatesList = new ArrayList<>();
    private InputValidator myInputValidator = new HaxeTypeQnameNameValidator();

    @Nullable
    private  BiFunction<? super String, ? super CreateWithTemplatesDialogPanel.TemplatePresentation, Boolean> myKindSelector;

    private HaxeCreateTypeDialogBuilder(@NotNull Module module, @NotNull VirtualFile sourceRoot) {
        mySourceRoot = sourceRoot;
        myProject = module.getProject();
        myModule = module;
    }

    public HaxeCreateTypeDialogBuilder setTitle(@NlsContexts.PopupTitle String title) {
        myTitle = title;
        return this;
    }

    public HaxeCreateTypeDialogBuilder setDefaultText(String text) {
        myDefaultText = text;
        return this;
    }

    public HaxeCreateTypeDialogBuilder addKind(@Nls @NotNull String kind,
                                               @Nullable Icon icon,
                                               @NotNull String templateName) {

        myTemplatesList.add(new CreateWithTemplatesDialogPanel.TemplatePresentation(kind, icon, templateName));
        return this;
    }


    public <T extends PsiElement> void show(@NotNull String errorTitle,
                                            @Nullable String selectedItem,
                                            @Nullable BiConsumer<? super T,String> elementConsumer) {


        CreateWithTemplatesDialogPanel contentPanel = new CreateWithTemplatesDialogPanel(selectedItem, myTemplatesList);
        ElementCreator elementCreator = createElementCreator(myModule, errorTitle, contentPanel, numberOfGenerics);
        JBPopup popup = NewItemPopupUtil.createNewItemPopup(myTitle, contentPanel, contentPanel.getNameField());

        if (myDefaultText != null) {
            JTextField textField = contentPanel.getTextField();
            textField.setText(myDefaultText);
            textField.selectAll();
        }

        if (myKindSelector != null) {
            contentPanel.setTemplateSelectorMatcher(myKindSelector);
        }

        contentPanel.setApplyAction(e -> {
            String newElementName = contentPanel.getEnteredName();
            if (StringUtil.isEmptyOrSpaces(newElementName)) return;

            boolean isValid = myInputValidator == null || myInputValidator.canClose(newElementName);

            if (isValid) {
                popup.closeOk(e);
                //noinspection unchecked
                T createdElement = (T) createElement(newElementName, elementCreator);
                if (createdElement != null && elementConsumer != null) {
                    elementConsumer.accept(createdElement, newElementName);
                }
            } else {
                String errorMessage = Optional.ofNullable(myInputValidator)
                        .filter(validator -> validator instanceof InputValidatorEx)
                        .map(validator -> ((InputValidatorEx) validator).getErrorText(newElementName))
                        .orElse(LangBundle.message("incorrect.name"));
                contentPanel.setError(errorMessage);
            }
        });

        Disposer.register(popup, contentPanel);
        popup.showCenteredInCurrentWindow(myProject);
    }

    private static @NotNull ElementCreator createElementCreator(@NotNull Module module, @NotNull String errorTitle, CreateWithTemplatesDialogPanel contentPanel, int numberOfGenerics) {
        HaxeTypeCreator typeCreator = new HaxeTypeCreator();
        return new ElementCreator(module.getProject(), errorTitle) {
            final int generics = numberOfGenerics;

            @Override
            protected PsiElement @NotNull [] create(@NotNull String newName) {
                HaxeFile appendToFile = fileToAppendTypeIn(newName, module);
                String typeQname = contentPanel.getEnteredName();
                String type = contentPanel.getSelectedTemplate();

                PsiElement element = typeCreator.createType(module, typeQname, type, appendToFile, generics);
                return element != null ? new PsiElement[]{element} : PsiElement.EMPTY_ARRAY;
            }

            @Override
            protected @NotNull String getActionName(@NotNull String newName) {
                return typeCreator.getActionName(newName, contentPanel.getSelectedTemplate());
            }

            private HaxeFile fileToAppendTypeIn(@NotNull String newName, @NotNull Module module) {
                String packageOrModuleName = HaxeResolveUtil.splitQName(newName).first;
                PsiManager psiManager = PsiManager.getInstance(module.getProject());
                GlobalSearchScope projectScope = GlobalSearchScope.moduleScope(module);
                HaxeClass resolve = HaxeResolveUtil.findClassByQName(packageOrModuleName, psiManager, projectScope);
                PsiFile psiFile = resolve == null ? null : resolve.getContainingFile();
                if(psiFile instanceof HaxeFile haxeFile) return haxeFile;
                return null;
            }
        };
    }


    private @Nullable PsiElement createElement(String newElementName, ElementCreator creator) {
        if(!validatePackage(newElementName)) return null;


        PsiElement[] elements = creator.tryCreate(newElementName);
        return elements.length > 0 ? elements[0] : null;
    }

    public void setGenericsCount(int count) {
        numberOfGenerics = count;
    }

    private boolean validatePackage(String newElementName) {
        FullyQualifiedInfo qualifiedInfo = new FullyQualifiedInfo(newElementName);
        String packageName = qualifiedInfo.packagePath;

        PsiManager psiManager = PsiManager.getInstance(myProject);
        GlobalSearchScope scope = GlobalSearchScope.moduleScope(myModule);
        PsiPackage packageByQName = HaxeResolveUtil.findPackageByQName(packageName, psiManager, scope);

        if (packageByQName == null) {
            MessageDialogBuilder.YesNo createPackageDialog = MessageDialogBuilder.yesNo("Create missing package(s)", "create '" + packageName + "' package");
            boolean ask = ReadAction.compute(() -> createPackageDialog.ask(myProject));
            if (ask) {
                createHaxePackage(myProject, mySourceRoot, packageName);
                return true;
            } else {
                // missing package, not going to create type
                return false;
            }
        }
        return true;
    }

    private void createHaxePackage(Project myProject, VirtualFile sourceRoot, String packageName) {
        CommandProcessor.getInstance().runUndoTransparentAction(() -> {
            WriteCommandAction.writeCommandAction(myProject)
                    .withName("create package(s)")
                    .withGlobalUndo()
                    .compute(() -> {
                        try {
                            String[] pathElements = packageName.split("\\.");
                            VirtualFile path = sourceRoot;
                            for (String pathElement : pathElements) {
                                VirtualFile child = path.findChild(pathElement);
                                if (child == null) {
                                    path = path.createChildDirectory(path, pathElement);
                                    CommandProcessor.getInstance().addAffectedFiles(myProject, path);
                                } else {
                                    path = child;
                                }
                            }
                            return path;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        });
    }
}



