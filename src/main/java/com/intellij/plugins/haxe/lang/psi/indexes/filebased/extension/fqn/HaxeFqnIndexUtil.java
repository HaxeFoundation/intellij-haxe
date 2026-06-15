package com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension.fqn;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.model.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.ID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class HaxeFqnIndexUtil {


    static HaxeModuleModel resolveModule(ID<String, HaxeComponentIndexData> index, @NotNull String qName, @NotNull Project project, @Nullable GlobalSearchScope scope, FullyQualifiedInfo fqn) {
        AtomicReference<HaxeModuleModel> reference = new AtomicReference<>();
        FileBasedIndex.getInstance().getFilesWithKey(index, Set.of(qName),
                new Processor<VirtualFile>() {
                    @Override
                    public boolean process(VirtualFile virtualFile) {
                        PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
                        HaxeComponentIndexData data = FileBasedIndex.getInstance().getFileData(index, virtualFile, project).get(qName);
                        String className = data.getFqn().getClassName();
                        if (file instanceof HaxeFile haxeFile) {
                            if(haxeFile.getModule().getModel() instanceof HaxeModuleModel moduleModel) {
                                reference.set(moduleModel);
                                return false;
                            }
                        }
                        return true;
                    }
                }, scope);
        return reference.get();
    }

    static boolean findAndAddMember(FullyQualifiedInfo fqn, List<PsiElement> results, HaxeBaseMemberModel member) {
        if(member != null && !fqn.hasParameterName()) {
            results.add(member.getBasePsi());
            return true;
        }
        return false;
    }

    static boolean findAndAddParameter(FullyQualifiedInfo fqn, List<PsiElement> results, HaxeMethodModel methodModel) {
        List<HaxeParameterModel> parameters = methodModel.getParameters();
        for (HaxeParameterModel parameter : parameters) {
            if(fqn.getParameterName().equals(parameter.getName())) {
                results.add(parameter.getBasePsi());
                return true;
            }
        }
        return false;
    }
}
