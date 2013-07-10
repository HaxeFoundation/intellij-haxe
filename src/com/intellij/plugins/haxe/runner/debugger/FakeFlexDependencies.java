package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.lang.javascript.flex.projectStructure.model.*;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by todd on 7/10/13.
 */

public class FakeFlexDependencies implements Dependencies {

  private static final Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.config.sdk.HaxeSdkUtil");
  private static final DependencyEntry[] NO_DEPENDENCIES = {};

  @Nullable
  public SdkEntry getSdkEntry() {
    LOG.info("FakeFlexDependencies::getSdkEntry");
    return null;
  }

  public DependencyEntry[] getEntries() {
    LOG.info("FakeFlexDependencies::getEntries");
    return NO_DEPENDENCIES;
  }

  @NotNull
  public com.intellij.flex.model.bc.LinkageType getFrameworkLinkage()
  {
    LOG.info("FakeFlexDependencies::getFrameworkLinkage");
    return null;
  }

  @NotNull
  public java.lang.String getTargetPlayer() {
    LOG.info("FakeFlexDependencies::getTargetPlayer");
    return null;
  }

  @NotNull
  public com.intellij.flex.model.bc.ComponentSet getComponentSet()
  {
    LOG.info("FakeFlexDependencies::getComponentSet");
    return null;
  }

}
