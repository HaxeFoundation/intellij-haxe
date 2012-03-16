package com.intellij.plugins.haxe.lang.psi;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public interface HaxeClass extends HaxeComponent {
  HaxeClass[] EMPTY_ARRAY = new HaxeClass[0];

  @Nullable
  @NonNls
  String getQualifiedName();

  @NotNull
  List<HaxeType> getExtendsList();

  @NotNull
  List<HaxeType> getImplementsList();

  boolean isInterface();

  @NotNull
  List<HaxeNamedComponent> getMethods();

  @NotNull
  List<HaxeNamedComponent> getFields();

  @Nullable
  HaxeNamedComponent findFieldByName(@NotNull final String name);

  @Nullable
  HaxeNamedComponent findMethodByName(@NotNull final String name);

  @Nullable
  HaxeNamedComponent findMethodBySignature(@NotNull final String name, List<HaxeType> parameterTypes);
}
