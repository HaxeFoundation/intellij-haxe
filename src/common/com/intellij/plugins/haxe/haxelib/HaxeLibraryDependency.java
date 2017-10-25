/*
 * Copyright 2017 Eric Bishton
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
package com.intellij.plugins.haxe.haxelib;

import com.intellij.openapi.module.Module;
import com.intellij.plugins.haxe.HaxeBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class HaxeLibraryDependency extends HaxeLibraryReference {
  final private HaxeLibraryList reliants;


  public HaxeLibraryDependency(@NotNull Module module, @NotNull String name, @Nullable String semver) {
    this(module, name, semver, null);
  }

  public HaxeLibraryDependency(@NotNull Module module, @NotNull String name, @Nullable String semver, @Nullable HaxeLibrary reliant) {
    this(HaxelibProjectUpdater.getLibraryCache(module), name, semver, reliant);
  }

  public HaxeLibraryDependency(@NotNull HaxelibLibraryCache owner, @NotNull String name, @Nullable String semver, @Nullable HaxeLibrary reliant) {
    super(owner, name, HaxelibSemVer.create(semver), false);
    reliants = new HaxeLibraryList(owner.getSdk());
    if (null != reliant) {
      this.reliants.add(reliant.createReference());
    }
  }

  public HaxeLibraryDependency(@NotNull HaxelibLibraryCache owner, @NotNull String name, @Nullable String semver, @NotNull HaxeLibraryList reliants) {
    this(owner, name, semver, (HaxeLibrary)null);
    this.reliants.addAll(reliants);
  }

  public HaxeLibraryDependency clone() {
    synchronized (this) {
      return new HaxeLibraryDependency(owner, name, semver.toString(), reliants);
    }
  }

  @Override
  public String toString() {
    return computePresentableName();
  }

  // Note: This was intended to override getPresentableName(), but doing so
  //       makes it difficult to locate the library in the project list.
  @NotNull
  public String computePresentableName() {
    String name = super.getPresentableName();
    List<String> relnames = null;

    synchronized (this) {
      if (!reliants.isEmpty()) {
        final int size = reliants.size(); // Slow call!! Don't repeat it.
        final List<String> namelist = new ArrayList<String>(size);
        reliants.iterate(new HaxeLibraryList.Lambda() {
          @Override
          public boolean processEntry(HaxeLibraryReference entry) {
            namelist.add(entry.getName());
            return true;
          }
        });
        relnames = namelist;
      }
    }

    if (null != relnames && !relnames.isEmpty()) {
      final StringBuilder builder = new StringBuilder(name);
      builder.append(' ');
      builder.append(HaxeBundle.message("haxelib.dependency.list.prefix"));

      int size = relnames.size();
      for (int i = 0; i < size; ++i) {
        builder.append(relnames.get(i));
        if (i < size - 1) {
          builder.append(HaxeBundle.message("haxelib.dependency.list.separator"));
        }
      }
      name = builder.toString();
    }

    return name;
  }

  public HaxeLibraryList getReliants() {
    return reliants;
  }

  public void addReliant(HaxeLibrary reliant) {
    HaxeLibraryReference newRef = reliant.createReference();
    if (null != reliant && !reliants.contains(newRef)) {
      reliants.add(newRef);
    }

  }

  public void addReliant(HaxeLibraryReference reliant) {
    if (null != reliant && !reliants.contains(reliant)) {
      reliants.add(reliant.clone());
    }
  }

  /**
   * Generate a key that matches dependencies.
   * @return
   */
  public String getKey() {
    return getName();
  }

  /**
   * Full member-by-member equivalency.
   * @param o
   * @return
   */
  @Override
  public boolean matches(Object o) {
    if (this == o) return true;
    if (!(o instanceof HaxeLibraryDependency)) return false;
    if (!super.matches(o)) return false;

    HaxeLibraryDependency that = (HaxeLibraryDependency)o;

    return reliants != null ? reliants.equals(that.reliants) : that.reliants == null;
  }
}
