package com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension.fqn;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension.HaxeComponentBaseIndex;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.indexer.HaxeFullyQualifiedNameIndexer;
import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexUtil;
import com.intellij.plugins.haxe.model.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.ID;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension.fqn.HaxeFqnIndexUtil.resolveModule;

public class HaxeFullyQualifiedMemberNameIndex extends HaxeComponentBaseIndex {

    private static final ID<String, HaxeComponentIndexData> INDEX = ID.create("HaxeFullyQualifiedMemberNameIndex");
    private static final int INDEX_VERSION = HaxeIndexUtil.BASE_INDEX_VERSION;


    @Override
    public @NotNull ID<String, HaxeComponentIndexData> getName() {
        return INDEX;
    }

    @Override
    public int getVersion() {
        return INDEX_VERSION;
    }

    @Override
    public @NotNull KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }


    @Override
    public @NotNull DataIndexer<String, HaxeComponentIndexData, FileContent> getIndexer() {
        return new HaxeFullyQualifiedNameIndexer();
    }


    public static Collection<PsiElement> getByFqn(@NotNull String name, @NotNull Project project, @Nullable GlobalSearchScope scope) {

        List<PsiElement> results = new ArrayList<>();

        List<HaxeComponentIndexData> values = FileBasedIndex.getInstance().getValues(INDEX, name, scope);
        for (HaxeComponentIndexData value : values) {
            FullyQualifiedInfo fqn = value.getFqn();
            if(!value.getFqn().hasMemberName()) {
                continue;
            }

            HaxeModuleModel  moduleModel =  resolveModule(INDEX, name, project, scope, fqn);
            if(moduleModel == null) {
                continue;
            }

            if (fqn.moduleName.equals(fqn.getClassName())) {
                HaxeClassModel mainClass = moduleModel.getMainClass();
                // check main class
                if (mainClass != null) {
                    HaxeBaseMemberModel member = mainClass.getMemberSelf(fqn.memberName, null);
                    if (findAndAddMember(fqn, results, member)) continue;

                    if (member instanceof HaxeMethodModel methodModel) {
                        if (findAndAddParameter(fqn, results, methodModel)) continue;
                    }
                    // check module
                } else {
                    HaxeBaseMemberModel member = moduleModel.getMember(fqn.memberName, null);
                    if (findAndAddMember( fqn, results,member)) continue;

                    if(member instanceof HaxeMethodModel methodModel) {
                        if(findAndAddParameter(fqn, results, methodModel)) continue;
                    }

                }
                // find non-main class
            }else if(fqn.hasClassName()) {
                HaxeClassModel classModel = moduleModel.getClass(fqn.className);
                if (classModel != null) {
                    HaxeBaseMemberModel member = classModel.getMemberSelf(fqn.memberName, null);
                    if (findAndAddMember(fqn, results, member)) continue;

                    if (member instanceof HaxeMethodModel methodModel) {
                        if (findAndAddParameter(fqn, results, methodModel)) continue;
                    }
                }
            }
        }
        return results;
    }


    private static boolean findAndAddMember(FullyQualifiedInfo fqn, List<PsiElement> results, HaxeBaseMemberModel member) {
        if(member != null && !fqn.hasParameterName()) {
            results.add(member.getBasePsi());
            return true;
        }
        return false;
    }

    private static boolean findAndAddParameter(FullyQualifiedInfo fqn, List<PsiElement> results, HaxeMethodModel methodModel) {
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
