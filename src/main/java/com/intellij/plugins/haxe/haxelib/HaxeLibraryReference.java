/*
 * Copyright 2017-2019 Eric Bishton
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

import com.intellij.openapi.diagnostic.LogLevel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;

import lombok.CustomLog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A library wrapper used in HaxeLibraryLists as created when adding
 * or removing libraries from the module and project library tables.
 *
 * Note that equals on this class denotes that one reference (or subclass)
 * points to the same library as another reference.  NOT that they are
 * the same reference or member-for-member identical.  Use matches() for that.
 */
@CustomLog
public class HaxeLibraryReference {

  static {      // Take this out when finished debugging.
    log.setLevel(LogLevel.DEBUG);
  }

  protected final String name;
  protected final HaxelibLibraryCache owner;
  protected final HaxelibSemVer semver;
  protected final AtomicBoolean isManaged = new AtomicBoolean(false);

  public HaxeLibraryReference(@NotNull Module module, @NotNull String libName, @NotNull HaxelibSemVer semver) {
    // TODO: I don't like stuffing the "isManaged" knowledge into the name.  Find a better way.
    this(module, libName, semver, false);
  }

  public HaxeLibraryReference(@NotNull HaxelibLibraryCache owner, @NotNull String libName, @NotNull HaxelibSemVer semver) {
    this(owner, libName, semver, false);
  }

  public HaxeLibraryReference(@NotNull Module module, @NotNull String libName, @NotNull HaxelibSemVer semver, boolean isManaged) {
    this(HaxelibProjectUpdater.getLibraryCache(module), libName, semver, isManaged);
  }

  public HaxeLibraryReference(@NotNull HaxelibLibraryCache owner, @NotNull String libName, @NotNull HaxelibSemVer semver, boolean isManaged) {
    this.name = HaxelibNameUtil.parseHaxelib(libName);
    this.isManaged.set(isManaged || HaxelibNameUtil.isManagedLibrary(libName)); // NOT this.name, which has already been stripped!
    this.semver = semver;
    this.owner = owner;
  }

  public HaxeLibraryReference clone() {
    synchronized (this) {
      return new HaxeLibraryReference(owner, name, semver, isManaged.get());
    }
  }

  public static HaxeLibraryReference create(@NotNull Module module, @NotNull String name) {
    return create(module.getProject(), name);
  }

  public static HaxeLibraryReference create(@NotNull Project project, @NotNull String name) {
    HaxelibLibraryCache owner = HaxelibProjectUpdater.getLibraryCache(project);
    return create(owner, name);
  }

  public static HaxeLibraryReference create(@NotNull HaxelibLibraryCache owner, @NotNull String name) {
    if (name.isEmpty()) {
      return null;
    }

    if (name.contains(":")) {
      String[] parts = name.split(":");
      if (parts.length > 2) {
        log.warn("Unexpectedly encountered multiple colons in library description.");
      }
      return new HaxeLibraryReference(owner, parts[0], HaxelibSemVer.create(parts[1]));
    }
    return new HaxeLibraryReference(owner, name, HaxelibSemVer.ANY_VERSION);
  }

  /**
   * @return the library that this reference refers to.
   */
  @Nullable
  public HaxeLibrary getLibrary() {
    return owner != null ? owner.getLibrary(name, semver) : null;
  }

  /**
   * @return the undecorated name of the library that this reference refers to.
   */
  @NotNull
  public String getName() {
    return name;
  }

  /**
   * Get the owner of the library.
   *
   * @return the Cache that owns the reference (as given at instantiation).
   */
  @NotNull
  public HaxelibLibraryCache getOwner() {
    return owner;
  }

  /**
   * @return the semantic version number for this reference.  If this is a development library,
   *         the version may be HaxelibSemVer.DEVELOPMENT_VERSION instead of the library's internal version number.
   */
  @NotNull
  public HaxelibSemVer getVersion() {
    return semver;
  }

  /**
   * @return the decorated name of the library that this reference refers to.
   *         The decorated name contains information about whether this object is managed
   *         automatically.
   */
  @NotNull
  public String getPresentableName() {
    StringBuilder bld = new StringBuilder();
    bld.append(isManaged.get() ? HaxelibNameUtil.stringifyHaxelib(name) : name);
    bld.append(':');
    bld.append(semver.toString());
    return bld.toString();
  }

  /**
   * Determine if the library this reference points to is available. (The name and version
   * may exist, but if the lib isn't current, then it won't be available -- see 'haxelib set'.)
   *
   * @return if the library this reference points to is available.
   */
  public boolean isAvailable() {
    return getLibrary() != null;
  }

  /**
   * @return whether or not this reference is managed.
   */
  public boolean isManaged() {
    return isManaged.get();
  }

  /**
   * Mark this entry as managed.
   */
  public void markAsManagedEntry() {
    synchronized (this) {
      isManaged.set(true);
    }
  }

  public String toString() {
    return name + ":" + semver;
  }

  /**
   * Determine if this reference matches the given library, matching name and
   * semantic version.  If class library path matching is required, use
   * getLibrary().matchesIdeaLib().
   *
   * @param lib
   * @return
   */
  public boolean matchesIdeaLib(@Nullable Library lib) {
    if (null == lib) return false;
    String name = lib.getName();
    if (null == name) return false;
    HaxeLibraryReference ref = HaxeLibraryReference.create(getOwner(), name);
    return this.equals(ref);
  }

  /**
   * Exact equivalence -- match every member.
   * @param o
   * @return
   */
  public boolean matches(@Nullable Object o) {
    if (this == o) return true;
    if (!(o instanceof HaxeLibraryReference)) return false;

    HaxeLibraryReference reference = (HaxeLibraryReference)o;

    if (!name.equals(reference.name)) return false;
    if (!owner.equals(reference.owner)) return false;
    if (!semver.equals(reference.semver)) return false;
    return isManaged.equals(reference.isManaged);
  }


  /**
   * Equals for this class is a **SOFT** equivalency.  If two references point to
   * the same library (following haxelib's rules -- see HaxelibSemVer), they are equal.
   */
  @Override
  final public boolean equals(@Nullable Object o) {
    if (this == o) return true;
    if (!(o instanceof HaxeLibraryReference)) return false;

    HaxeLibraryReference reference = (HaxeLibraryReference)o;

    if (!name.equals(reference.name)) return false;
    return semver.matchesRequestedVersion(reference.semver);
  }

  @Override
  final public int hashCode() {
    int result = name.hashCode();
    // result = 31 * result + semver.hashCode();
    return result;
  }
}
