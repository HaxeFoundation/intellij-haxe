package org.jetbrains.jps.haxe.model.module;

import com.intellij.plugins.haxe.module.impl.HaxeModuleSettingsBaseImpl;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.haxe.model.module.impl.JpsHaxeModuleSettingsImpl;
import org.jetbrains.jps.model.module.JpsModuleType;
import org.jetbrains.jps.model.serialization.module.JpsModulePropertiesSerializer;

/**
 * @author: Fedor.Korotkov
 */
public class JpsHaxeModuleType extends JpsModuleType<JpsHaxeModuleSettings> {
  public static final JpsHaxeModuleType INSTANCE = new JpsHaxeModuleType();
  private static final String ID = "HAXE_MODULE";

  private JpsHaxeModuleType() {
  }

  public static JpsModulePropertiesSerializer<JpsHaxeModuleSettings> createModulePropertiesSerializer() {
    return new JpsModulePropertiesSerializer<JpsHaxeModuleSettings>(INSTANCE, ID, "HaxeModuleSettingsStorage") {
      @Override
      public JpsHaxeModuleSettings loadProperties(@Nullable final Element componentElement) {
        final HaxeModuleSettingsBaseImpl moduleSettingsBase = XmlSerializer.deserialize(componentElement, HaxeModuleSettingsBaseImpl.class);
        return new JpsHaxeModuleSettingsImpl(moduleSettingsBase);
      }

      @Override
      public void saveProperties(@NotNull final JpsHaxeModuleSettings settings, @NotNull final Element componentElement) {
        XmlSerializer.serializeInto(settings.getProperties(), componentElement);
      }
    };
  }
}
