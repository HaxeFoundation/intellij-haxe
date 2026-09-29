/*
 * Copyright 2017-2018 Ilya Malanin
 * Copyright 2019 Eric Bishton
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
package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.util.HaxeFileUtil;
import com.intellij.plugins.haxe.util.HaxeModuleVariants;
import com.intellij.plugins.haxe.util.HaxeNameUtils;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;

public class HaxePackageModel implements HaxeExposableModel {
  private final HaxeProjectModel project;
  protected final HaxeSourceRootModel root;
  private final HaxePackageModel parent;
  private final String name;
  protected final String path;
  private final FullyQualifiedInfo qualifiedInfo;


  public HaxePackageModel(@NotNull HaxeSourceRootModel root,
                          @NotNull String name,
                          @Nullable HaxePackageModel parent) {
    this.project = root.project;
    this.name = name;
    this.root = root;
    this.parent = parent;

    if (parent != null && !parent.path.isEmpty()) {
      path = parent.path + '.' + name;
    } else {
      path = name;
    }

    qualifiedInfo = new FullyQualifiedInfo(path, null, null, null);
  }

  public HaxeProjectModel getProject() {
    return project;
  }

  public String getName() {
    return name;
  }

  public HaxeModel resolve(String fullyQualifiedName) {
    return resolve(new FullyQualifiedInfo(fullyQualifiedName));
  }

  public HaxeModel resolve(FullyQualifiedInfo info) {
    if (info.packageName.equals(this.path)) {
      if (info.moduleName == null && info.className == null) return this;
      HaxeFileModel file = getFileModel(info.moduleName);
      if (file != null) return file.resolve(info);
      return null;
    } else if (info.packageName.indexOf(path) == 0 || path.isEmpty()) {
      String searchName = path.isEmpty() ? info.packageName : info.packageName.substring(path.length() + 1);
      HaxePackageModel child = getChild(searchName);
      if (child != null) {
        return child.resolve(info);
      }
      return null;
    }
    return null;
  }

  @Nullable
  public HaxePackageModel getChild(@NotNull String name) {
    if (name.isEmpty()) {
      return this;
    }

    int index = name.indexOf('.');
    if (-1 == index) {
      PsiDirectory directory = root.access(path.isEmpty() ? name : path + '.' + name);
      if (directory != null) {
        return new HaxePackageModel(root, name, this);
      }
      return null;
    }

    HaxePackageModel child = new HaxePackageModel(root, name.substring(0,index), this);
    return child.getChild(name.substring(index+1));
  }


  @NotNull
  public List<HaxePackageModel> getChildren() {
    PsiDirectory directory = root.access(path);
    if (directory != null) {
      return Arrays.stream(directory.getSubdirectories())
        .map(subDirectory -> new HaxePackageModel(root, subDirectory.getName(), this))
        .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  @Nullable
  public HaxeFileModel getFileModel(String fileName) {
    final HaxeFile file = getFile(fileName);
    return file != null && file.isValid() ? HaxeFileModel.fromElement(file) : null;
  }

  protected HaxeFile getFile(String filePath) {
    List<String> parts = HaxeFileUtil.splitPath(filePath);
    String fname = parts.getLast();
    if (fname == null || fname.isEmpty()) return null;

    String packagePath = HaxeFileUtil.joinPath(parts.subList(0, parts.size() - 1));
    String accessPath = null != packagePath && !packagePath.isEmpty() ? HaxeFileUtil.joinPath(path, packagePath) : path;
    try {
      // target-specific files ("MyClass.js.hx") can shadow plain normal files ("MyClass.hx") when a target is active.
      HaxeFile variantFile = findActiveVariantFileByIndex(fname, accessPath);
      if (variantFile != null) return variantFile;

      return findFileByIndex(fname, accessPath);
    }
    catch (IndexNotReadyException e) {
      // dumb mode: indexes unavailable, walk the directories instead
      return findFileByDirectoryWalk(fname, accessPath);
    }
  }

  @Nullable
  private HaxeFile findActiveVariantFileByIndex(String fname, String accessPath) {
    if (project == null) return null;
    for (String variant : HaxeModuleVariants.activeVariants(project.getProject())) {
      HaxeFile file = findFileByIndex(fname + '.' + variant, accessPath);
      if (file != null) return file;
    }
    return null;
  }

  /**
   * Index-backed lookup: one FilenameIndex query with candidates matched to
   * this root, then to any project root since some libs share package names,
   * by relative path. Much cheaper than walking subdirectories per package
   * segment per source root.
   * <p>
   * An empty candidate set is a definitive miss: every root the models serve
   * is an order-entry root (module source roots and library classes roots via
   * OrderEnumerator, SDK source roots for std - see HaxeProjectModel's
   * RootsCache), and the platform indexes all of those under allScope. The
   * only in-scope-but-unindexed states are indexes not yet built (dumb mode,
   * handled by the IndexNotReadyException fallback in {@link #getFile}) and
   * user-excluded subtrees, which platform resolve ignores everywhere.
   */
  @Nullable
  private HaxeFile findFileByIndex(String fname, String accessPath) {
    if (project == null) return null;
    Collection<VirtualFile> candidates = HaxeFilenameCandidateCache.getInstance(project.getProject()).candidatesFor(fname + ".hx");
    if (candidates.isEmpty()) return null;

    String relative = relativeFilePath(fname, accessPath);
    VirtualFile found = findInRoot(root, relative, candidates);
    if (found != null) return asHaxeFile(found);

    // scan all source roots in project order (some libs share package names)
    for (HaxeSourceRootModel other : project.getRoots()) {
      found = findInRoot(other, relative, candidates);
      if (found != null) return asHaxeFile(found);
    }
    return null;
  }

  /**
   * One VFS descent instead of a relative-path walk per candidate. Candidate
   * membership doubles as the exact-name check: on a case-insensitive
   * filesystem the descent can return a case-mismatched file, which the
   * index never lists under this name.
   */
  @Nullable
  private static VirtualFile findInRoot(HaxeSourceRootModel rootModel, String relative, Collection<VirtualFile> candidates) {
    if (rootModel.root == null) return null;
    VirtualFile file = rootModel.root.findFileByRelativePath(relative);
    return file != null && candidates.contains(file) ? file : null;
  }

  private static @NotNull String relativeFilePath(String fname, String accessPath) {
    // package paths mix '.' and '/' separators depending on the caller
    return accessPath == null || accessPath.isEmpty()
           ? fname + ".hx"
           : accessPath.replace('.', '/') + '/' + fname + ".hx";
  }

  @Nullable
  private HaxeFile asHaxeFile(VirtualFile file) {
    PsiFile psi = PsiManager.getInstance(project.getProject()).findFile(file);
    return psi instanceof HaxeFile haxeFile && haxeFile.isValid() ? haxeFile : null;
  }

  @Nullable
  private HaxeFile findFileByDirectoryWalk(String fname, String accessPath) {
    PsiDirectory directory = root.access(accessPath);
    if (directory != null && directory.isValid()) {
      PsiFile file = directory.findFile(fname + ".hx");
      if (file != null && file.isValid() && file instanceof HaxeFile haxeFile) {
        return haxeFile;
      }
    }
    // scan all source roots (some libs share package names across libs)
    if (project == null) return null;
    for (HaxeSourceRootModel rootModel : project.getRoots()) {
      directory = rootModel.access(accessPath);
      if (directory != null && directory.isValid()) {
        PsiFile file = directory.findFile(fname + ".hx");
        if (file != null && file.isValid() && file instanceof HaxeFile haxeFile) {
          return haxeFile;
        }
      }
    }
    return null;
  }

  @Nullable
  public HaxeClassModel getClassModel(@NotNull String className) {
    String fileName = HaxeNameUtils.classNameToFileName(className);
    HaxeFileModel file = getFileModel(fileName);
    if (file != null) {
      return file.getClassModel(className);
    }
    return null;
  }

  @NotNull
  @Override
  public List<HaxeModel> getExposedMembers() {
    PsiDirectory directory = root.access(path);
    if (directory != null) {
      PsiFile[] files = directory.getFiles();

      List<HaxeModel>  result = new ArrayList<>();
      for(PsiFile file : files) {
        if( file instanceof HaxeFile) {
          HaxeFileModel fileModel = HaxeFileModel.fromElement(file);
          if(fileModel != null)result.addAll(fileModel.getExposedMembers());
        }
      }
      return result;
    }
    return Collections.emptyList();
  }

  /**
   * The main class of every module in this package. Cached on the package
   * directory, since the same-package check runs this for every reference no
   * earlier check resolved. Invalidates on ANY PSI change
   * (global modification count) - a PsiDirectory has no per-directory
   * timestamp, so finer dependencies cannot exist; the global count also
   * covers files added to or removed from the directory.
   */
  @NotNull
  public List<HaxeModel> getModulesMainClass() {
    PsiDirectory directory = root.access(path);
    if (directory == null) return Collections.emptyList();
    return CachedValuesManager.getCachedValue(directory, () -> modulesMainClassResult(directory));
  }

  private static CachedValueProvider.Result<List<HaxeModel>> modulesMainClassResult(PsiDirectory directory) {
    // one entry per module: a target-specific file and its plain sibling both
    // declare the same module, so only the ladder's winner is that module here
    Map<String, HaxeClassModel> winners = new LinkedHashMap<>();
    Map<String, Integer> ranks = new HashMap<>();
    for (PsiFile file : directory.getFiles()) {
      if (!(file instanceof HaxeFile haxeFile)) continue;

      HaxeFileModel fileModel = HaxeFileModel.fromElement(haxeFile);
      if (fileModel == null) continue;

      HaxeClassModel mainClassModel = fileModel.getMainClassModel();
      if (mainClassModel == null) continue;

      String module = fileModel.getName();
      int rank = variantRank(haxeFile);
      Integer existing = ranks.get(module);
      if (existing == null || rank > existing) {
        winners.put(module, mainClassModel);
        ranks.put(module, rank);
      }
    }
    List<HaxeModel> result = List.copyOf(winners.values());
    return CachedValueProvider.Result.create(result, PsiModificationTracker.MODIFICATION_COUNT);
  }

  /**
   * The compiler's platform file selection as a rank: an ACTIVE target-specific
   * file ("Net.js.hx" while js compiles) shadows the plain "Net.hx"; an
   * inactive one only stands in when no other file declares the module.
   */
  private static int variantRank(@NotNull HaxeFile file) {
    String variant = HaxeModuleVariants.variantOf(file);
    if (variant == null) return 1;
    return HaxeModuleVariants.isActive(variant, file.getProject()) ? 2 : 0;
  }

  @Override
  public PsiElement getBasePsi() {
    return JavaPsiFacade.getInstance(this.project.getProject()).findPackage(path);
  }

  @Nullable
  @Override
  public HaxeExposableModel getExhibitor() {
    if (parent != null) {
      return parent;
    }

    return null;
  }

  @Nullable
  @Override
  public FullyQualifiedInfo getQualifiedInfo() {
    return qualifiedInfo;
  }

  @Override
  public boolean isValid() {
    return true;
  }

  public HaxeSourceRootModel getRoot() {
    return root;
  }

  public HaxePackageModel getParent() {
    return parent;
  }
}