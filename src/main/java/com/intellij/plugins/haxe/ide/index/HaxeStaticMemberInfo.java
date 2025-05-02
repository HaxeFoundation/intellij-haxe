package com.intellij.plugins.haxe.ide.index;

import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

@Getter
@EqualsAndHashCode
public class HaxeStaticMemberInfo {
  @NotNull private final String packageName;
  @Nullable private final String className;
  @NotNull private final String moduleName;
  @NotNull private final String memberName;
  @NotNull private final String typeValue;

  @NotNull private final HaxeComponentType type;

  public HaxeStaticMemberInfo(@NotNull String PackageName,
                              @NotNull String moduleName,
                              @Nullable String className,
                              @NotNull String memberName,
                              @NotNull HaxeComponentType type,
                              String typeValue) {

    this.packageName = PackageName;
    this.moduleName = moduleName;
    this.className = className;
    this.memberName = memberName;
    this.type = type;
    this.typeValue = typeValue != null ? typeValue : "";
  }


  @NotNull
  public Icon getIcon() {
    return type.getIcon();
  }

  public FullyQualifiedInfo toFullyQualifiedInfo() {
   return new FullyQualifiedInfo(packageName, moduleName, className, memberName);
  }
}
