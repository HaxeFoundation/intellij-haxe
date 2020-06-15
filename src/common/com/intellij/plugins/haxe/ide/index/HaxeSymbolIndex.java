/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2016 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2020 Eric Bishton
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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.CommonProcessors;
import com.intellij.util.indexing.*;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HaxeSymbolIndex extends ScalarIndexExtension<String> {
  public static final ID<String, Void> HAXE_SYMBOL_INDEX = ID.create("HaxeSymbolIndex");
  private static final int INDEX_VERSION = HaxeIndexUtil.BASE_INDEX_VERSION + 8;
  private DataIndexer<String, Void, FileContent> myDataIndexer = new MyDataIndexer();

  @NotNull
  @Override
  public ID<String, Void> getName() {
    return HAXE_SYMBOL_INDEX;
  }

  @Override
  public int getVersion() {
    return INDEX_VERSION;
  }

  @NotNull
  @Override
  public DataIndexer<String, Void, FileContent> getIndexer() {
    return myDataIndexer;
  }

  @NotNull
  @Override
  public KeyDescriptor<String> getKeyDescriptor() {
    return new EnumeratorStringDescriptor();
  }

  @NotNull
  @Override
  public FileBasedIndex.InputFilter getInputFilter() {
    return HaxeSdkInputFilter.INSTANCE;
  }

  @Override
  public boolean dependsOnFileContent() {
    return true;
  }

  public static String[] getAllSymbols(@NotNull final GlobalSearchScope scope) {
    HaxeIndexUtil.warnIfDumbMode(scope.getProject());
    final CommonProcessors.CollectProcessor<String> processor = new CommonProcessors.CollectProcessor<String>();
    FileBasedIndex.getInstance().processAllKeys(HAXE_SYMBOL_INDEX, processor, scope, null);
    return ArrayUtil.toStringArray(processor.getResults());
  }

  public static List<HaxeComponentName> getItemsByName(@NotNull final String name,
                                                       @NotNull final Project project,
                                                       @NotNull final GlobalSearchScope searchScope) {
    HaxeIndexUtil.warnIfDumbMode(project);
    final Collection<VirtualFile> files =
      FileBasedIndex.getInstance().getContainingFiles(HAXE_SYMBOL_INDEX, name, searchScope);
    final Set<HaxeComponentName> result = new THashSet<>();
    for (VirtualFile vFile : files) {
      final PsiFile psiFile = PsiManager.getInstance(project).findFile(vFile);
      if (psiFile == null || psiFile.getFileType() != HaxeFileType.HAXE_FILE_TYPE) {
        continue;
      }
      processComponents(psiFile, subComponent -> {
        if (name.equals(subComponent.getName())) {
          result.add(subComponent.getComponentName());
        }
        return true;
      });
    }
    return new ArrayList<>(result);
  }

  private static class MyDataIndexer implements DataIndexer<String, Void, FileContent> {
    @Override
    @NotNull
    public Map<String, Void> map(@NotNull final FileContent inputData) {
      final PsiFile psiFile = inputData.getPsiFile();
      final List<HaxeClass> classes = HaxeResolveUtil.findComponentDeclarations(psiFile);
      if (classes.isEmpty()) {
        return Collections.emptyMap();
      }
      final Map<String, Void> result = new THashMap<>();
      for (HaxeClass haxeClass : classes) {
        final String className = haxeClass.getName();
        if (className == null) {
          continue;
        }
        result.put(className, null);
        for (HaxeNamedComponent namedComponent : getNamedComponents(haxeClass)) {
          result.put(namedComponent.getName(), null);
        }
      }
      return result;
    }
  }

  private static void processComponents(PsiFile psiFile, PsiElementProcessor<HaxeNamedComponent> processor) {
    // top-level components
    final List<HaxeClass> classes = HaxeResolveUtil.findComponentDeclarations(psiFile);
    for (HaxeClass cls : classes) {
      if (!processSubComponent(processor, cls)) {
        return;
      }
    }
  }

  private static boolean processSubComponent(PsiElementProcessor<HaxeNamedComponent> processor, HaxeComponent component) {
    final String componentName = component.getName();
    if (componentName == null) {
      return true;
    }
    if (!processor.execute(component)) {
      return false;
    }
    if (component instanceof HaxeClass) {
      for (HaxeNamedComponent subComponent : getNamedComponents((HaxeClass)component)) {
        if (!processor.execute(subComponent)) {
          return false;
        }
      }
    }
    return true;
  }

  private static final Class[] BODY_TYPES =
    new Class[]{HaxeClassBody.class, HaxeAbstractBody.class, HaxeEnumBody.class, HaxeExternClassDeclarationBody.class, HaxeAnonymousTypeBody.class};
  private static final Class[] MEMBER_TYPES =
    new Class[]{HaxeEnumValueDeclaration.class, HaxeMethod.class,
      HaxeFieldDeclaration.class};

  @NotNull
  private static List<HaxeNamedComponent> getNamedComponents(@Nullable final HaxeClass cls) {
    final PsiElement body = PsiTreeUtil.getChildOfAnyType(cls, BODY_TYPES);
    final List<HaxeNamedComponent> components = new ArrayList<HaxeNamedComponent>();
    if (body != null) {
      final Collection<HaxeNamedComponent> members = PsiTreeUtil.findChildrenOfAnyType(body, MEMBER_TYPES);
      for (HaxeNamedComponent member : members) {
        if (member instanceof HaxeMethod && ((HaxeMethod)member).isConstructor()) {
          continue;
        }
        components.add(member);
      }
    }
    return components;
  }
}
