package org.jetbrains.jps.haxe.model.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.haxe.model.module.JpsHaxeModuleType;
import org.jetbrains.jps.haxe.model.sdk.JpsHaxeSdkType;
import org.jetbrains.jps.model.serialization.JpsModelSerializerExtension;
import org.jetbrains.jps.model.serialization.library.JpsSdkPropertiesSerializer;
import org.jetbrains.jps.model.serialization.module.JpsModulePropertiesSerializer;

import java.util.Collections;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class JpsHaxeModelSerializerExtension extends JpsModelSerializerExtension {
  @NotNull
  @Override
  public List<? extends JpsSdkPropertiesSerializer<?>> getSdkPropertiesSerializers() {
    return Collections.singletonList(JpsHaxeSdkType.createJpsSdkPropertiesSerializer());
  }

  @NotNull
  public List<? extends JpsModulePropertiesSerializer<?>> getModulePropertiesSerializers() {
    return Collections.singletonList(JpsHaxeModuleType.createModulePropertiesSerializer());
  }
}
