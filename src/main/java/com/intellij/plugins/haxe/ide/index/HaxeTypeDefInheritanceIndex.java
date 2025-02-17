/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Ilya Malanin
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

import com.intellij.openapi.util.Pair;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeTypeDefImpl;
import com.intellij.plugins.haxe.model.HaxeAnonymousTypeModel;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.intellij.plugins.haxe.ide.index.HaxeInheritanceIndexUtil.containsDotSeparator;
import static com.intellij.plugins.haxe.ide.index.HaxeInheritanceIndexUtil.getClassNameCandidate;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeTypeDefInheritanceIndex extends FileBasedIndexExtension<String, List<HaxeClassInfo>> {
  public static final ID<String, List<HaxeClassInfo>> HAXE_TYPEDEF_INHERITANCE_INDEX = ID.create("HaxeTypeDefInheritanceIndex");
  private static final int INDEX_VERSION = HaxeIndexUtil.BASE_INDEX_VERSION + 2;
  private final DataIndexer<String, List<HaxeClassInfo>, FileContent> myIndexer = new MyDataIndexer();
  private final DataExternalizer<List<HaxeClassInfo>> myExternalizer = new HaxeClassInfoListExternalizer();

  @NotNull
  @Override
  public ID<String, List<HaxeClassInfo>> getName() {
    return HAXE_TYPEDEF_INHERITANCE_INDEX;
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
      HaxeModule module = PsiTreeUtil.findChildOfType(psiFile, HaxeModule.class);
      if (module == null) return Collections.emptyMap();

      final PsiElement[] fileChildren = module.getChildren();
      List<PsiElement> filtered = ContainerUtil.filter(fileChildren, element -> element instanceof AbstractHaxeTypeDefImpl);
      final List<AbstractHaxeTypeDefImpl> classes = ContainerUtil.map(filtered, element -> (AbstractHaxeTypeDefImpl)element);

      if (classes.isEmpty()) {
        return Collections.emptyMap();
      }

      final Map<String, List<HaxeClassInfo>> result = new HashMap<>(classes.size());
      final Map<String, String> qNameCache = new HashMap<String, String>();
      for (AbstractHaxeTypeDefImpl haxeTypeDef : classes) {
        String qualifiedName = haxeTypeDef.getQualifiedName();
        if (qualifiedName != null) {
          Pair<String, String> pair = HaxeResolveUtil.splitQName(qualifiedName);
          final HaxeClassInfo value = new HaxeClassInfo(pair.getSecond(), pair.getFirst(), HaxeComponentType.typeOf(haxeTypeDef));
          final HaxeTypeOrAnonymous haxeTypeOrAnonymous = haxeTypeDef.getTypeOrAnonymous();
          final HaxeType type = haxeTypeOrAnonymous == null ? null : haxeTypeOrAnonymous.getType();
          final HaxeAnonymousType anonymousType = haxeTypeOrAnonymous == null ? null : haxeTypeOrAnonymous.getAnonymousType();
          if (anonymousType != null) {
            HaxeAnonymousTypeModel model = (HaxeAnonymousTypeModel) anonymousType.getModel();
            for (HaxeType haxeType : model.getCompositeTypesPsi()) {
              final String classNameCandidate = haxeType.getText();
              final String key = containsDotSeparator(classNameCandidate) ?
                      classNameCandidate :
                      getQNameAndCache(qNameCache, psiFile, classNameCandidate, haxeType);
              put(result, key, value);
            }
          } else if (type != null) {
            final String classNameCandidate = getClassNameCandidate(type);
            final String qName = containsDotSeparator(classNameCandidate) ?
                    classNameCandidate :
                    getQNameAndCache(qNameCache, psiFile, classNameCandidate, type);
            put(result, qName, value);
          }
        }
      }
      return result;
    }

    private static String getQNameAndCache(Map<String, String> qNameCache, PsiFile psiFile, String classNameCandidate, HaxeType haxeType) {
      String result = qNameCache.get(classNameCandidate);
      if (result == null) {
        result = HaxeResolveUtil.getQName(psiFile, classNameCandidate, true, true, haxeType);
        if (result == null) result = classNameCandidate;// fallback so key wont be null
        qNameCache.put(classNameCandidate, result);
      }
      return result;
    }

    private static void put(Map<String, List<HaxeClassInfo>> map, String key, HaxeClassInfo value) {
      List<HaxeClassInfo> infos = map.computeIfAbsent(key, k -> new ArrayList<>());
      infos.add(value);
    }
  }
}
