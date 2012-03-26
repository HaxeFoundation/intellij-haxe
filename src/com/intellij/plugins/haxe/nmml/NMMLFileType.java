package com.intellij.plugins.haxe.nmml;

import com.intellij.lang.xml.XMLLanguage;
import com.intellij.ide.highlighter.XmlLikeFileType;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author: Fedor.Korotkov
 */
public class NMMLFileType extends XmlLikeFileType {
  public static final NMMLFileType INSTANCE = new NMMLFileType();

  public NMMLFileType() {
    super(XMLLanguage.INSTANCE);
  }

  @NotNull
  @Override
  public String getName() {
    return HaxeBundle.message("nme.nmml");
  }

  @NotNull
  @Override
  public String getDescription() {
    return HaxeBundle.message("nme.nmml.description");
  }

  @NotNull
  @Override
  public String getDefaultExtension() {
    return "nmml";
  }

  @Override
  public Icon getIcon() {
    return HaxeIcons.NMML_ICON_16x16;
  }
}
