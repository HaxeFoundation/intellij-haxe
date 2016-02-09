/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.ide.index;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.*;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeComponentFileNameIndex extends ScalarIndexExtension<String> {
  public static final ID<String, Void> HAXE_COMPONENT_FILE_NAME_INDEX = ID.create("HaxeComponentFileNameIndex");
  private static final int INDEX_VERSION = HaxeIndexUtil.BASE_INDEX_VERSION + 4;
  private DataIndexer<String, Void, FileContent> myDataIndexer = new MyDataIndexer();

  @NotNull
  @Override
  public ID<String, Void> getName() {
    return HAXE_COMPONENT_FILE_NAME_INDEX;
  }

  @NotNull
  @Override
  public DataIndexer<String, Void, FileContent> getIndexer() {
    return myDataIndexer;
  }

  @Override
  public int getVersion() {
    return INDEX_VERSION;
  }

  @Override
  public boolean dependsOnFileContent() {
    return true;
  }

  @Override
  public KeyDescriptor<String> getKeyDescriptor() {
    return new EnumeratorStringDescriptor();
  }

  @Override
  public FileBasedIndex.InputFilter getInputFilter() {
    return HaxeSdkInputFilter.INSTANCE;
  }

  @NotNull
  public static List<VirtualFile> getFilesNameByQName(@NotNull String qName, @NotNull final GlobalSearchScope filter) {
    final List<VirtualFile> result = new ArrayList<VirtualFile>();
    getFileNames(qName, new Processor<VirtualFile>() {
      @Override
      public boolean process(VirtualFile file) {
        result.add(file);
        return true;
      }
    }, filter);
    return result;
  }

  public static boolean getFileNames(@NotNull String qName,
                                     @NotNull Processor<VirtualFile> processor,
                                     @NotNull final GlobalSearchScope filter) {
    return FileBasedIndex.getInstance()
      .getFilesWithKey(HAXE_COMPONENT_FILE_NAME_INDEX, Collections.<String>singleton(qName), processor, filter);
  }

  private static class MyDataIndexer implements DataIndexer<String, Void, FileContent> {
    @Override
    @NotNull
    public Map<String, Void> map(final FileContent inputData) {
      final PsiFile psiFile = inputData.getPsiFile();
      final String moduleName = FileUtil.getNameWithoutExtension(inputData.getFileName());
      final List<HaxeClass> classes = HaxeResolveUtil.findComponentDeclarations(psiFile);
      if (classes.isEmpty()) {
        return Collections.emptyMap();
      }
      boolean moduleAdded = false;
      final Map<String, Void> result = new THashMap<String, Void>(classes.size());
      for (HaxeClass haxeClass : classes) {
        final String className = haxeClass.getName();
        if (className != null) {
          if(!moduleAdded && className.equals(moduleName)) {
            moduleAdded = true;
          }
          result.put(haxeClass.getQualifiedName(), null);
        }
      }
      if(!moduleAdded) {
        String mainModuleName = HaxeResolveUtil.joinQName(HaxeResolveUtil.getPackageName(psiFile), moduleName);
        result.put(mainModuleName, null);
      }
      return result;
    }
  }
}
