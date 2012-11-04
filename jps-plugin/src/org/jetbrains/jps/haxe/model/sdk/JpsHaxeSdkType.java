package org.jetbrains.jps.haxe.model.sdk;

import com.intellij.plugins.haxe.HaxeCommonBundle;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkAdditionalDataBase;
import com.intellij.plugins.haxe.config.sdk.impl.HaxeSdkAdditionalDataBaseImpl;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.haxe.model.sdk.impl.JpsHaxeSdkAdditionalDataImpl;
import org.jetbrains.jps.model.library.sdk.JpsSdkType;
import org.jetbrains.jps.model.serialization.library.JpsSdkPropertiesSerializer;

/**
 * @author: Fedor.Korotkov
 */
public class JpsHaxeSdkType extends JpsSdkType<JpsHaxeSdkAdditionalData> {
  public static final JpsHaxeSdkType INSTANCE = new JpsHaxeSdkType();

  public static JpsSdkPropertiesSerializer<JpsHaxeSdkAdditionalData> createJpsSdkPropertiesSerializer() {
    return new JpsSdkPropertiesSerializer<JpsHaxeSdkAdditionalData>(HaxeCommonBundle.message("haxe.sdk.name"), INSTANCE) {
      @NotNull
      public JpsHaxeSdkAdditionalData loadProperties(@Nullable final Element propertiesElement) {
        final HaxeSdkAdditionalDataBase sdkData = XmlSerializer.deserialize(propertiesElement, HaxeSdkAdditionalDataBaseImpl.class);
        return new JpsHaxeSdkAdditionalDataImpl(sdkData);
      }

      public void saveProperties(@NotNull final JpsHaxeSdkAdditionalData properties, @NotNull final Element element) {
        XmlSerializer.serializeInto(properties.getSdkData(), element);
      }
    };
  }
}
