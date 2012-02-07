package com.intellij.plugins.haxe.config.sdk;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.projectRoots.AdditionalDataConfigurable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.plugins.haxe.config.sdk.ui.NekoConfigurablePanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author: Fedor.Korotkov
 */
public class NekoConfigurable implements AdditionalDataConfigurable {
  private final NekoConfigurablePanel myNekoConfigurablePanel;
  private Sdk mySdk;

  public NekoConfigurable() {
    myNekoConfigurablePanel = new NekoConfigurablePanel();
  }

  @Override
  public void setSdk(Sdk sdk) {
    mySdk = sdk;
  }

  @Override
  public JComponent createComponent() {
    return myNekoConfigurablePanel.getPanel();
  }

  @Override
  public boolean isModified() {
    final HaxeSdkData haxeSdkData = getHaxeSdkData();
    return haxeSdkData == null || !myNekoConfigurablePanel.getNekoBinPath().equals(haxeSdkData.getNekoBinPath());
  }

  @Override
  public void apply() throws ConfigurationException {
    final HaxeSdkData haxeSdkData = getHaxeSdkData();
    if (haxeSdkData == null) {
      return;
    }

    final HaxeSdkData newData = new HaxeSdkData(haxeSdkData.getHomePath(), haxeSdkData.getVersion());
    newData.setNekoBinPath(myNekoConfigurablePanel.getNekoBinPath());

    final SdkModificator modificator = mySdk.getSdkModificator();
    modificator.setSdkAdditionalData(newData);
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      public void run() {
        modificator.commitChanges();
      }
    });
  }

  @Nullable
  private HaxeSdkData getHaxeSdkData() {
    return mySdk.getSdkAdditionalData() instanceof HaxeSdkData ? (HaxeSdkData)mySdk.getSdkAdditionalData() : null;
  }

  @Override
  public void reset() {
    if (getHaxeSdkData() != null) {
      myNekoConfigurablePanel.setNekoBinPath(getHaxeSdkData().getNekoBinPath());
    }
    myNekoConfigurablePanel.getPanel().repaint();
  }

  @Override
  public void disposeUIResources() {
  }
}
