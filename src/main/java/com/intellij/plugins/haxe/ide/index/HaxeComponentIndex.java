/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeComponent;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeComponentIndex extends FileBasedIndexExtension<String, HaxeClassInfo> {
  public static final ID<String, HaxeClassInfo> HAXE_COMPONENT_INDEX = ID.create("HaxeComponentIndex");
  private static final int INDEX_VERSION = HaxeIndexUtil.BASE_INDEX_VERSION + 6;
  private final DataIndexer<String, HaxeClassInfo, FileContent> myIndexer = new MyDataIndexer();
  private final DataExternalizer<HaxeClassInfo> myExternalizer = new HaxeClassInfoExternalizer();

  @NotNull
  @Override
  public ID<String, HaxeClassInfo> getName() {
    return HAXE_COMPONENT_INDEX;
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
  public DataExternalizer<HaxeClassInfo> getValueExternalizer() {
    return myExternalizer;
  }

  @Override
  public FileBasedIndex.InputFilter getInputFilter() {
    return HaxeSdkInputFilter.INSTANCE;
  }

  @NotNull
  @Override
  public DataIndexer<String, HaxeClassInfo, FileContent> getIndexer() {
    return myIndexer;
  }

  public static List<HaxeComponent> getItemsByName(String name, Project project, GlobalSearchScope searchScope) {
    HaxeIndexUtil.warnIfDumbMode(project);
    Collection<VirtualFile> files =
      FileBasedIndex.getInstance().getContainingFiles(HAXE_COMPONENT_INDEX, name, searchScope);
    final List<HaxeComponent> result = new ArrayList<HaxeComponent>();
    for (VirtualFile vFile : files) {
      PsiFile file = PsiManager.getInstance(project).findFile(vFile);
      if (file == null || file.getFileType() != HaxeFileType.INSTANCE) {
        continue;
      }
      final HaxeComponent component = HaxeResolveUtil.findComponentDeclaration(file, name);
      if (component != null) {
        result.add(component);
      }
    }
    return result;
  }

  public static void processAll(Project project, Processor<Pair<String, HaxeClassInfo>> processor, GlobalSearchScope scope) {
    HaxeIndexUtil.warnIfDumbMode(project);
    final Collection<String> keys = getNames(project);
    for (String key : keys) {
      final List<HaxeClassInfo> values = FileBasedIndex.getInstance().getValues(HAXE_COMPONENT_INDEX, key, scope);
      for (HaxeClassInfo value : values) {
        final Pair<String, HaxeClassInfo> pair = Pair.create(key, value);
        if (!processor.process(pair)) {
          return;
        }
      }
    }
  }

  public static Collection<String> getNames(Project project) {
    HaxeIndexUtil.warnIfDumbMode(project);
    return FileBasedIndex.getInstance().getAllKeys(HAXE_COMPONENT_INDEX, project);
  }

  private static class MyDataIndexer implements DataIndexer<String, HaxeClassInfo, FileContent> {
    @Override
    @NotNull
    public Map<String, HaxeClassInfo> map(final FileContent inputData) {
      final PsiFile psiFile = inputData.getPsiFile();
      final List<HaxeClass> classes = HaxeResolveUtil.findComponentDeclarations(psiFile);
      if (classes.isEmpty()) {
        return Collections.emptyMap();
      }
      final Map<String, HaxeClassInfo> result = new THashMap<String, HaxeClassInfo>(classes.size());
      for (HaxeClass haxeClass : classes) {
        if (haxeClass.getName() == null) {
          continue;
        }
        final Pair<String, String> packageAndName = HaxeResolveUtil.splitQName(haxeClass.getQualifiedName());
        final HaxeClassInfo info = new HaxeClassInfo(packageAndName.getFirst(), HaxeComponentType.typeOf(haxeClass));
        result.put(packageAndName.getSecond(), info);
      }
      return result;
    }
  }
}
