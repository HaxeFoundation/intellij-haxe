package com.intellij.plugins.haxe.lang.psi.stubs.stub;

import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stub for genericListPart (type parameter declaration).
 * Extends HaxeClassStub so it is compatible with AbstractHaxePsiClass's stub-aware constructor chain.
 * Only the name field carries meaningful data; all other HaxeClassStub fields are set to neutral defaults.
 */
public class HaxeGenericListPartStub extends HaxeClassStub {

  public HaxeGenericListPartStub(StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType,
                                  @Nullable String name) {
    super(parent, elementType,
          name,
          name,                               // qualifiedName = simple name (overridden at PSI level)
          HaxeComponentType.TYPE_PARAMETER.getKey(),
          false,                              // isPrivate
          false,                              // isExtern
          false,                              // isEnum
          new String[0],                      // superTypeNames
          0);                                 // metaFlags
  }
}
