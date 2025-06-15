package com.intellij.plugins.haxe.ide.quickfix.typedialog;

import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.ide.HaxeFileTemplateUtil;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class HaxeTypeCreator {

    private static final  List<String> GENERIC_NAMES = List.of(
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T");

    @Nullable
    PsiElement createType(@NotNull Module module, @NonNls @NotNull String qname, @NonNls @NotNull String type, HaxeFile appendToFile, int genericsCount) {
        Project myProject = module.getProject();
        Pair<String, String> splitQName = HaxeResolveUtil.splitQName(qname);
        String packageName = splitQName.first;
        String typeName = splitQName.second;
        String generics = generateGenericsString(genericsCount);
        if (appendToFile != null) {
            return appendType(typeName, generics , type, appendToFile);
        } else {
            AtomicReference<PsiElement> atomicReference = new AtomicReference<>();
            CommandProcessor.getInstance().runUndoTransparentAction(() -> {
                try {
                    PsiManager psiManager = PsiManager.getInstance(myProject);
                    GlobalSearchScope scope = GlobalSearchScope.moduleScope(module);
                    PsiPackage packageByQName = HaxeResolveUtil.findPackageByQName(packageName, psiManager, scope);

                    PsiDirectory[] directories = packageByQName.getDirectories(scope);
                    PsiDirectory directory = directories[0];

                    PsiElement psiElement = HaxeFileTemplateUtil.createType(typeName, generics, packageName, directory, type, HaxeTypeCreator.class.getClassLoader());

                    VirtualFile virtualFile = psiElement.getContainingFile().getVirtualFile();
                    CommandProcessor.getInstance().addAffectedFiles(myProject, virtualFile);

                    atomicReference.set(psiElement);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return atomicReference.get();
        }
    }

    private String generateGenericsString(int generics) {
        if(generics <1) return "";
        if(generics == 1) return  "<T>";

        List<String> name = new ArrayList<>();
        if (GENERIC_NAMES.size() > generics) {
            for (int i = 0; i < generics; i++) {
                name.add(GENERIC_NAMES.get(i));
            }
        } else {
            for (int i = 0; i < generics; i++) {
                name.add("T" + (i + 1));
            }
        }

        return "<" + String.join(",", name) + ">";
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

    private PsiElement appendType(String typeName, String generics, @NonNls @NotNull String type, HaxeFile appendToFile) {
        Project project = appendToFile.getProject();
        PsiElement element = createTypePsi(project, typeName + generics, type);
        if (element == null) return null;
        PsiElement newLine = appendToFile.addAfter(HaxeElementGenerator.createNewLine(project), appendToFile.getLastChild());
        return appendToFile.addAfter(element, newLine);
    }

    private PsiElement createTypePsi(@NotNull Project project, String typeName, @NonNls @NotNull String type) {
        return switch (type) {
            case "HaxeClass" -> HaxeElementGenerator.createClass(project, typeName);
            case "HaxeInterface" -> HaxeElementGenerator.createInterface(project, typeName);
            case "HaxeEnum" -> HaxeElementGenerator.createEnum(project, typeName);
            case "HaxeAbstract" -> HaxeElementGenerator.createAbstract(project, typeName);
            default -> null;
        };
    }


    @NotNull
    @NlsContexts.Command
    String getActionName(@NonNls @NotNull String name, @NonNls @NotNull String templateName) {
        String typeName = HaxeResolveUtil.splitQName(name).second;
        return "create " + typeName;
    }

}

