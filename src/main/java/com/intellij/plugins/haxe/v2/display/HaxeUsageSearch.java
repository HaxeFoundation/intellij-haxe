package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataCompileTimeMeta;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * Answers "is this declaration used?" from static reference search and the
 * compiler's post-macro knowledge together. Static search runs first and
 * stops at the FIRST hit. Only a member without a static reference goes on
 * to the compiler ({@link HaxeCompilerUsageService}), which also sees usages
 * that exist only in generated code.
 *
 * The answer has three states because the compiler side answers only from
 * its cache under the read lock. "No reference found" splits into UNUSED
 * (the compiler confirmed it) and UNKNOWN (the verdict is still being
 * fetched; the caller's static conclusion stands for this pass and is
 * corrected on the next).
 */
public final class HaxeUsageSearch {

  public enum UsageState {
    /** A reference exists, found statically or by the compiler. */
    USED,
    /** The compiler confirmed there are no references, generated code included. */
    UNUSED,
    /** No static reference and no compiler verdict yet; the caller's static conclusion applies. */
    UNKNOWN
  }

  // TODO: processReferences(declaration, processor) - enumerate static and
  //  compiler locations merged and deduplicated, for Find Usages and Safe
  //  Delete; it may block on the network, so it belongs on a progress thread.

  private HaxeUsageSearch() {
  }

  /** Call in a read action; never blocks on the network. */
  @NotNull
  public static UsageState usageState(@NotNull HaxeNamedComponent declaration) {
    return usageState(declaration, GlobalSearchScope.projectScope(declaration.getProject()));
  }

  /** As above with a caller-chosen static search scope (locals search their enclosing scope only). */
  @NotNull
  public static UsageState usageState(@NotNull HaxeNamedComponent declaration, @NotNull SearchScope scope) {
    if (ReferencesSearch.search(declaration, scope, false).findFirst() != null) {
      return UsageState.USED;
    }
    return HaxeCompilerUsageService.getInstance(declaration.getProject()).usageState(declaration);
  }

  /**
   * Whether "unused" reporting must stay quiet about the declaration: true
   * only for USED, which includes usages the compiler found in generated
   * code. UNKNOWN does NOT count as used, so the caller's static conclusion
   * stands until the compiler's verdict arrives.
   */
  public static boolean isConsideredUsed(@NotNull HaxeNamedComponent declaration) {
    return usageState(declaration) == UsageState.USED;
  }

  /** As above with a caller-chosen static search scope. */
  public static boolean isConsideredUsed(@NotNull HaxeNamedComponent declaration, @NotNull SearchScope scope) {
    return usageState(declaration, scope) == UsageState.USED;
  }

  /**
   * Whether the declaration's metadata should exempt it from "unused"
   * warnings. Registry-known metadata (except {@code @:deprecated}) may be
   * consumed invisibly: a macro can wire up a handler through its metadata,
   * and even the compiler's reference search then reports zero usages. Metadata
   * the registry does NOT know is likely a typo and keeps nothing alive; a
   * user whose custom metadata is misjudged adds {@code @:keep}. While the
   * registry has not loaded, any metadata counts.
   */
  public static boolean metadataKeepsAlive(@NotNull HaxeNamedComponent declaration) {
    List<String> names = new ArrayList<>();
    for (HaxeMeta meta : declaration.getMetadataList(HaxeMetadataCompileTimeMeta.class)) {
      HaxeMetadataType type = meta.getType();
      if (type != null) {
        names.add(type.getText());
      }
    }
    // deprecation documents a member, it does not use it
    names.remove("deprecated");
    if (names.isEmpty()) return false;

    VirtualFile virtualFile = HaxeCompilerDisplayService.physicalFileOf(declaration);
    if (virtualFile == null) return true;
    Set<String> known = HaxeCompilerMetadataService.getInstance(declaration.getProject()).knownBareNames(virtualFile);
    if (known == null) return true;
    return names.stream().anyMatch(known::contains);
  }
}
