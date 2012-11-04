package org.jetbrains.jps.haxe.model.sdk;

import com.intellij.plugins.haxe.config.sdk.HaxeSdkAdditionalDataBase;
import org.jetbrains.jps.model.JpsElement;

/**
 * @author: Fedor.Korotkov
 */
public interface JpsHaxeSdkAdditionalData extends HaxeSdkAdditionalDataBase, JpsElement {
  HaxeSdkAdditionalDataBase getSdkData();
}
