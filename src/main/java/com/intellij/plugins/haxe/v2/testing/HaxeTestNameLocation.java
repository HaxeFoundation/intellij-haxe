package com.intellij.plugins.haxe.v2.testing;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.HaxeClassNameUnifiedIndex;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildClasspaths;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/// Resolves the identifier-shaped test names most frameworks report:
/// `<package with dots replaced by underscores>.<ClassName>[.<methodName>]`
/// (utest's TeamCity spelling, which the other reporters follow), with an
/// EMPTY leading segment for the default package (`.MyTest.testX`) and plain
/// dotted FQNs accepted as-is. Resolution order per name: the whole name as a
/// class (suite rows), then `class.method` split at the last dot (test rows);
/// a class part resolves as a literal FQN, as the underscores-to-dots package
/// variant, and finally through the simple-name index filtered by the
/// reporter's naming (packages that genuinely contain underscores stay
/// ambiguous to the two rewrites).
public final class HaxeTestNameLocation {

  /**
   * The protocol of the converter-injected name hints (reporter-emitted
   * hints carry their own). Lives here rather than on the locator because
   * {@code v2.testing} must not depend on {@code v2.testing.run}.
   */
  public static final String PROTOCOL = "haxe:test";

  /** Carries the run's tests build file on a converter-injected hint: {@code haxe:test://<name>?build=<path>}. */
  public static final String BUILD_HINT = "?build=";

  private HaxeTestNameLocation() {
  }

  /** The class or method the reported name points at, or null. */
  @Nullable
  public static PsiElement resolve(@NotNull String reportedName,
                                   @NotNull Project project,
                                   @NotNull GlobalSearchScope scope) {
    return resolve(reportedName, null, project, scope);
  }

  /** Resolves a {@link #PROTOCOL} hint's path: the reported name, tie-broken by its optional {@link #BUILD_HINT}. */
  @Nullable
  public static PsiElement resolveHint(@NotNull String hintPath,
                                       @NotNull Project project,
                                       @NotNull GlobalSearchScope scope) {
    int marker = hintPath.lastIndexOf(BUILD_HINT);
    if (marker < 0) return resolve(hintPath, null, project, scope);
    String buildFilePath = hintPath.substring(marker + BUILD_HINT.length());
    return resolve(hintPath.substring(0, marker), buildFilePath, project, scope);
  }

  /**
   * The class or method the reported name points at, or null. A name can be
   * carried by classes in SEVERAL sibling projects of one module (default
   * packages especially); {@code buildFilePath} — the run's tests build —
   * breaks the tie in favor of the class living under that build's
   * classpaths.
   */
  @Nullable
  public static PsiElement resolve(@NotNull String reportedName,
                                   @Nullable String buildFilePath,
                                   @NotNull Project project,
                                   @NotNull GlobalSearchScope scope) {
    String name = StringUtil.trimLeading(reportedName, '.');
    List<String> classpathDirs = buildFilePath == null
                                 ? List.of()
                                 : HaxeBuildClasspaths.sourceDirectories(project, buildFilePath);

    HaxeClass wholeNameClass = resolveClass(name, classpathDirs, project, scope);
    if (wholeNameClass != null) return wholeNameClass;

    int lastDot = name.lastIndexOf('.');
    if (lastDot <= 0) return null;
    HaxeClass testClass = resolveClass(name.substring(0, lastDot), classpathDirs, project, scope);
    if (testClass == null) return null;

    PsiMethod method = findMethod(testClass, name.substring(lastDot + 1));
    return method != null ? method : testClass;
  }

  @Nullable
  private static HaxeClass resolveClass(@NotNull String teamcityName,
                                        @NotNull List<String> classpathDirs,
                                        @NotNull Project project,
                                        @NotNull GlobalSearchScope scope) {
    List<HaxeClass> candidates = classCandidates(teamcityName, project, scope);
    if (candidates.isEmpty()) return null;
    if (classpathDirs.isEmpty() || candidates.size() == 1) return candidates.getFirst();
    return candidates.stream()
      .filter(candidate -> underAny(candidate, classpathDirs))
      .findFirst()
      .orElse(candidates.getFirst());
  }

  @NotNull
  private static List<HaxeClass> classCandidates(@NotNull String teamcityName,
                                                 @NotNull Project project,
                                                 @NotNull GlobalSearchScope scope) {
    Set<HaxeClass> candidates = new LinkedHashSet<>();
    PsiManager psiManager = PsiManager.getInstance(project);
    for (String candidate : fqnCandidates(teamcityName)) {
      HaxeClass found = HaxeResolveUtil.findClassByQName(candidate, psiManager, scope);
      if (found != null) candidates.add(found);
    }
    // the index supplies the OTHER classes sharing the name - same FQN in
    // sibling projects, and packages containing real underscores that are
    // indistinguishable from the reporter's dot rewrite
    String simpleName = StringUtil.getShortName(teamcityName);
    HaxeClassNameUnifiedIndex.getByNameFiltered(simpleName, project, scope).stream()
      .filter(candidate -> teamcityName.equals(teamcityNameOf(candidate.getQualifiedName())))
      .forEach(candidates::add);
    return List.copyOf(candidates);
  }

  private static boolean underAny(@NotNull HaxeClass haxeClass, @NotNull List<String> classpathDirs) {
    PsiFile containingFile = haxeClass.getContainingFile();
    VirtualFile file = containingFile != null ? containingFile.getVirtualFile() : null;
    if (file == null) return false;
    String path = file.getPath();
    return classpathDirs.stream().anyMatch(directory -> path.startsWith(directory + "/"));
  }

  @NotNull
  private static List<String> fqnCandidates(@NotNull String teamcityName) {
    List<String> candidates = new ArrayList<>();
    candidates.add(teamcityName);
    String packagePart = StringUtil.getPackageName(teamcityName);
    if (packagePart.indexOf('_') >= 0) {
      candidates.add(packagePart.replace('_', '.') + "." + StringUtil.getShortName(teamcityName));
    }
    return candidates;
  }

  /** The reporter's spelling of a qualified class name: dots in the package become underscores. */
  @Nullable
  public static String teamcityNameOf(@Nullable String qualifiedName) {
    if (qualifiedName == null) return null;
    String packagePart = StringUtil.getPackageName(qualifiedName);
    if (packagePart.isEmpty()) return qualifiedName;
    return packagePart.replace('.', '_') + "." + StringUtil.getShortName(qualifiedName);
  }

  @Nullable
  private static PsiMethod findMethod(@NotNull HaxeClass haxeClass, @NotNull String methodName) {
    for (PsiMethod method : haxeClass.getMethods()) {
      if (methodName.equals(method.getName())) return method;
    }
    return null;
  }
}
