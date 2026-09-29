package com.intellij.plugins.haxe.util;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.HaxeClassNameUnifiedIndex;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.fqn.HaxeFullyQualifiedClassNameUnifiedIndex;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeBaseMemberModel;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Qualified-name-as-TEXT → PSI element lookups, shared by features that meet
 * dotted names outside code: debugger value links, string-literal navigation.
 */
public final class HaxeQnameResolveUtil {

  private HaxeQnameResolveUtil() {
  }

  /**
   * The class or member a dotted name denotes: the source-level spelling
   * first, then the RUNTIME spelling (runtime names carry no module segment —
   * modules are a compile-time concept). Call in a read action.
   */
  @Nullable
  public static PsiElement findClassOrMember(@NotNull String qname, @NotNull Project project) {
    PsiElement element = HaxeResolveUtil.findClassOrMemberByQName(qname, project);
    if (element != null) return element;
    return resolveRuntimeName(project, qname);
  }

  /**
   * Whether a dotted prefix names something the project knows: a resolvable
   * class/member, or a PACKAGE some class FQN lives under. Drives the
   * "auto-popup only once the typed prefix is real" rule for qualified-name
   * completion in strings. Call in a read action; false while dumb.
   */
  public static boolean isKnownQnamePrefix(@NotNull String prefix, @NotNull Project project) {
    if (DumbService.isDumb(project)) return false;
    if (findClassOrMember(prefix, project) != null) return true;
    String packagePrefix = prefix + ".";
    for (String fqn : HaxeFullyQualifiedClassNameUnifiedIndex.getAllKeys(project)) {
      if (fqn.startsWith(packagePrefix)) return true;
    }
    return false;
  }

  @Nullable
  private static PsiElement resolveRuntimeName(@NotNull Project project, @NotNull String value) {
    FullyQualifiedInfo info = getRuntimeQualifiedInfo(value);
    if (info == null) return null;

    GlobalSearchScope scope = GlobalSearchScope.allScope(project);
    List<HaxeClass> haxeClasses = HaxeClassNameUnifiedIndex.getByNameFiltered(info.className, project, scope)
      .stream()
      .filter(aClass -> {
        String qualifiedName = aClass.getFullyQualifiedName();
        FullyQualifiedInfo withNoModuleName = new FullyQualifiedInfo(qualifiedName).withModuleName(null);
        return withNoModuleName.equals(info.toClassQualifiedName());
      }).toList();

    if (!haxeClasses.isEmpty()) {
      HaxeClass haxeClass = haxeClasses.getFirst();
      if (!info.hasMemberName()) {
        return haxeClass;
      }
      String name = info.getMemberName();
      HaxeBaseMemberModel member = haxeClass.getModel().getMember(name, null);
      if (member != null) {
        return member.getBasePsi();
      }
    }

    return null;
  }

  @Nullable
  private static FullyQualifiedInfo getRuntimeQualifiedInfo(@NotNull String value) {
    // Runtime does not contain module info, so it is dropped and only the class is used
    FullyQualifiedInfo info = new FullyQualifiedInfo(value);
    if (!info.hasModuleName()) return null;
    if (!info.hasClassName()) {
      info = info.withModuleName(null).withClassName(info.moduleName);
    }
    return info;
  }
}
