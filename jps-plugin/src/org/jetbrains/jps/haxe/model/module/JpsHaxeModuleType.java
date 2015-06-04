/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.jps.haxe.model.module;

import com.intellij.plugins.haxe.module.impl.HaxeModuleSettingsBaseImpl;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.haxe.model.module.impl.JpsHaxeModuleSettingsImpl;
import org.jetbrains.jps.model.ex.JpsElementTypeBase;
import org.jetbrains.jps.model.module.JpsModuleType;
import org.jetbrains.jps.model.serialization.module.JpsModulePropertiesSerializer;

/**
 * @author: Fedor.Korotkov
 */
public class JpsHaxeModuleType extends JpsElementTypeBase<JpsHaxeModuleSettings> implements JpsModuleType<JpsHaxeModuleSettings> {
  public static final JpsHaxeModuleType INSTANCE = new JpsHaxeModuleType();
  private static final String ID = "HAXE_MODULE";

  private JpsHaxeModuleType() {
  }

  public static JpsModulePropertiesSerializer<JpsHaxeModuleSettings> createModulePropertiesSerializer() {
    return new JpsModulePropertiesSerializer<JpsHaxeModuleSettings>(INSTANCE, ID, "HaxeModuleSettingsStorage") {
      @Override
      public JpsHaxeModuleSettings loadProperties(@Nullable final Element componentElement) {
        final HaxeModuleSettingsBaseImpl moduleSettingsBase;
        if (componentElement != null) {
          moduleSettingsBase = XmlSerializer.deserialize(componentElement, HaxeModuleSettingsBaseImpl.class);
        }
        else {
          moduleSettingsBase = new HaxeModuleSettingsBaseImpl();
        }
        return new JpsHaxeModuleSettingsImpl(moduleSettingsBase);
      }

      @Override
      public void saveProperties(@NotNull final JpsHaxeModuleSettings settings, @NotNull final Element componentElement) {
        XmlSerializer.serializeInto(settings.getProperties(), componentElement);
      }
    };
  }
}
