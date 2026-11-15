package com.intellij.plugins.haxe.buildsystem.lime;

import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.ArrayUtil;
import com.intellij.xml.DefaultXmlExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Binds the {@code config} prefix in lime project files. Lime reads an element
 * like {@code <config:android>} as a config section named after the colon and
 * never declares the prefix, so the IDE would report it as unbound. The prefix
 * is bound to {@link #CONFIG_NAMESPACE}, whose schema
 * {@link LimeXmlSchemaProvider} supplies.
 */
public class LimeXmlExtension extends DefaultXmlExtension {

  public static final String CONFIG_PREFIX = "config";
  /** A namespace only the plugin uses; files never spell it out. */
  public static final String CONFIG_NAMESPACE = "urn:intellij-haxe:lime-config";

  @Override
  public boolean isAvailable(PsiFile file) {
    return file instanceof XmlFile xmlFile && LimeOpenFlUtil.isLimeDialectFile(xmlFile);
  }

  @Override
  public String @Nullable [][] getNamespacesFromDocument(@NotNull XmlDocument parent, boolean declarationsExist) {
    String[][] namespaces = super.getNamespacesFromDocument(parent, declarationsExist);
    String[] config = {CONFIG_PREFIX, CONFIG_NAMESPACE};
    return namespaces == null ? new String[][]{config} : ArrayUtil.append(namespaces, config);
  }
}
