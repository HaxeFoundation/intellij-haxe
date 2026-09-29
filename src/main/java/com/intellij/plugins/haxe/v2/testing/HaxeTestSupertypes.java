package com.intellij.plugins.haxe.v2.testing;

import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeClassReferenceModel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The frameworks' shared marker-type test: does a class transitively extend/implement one of the given types. */
final class HaxeTestSupertypes {

  // pathological inheritance chains (macro-built, cyclic through typedefs) stay bounded
  private static final int MAX_SUPERTYPE_DEPTH = 32;

  private HaxeTestSupertypes() {
  }

  /** Walks extends + implements transitively; visited qualified names stop diamond/cyclic chains. */
  static boolean inheritsAny(@NotNull HaxeClassModel model, @NotNull Set<String> markerQualifiedNames) {
    return inheritsAny(model, markerQualifiedNames, new HashSet<>(), 0);
  }

  private static boolean inheritsAny(@NotNull HaxeClassModel model,
                                     @NotNull Set<String> markerQualifiedNames,
                                     @NotNull Set<String> visited,
                                     int depth) {
    if (depth > MAX_SUPERTYPE_DEPTH) return false;

    List<HaxeClassReferenceModel> supers = new ArrayList<>(model.getExtendingTypes());
    supers.addAll(model.getImplementingInterfaces());
    for (HaxeClassReferenceModel superReference : supers) {
      HaxeClassModel superModel = superReference.getHaxeClassModel();
      if (superModel == null) continue;
      String qualifiedName = superModel.haxeClass.getQualifiedName();
      if (qualifiedName == null || !visited.add(qualifiedName)) continue;
      if (markerQualifiedNames.contains(qualifiedName)) return true;
      if (inheritsAny(superModel, markerQualifiedNames, visited, depth + 1)) return true;
    }
    return false;
  }
}
