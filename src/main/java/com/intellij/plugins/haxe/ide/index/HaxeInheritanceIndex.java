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

import com.intellij.openapi.util.Condition;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeType;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeTypeDefImpl;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeInheritanceIndex extends FileBasedIndexExtension<String, List<HaxeClassInfo>> {
  public static final ID<String, List<HaxeClassInfo>> HAXE_INHERITANCE_INDEX = ID.create("HaxeInheritanceIndex");
  private static final int INDEX_VERSION = HaxeIndexUtil.BASE_INDEX_VERSION + 7;
  private final DataIndexer<String, List<HaxeClassInfo>, FileContent> myIndexer = new MyDataIndexer();
  private final DataExternalizer<List<HaxeClassInfo>> myExternalizer = new HaxeClassInfoListExternalizer();

  @NotNull
  @Override
  public ID<String, List<HaxeClassInfo>> getName() {
    return HAXE_INHERITANCE_INDEX;
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
  public DataExternalizer<List<HaxeClassInfo>> getValueExternalizer() {
    return myExternalizer;
  }

  @Override
  public FileBasedIndex.InputFilter getInputFilter() {
    return HaxeSdkInputFilter.INSTANCE;
  }

  @NotNull
  @Override
  public DataIndexer<String, List<HaxeClassInfo>, FileContent> getIndexer() {
    return myIndexer;
  }

  private static class MyDataIndexer implements DataIndexer<String, List<HaxeClassInfo>, FileContent> {
    @Override
    @NotNull
    public Map<String, List<HaxeClassInfo>> map(final FileContent inputData) {
      final PsiFile psiFile = inputData.getPsiFile();
      final PsiElement[] fileChildren = psiFile.getChildren();
      final List<HaxeClass> classes = ContainerUtil.map(ContainerUtil.filter(fileChildren, new Condition<PsiElement>() {
        @Override
        public boolean value(PsiElement element) {
          return element instanceof HaxeClass && !(element instanceof AbstractHaxeTypeDefImpl);
        }
      }), new Function<PsiElement, HaxeClass>() {
        @Override
        public HaxeClass fun(PsiElement element) {
          return (HaxeClass)element;
        }
      });
      if (classes.isEmpty()) {
        return Collections.emptyMap();
      }
      final Map<String, List<HaxeClassInfo>> result = new HashMap<String, List<HaxeClassInfo>>(classes.size());
      final Map<String, String> qNameCache = new HashMap<String, String>();
      for (HaxeClass haxeClass : classes) {
        final HaxeClassInfo value = new HaxeClassInfo(haxeClass.getQualifiedName(), HaxeComponentType.typeOf(haxeClass));
        for (HaxeType haxeType : haxeClass.getHaxeExtendsList()) {
          if (haxeType == null) continue;
          final String classNameCandidate = haxeType.getText();
          final String key = classNameCandidate.indexOf('.') != -1 ?
                             classNameCandidate :
                             getQNameAndCache(qNameCache, fileChildren, classNameCandidate);
          put(result, key, value);
        }
        for (HaxeType haxeType : haxeClass.getHaxeImplementsList()) {
          if (haxeType == null) continue;
          final String classNameCandidate = haxeType.getText();
          final String key = classNameCandidate.indexOf('.') != -1 ?
                             classNameCandidate :
                             getQNameAndCache(qNameCache, fileChildren, classNameCandidate);
          put(result, key, value);
        }
      }
      return result;
    }

    private static String getQNameAndCache(Map<String, String> qNameCache, PsiElement[] fileChildren, String classNameCandidate) {
      String result = qNameCache.get(classNameCandidate);
      if (result == null) {
        result = HaxeResolveUtil.getQName(fileChildren, classNameCandidate, true);
        qNameCache.put(classNameCandidate, result);
      }
      return result;
    }

    private static void put(Map<String, List<HaxeClassInfo>> map, String key, HaxeClassInfo value) {
      List<HaxeClassInfo> infos = map.get(key);
      if (infos == null) {
        infos = new ArrayList<HaxeClassInfo>();
        map.put(key, infos);
      }
      infos.add(value);
    }
  }
}
