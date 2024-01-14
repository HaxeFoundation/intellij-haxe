package com.intellij.plugins.haxe.haxelib.definitions.tags;

import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value
public class ProjectXmlHaxelibValue extends ProjectXmlConditionalValue {
  String name;
  String version;


  public ProjectXmlHaxelibValue(@NotNull String name,
                                @Nullable String version,
                                @Nullable String ifCondition,
                                @Nullable String unlessCondition) {
    super(ifCondition, unlessCondition);
    this.name = name;
    this.version = version;
  }
}
