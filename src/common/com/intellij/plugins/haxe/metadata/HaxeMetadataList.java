/*
 * Copyright 2020 Eric Bishton
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
package com.intellij.plugins.haxe.metadata;

import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataCompileTimeMeta;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataRunTimeMeta;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Maintains a list of metadata entries.  This is the holder, only.  This class
 * does not populate the list.  {@link com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils#getMetadataList(PsiElement)}
 */
public class HaxeMetadataList extends ArrayList<HaxeMeta> {

  public HaxeMetadataList() {
  }

  @NotNull
  private <T extends HaxeMeta> List<T> collect(Class<T> clazz) {
    List<T> list = null;
    for (HaxeMeta meta : this) {
      if (clazz.isInstance(meta)) {
        if (null == list) list = new ArrayList<>();
        list.add((T)meta);
      }
    }
    return null != list ? list : Collections.emptyList();
  }

  @NotNull
  public List<HaxeMetadataCompileTimeMeta> getCompileTimeMeta() {
    return collect(HaxeMetadataCompileTimeMeta.class);
  }

  @NotNull
  public List<HaxeMetadataRunTimeMeta> getRunTimeMeta() {
    return collect(HaxeMetadataRunTimeMeta.class);
  }


  public static HaxeMetadataList EMPTY = new HaxeMetadataList() {
    private final List<HaxeMeta> emptyList = Collections.unmodifiableList(new ArrayList<>());

    @Override
    public int size() {
      return emptyList.size();
    }

    @Override
    public boolean isEmpty() {
      return emptyList.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
      return emptyList.contains(o);
    }

    @NotNull
    @Override
    public Iterator<HaxeMeta> iterator() {
      return emptyList.iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
      return emptyList.toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
      return emptyList.toArray(a);
    }

    @Override
    public boolean add(HaxeMeta meta) {
      return emptyList.add(meta);
    }

    @Override
    public boolean remove(Object o) {
      return emptyList.remove(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
      return emptyList.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends HaxeMeta> c) {
      return emptyList.addAll(c);
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends HaxeMeta> c) {
      return emptyList.addAll(index, c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
      return emptyList.removeAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
      return emptyList.retainAll(c);
    }

    @Override
    public void replaceAll(UnaryOperator<HaxeMeta> operator) {
      emptyList.replaceAll(operator);
    }

    @Override
    public void sort(Comparator<? super HaxeMeta> c) {
      emptyList.sort(c);
    }

    @Override
    public void clear() {
      emptyList.clear();
    }

    @Override
    public boolean equals(Object o) {
      if (!getClass().isInstance(o)) return false;
      return emptyList.equals(o);
    }

    @Override
    public int hashCode() {
      return emptyList.hashCode();
    }

    @Override
    public HaxeMeta get(int index) {
      return emptyList.get(index);
    }

    @Override
    public HaxeMeta set(int index, HaxeMeta element) {
      return emptyList.set(index, element);
    }

    @Override
    public void add(int index, HaxeMeta element) {
      emptyList.add(index, element);
    }

    @Override
    public HaxeMeta remove(int index) {
      return emptyList.remove(index);
    }

    @Override
    public int indexOf(Object o) {
      return emptyList.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
      return emptyList.lastIndexOf(o);
    }

    @NotNull
    @Override
    public ListIterator<HaxeMeta> listIterator() {
      return emptyList.listIterator();
    }

    @NotNull
    @Override
    public ListIterator<HaxeMeta> listIterator(int index) {
      return emptyList.listIterator(index);
    }

    @NotNull
    @Override
    public List<HaxeMeta> subList(int fromIndex, int toIndex) {
      return emptyList.subList(fromIndex, toIndex);
    }

    @Override
    public Spliterator<HaxeMeta> spliterator() {
      return emptyList.spliterator();
    }

    @Override
    public boolean removeIf(Predicate<? super HaxeMeta> filter) {
      return emptyList.removeIf(filter);
    }

    @Override
    public Stream<HaxeMeta> stream() {
      return emptyList.stream();
    }

    @Override
    public Stream<HaxeMeta> parallelStream() {
      return emptyList.parallelStream();
    }

    @Override
    public void forEach(Consumer<? super HaxeMeta> action) {
      emptyList.forEach(action);
    }
  };
}
