package com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension.specialized;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.PackageIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeModule;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexUtil;
import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeSdkInputFilter;
import com.intellij.plugins.haxe.lang.psi.stubs.HaxeStubableFileService;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeModuleModel;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.intellij.util.io.VoidDataExternalizer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class HaxeImportHxFileIndex extends FileBasedIndexExtension<String, Void> {

    private static final ID<String, Void> INDEX = ID.create("HaxeImportHxIndex");
    private static final int INDEX_VERSION = HaxeIndexUtil.BASE_INDEX_VERSION;


    @Override
    public @NotNull ID<String, Void> getName() {
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
    public @NotNull DataExternalizer<Void> getValueExternalizer() {
        return VoidDataExternalizer.INSTANCE;
    }

    @Override
    public FileBasedIndex.@NotNull InputFilter getInputFilter() {
        return HaxeSdkInputFilter.INSTANCE;
    }

    @Override
    public @NotNull DataIndexer<String, Void, FileContent> getIndexer() {
        return new DataIndexer<String, Void, FileContent>() {
            @Override
            public @NotNull Map<String, Void> map(@NonNull FileContent inputData) {
                if (inputData.getPsiFile() instanceof HaxeFile haxeFile) {
                    if (HaxeStubableFileService.skipFilebasedIndex(haxeFile)) {
                        return Map.of();
                    }
                    if ("import.hx".equals(haxeFile.getName())) {
                        HashMap<String, Void> map = new HashMap<>();
                        map.put(determinePackage(haxeFile), null);
                        return map;
                    }
                }

                return Map.of();
            }

            private static String determinePackage(HaxeFile haxeFile) {
                if(haxeFile.getPackageStatement() != null) {
                    return haxeFile.getPackageName();
                }else {
                    // import.hx files are based on path and not package, so the package statment is not required.
                    VirtualFile file = haxeFile.getVirtualFile();
                    return PackageIndex.getInstance(haxeFile.getProject()).getPackageName(file);
                }
            }
        };
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    public static List<HaxeFile> getImportHxForPackage(@NotNull String name, @NotNull Project project, @NotNull GlobalSearchScope scope) {
        List<HaxeFile> haxeImportFiles = new ArrayList<>();

        FileBasedIndex.getInstance().getFilesWithKey(INDEX, Set.of(name),
                new Processor<VirtualFile>() {
                    @Override
                    public boolean process(VirtualFile virtualFile) {
                        PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
                        if (file instanceof HaxeFile haxeFile) {
                            haxeImportFiles.add(haxeFile);
                        }
                        return false;
                    }
                }, scope);

        return haxeImportFiles;
    }
}
