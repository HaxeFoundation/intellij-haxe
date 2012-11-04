package org.jetbrains.jps.haxe.model.sdk.impl;

import com.intellij.plugins.haxe.config.sdk.HaxeSdkAdditionalDataBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.haxe.model.sdk.JpsHaxeSdkAdditionalData;
import org.jetbrains.jps.model.ex.JpsElementBase;

/**
 * @author: Fedor.Korotkov
 */
public class JpsHaxeSdkAdditionalDataImpl extends JpsElementBase<JpsHaxeSdkAdditionalDataImpl> implements JpsHaxeSdkAdditionalData {
  private HaxeSdkAdditionalDataBase myAdditionalData;

  public JpsHaxeSdkAdditionalDataImpl(HaxeSdkAdditionalDataBase additionalData) {
    myAdditionalData = additionalData;
  }

  @Override
  public HaxeSdkAdditionalDataBase getSdkData() {
    return myAdditionalData;
  }

  @Override
  public String getHomePath() {
    return myAdditionalData.getHomePath();
  }

  @Override
  public String getVersion() {
    return myAdditionalData.getVersion();
  }

  @Override
  public String getNekoBinPath() {
    return myAdditionalData.getNekoBinPath();
  }

  @Override
  public void setNekoBinPath(String nekoBinPath) {
    myAdditionalData.setNekoBinPath(nekoBinPath);
  }

  @Override
  public String getHaxelibPath() {
    return myAdditionalData.getHaxelibPath();
  }

  @Override
  public void setHaxelibPath(String haxelibPath) {
    myAdditionalData.setHaxelibPath(haxelibPath);
  }

  @NotNull
  @Override
  public JpsHaxeSdkAdditionalDataImpl createCopy() {
    return new JpsHaxeSdkAdditionalDataImpl(myAdditionalData);
  }

  @Override
  public void applyChanges(@NotNull JpsHaxeSdkAdditionalDataImpl modified) {
    myAdditionalData = modified.myAdditionalData;
  }
}
