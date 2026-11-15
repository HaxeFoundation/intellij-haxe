package com.intellij.plugins.haxe.buildsystem.lime;

import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.ArrayUtil;
import com.intellij.xml.DefaultXmlExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Binds the prefixes lime project files use without declaring them. Lime reads
 * an element like {@code <config:android>} as a config section named after the
 * colon, and copies {@code android:}-prefixed attributes of the section's
 * {@code <application>} and {@code <activity>} into the Android manifest as
 * they are. {@link #CONFIG_NAMESPACE} has a schema ({@link LimeXmlSchemaProvider});
 * the Android namespace only keeps its prefix from being reported as unbound.
 */
public class LimeXmlExtension extends DefaultXmlExtension {

  public static final String CONFIG_PREFIX = "config";
  /** A namespace only the plugin uses; files never spell it out. */
  public static final String CONFIG_NAMESPACE = "urn:intellij-haxe:lime-config";

  public static final String ANDROID_PREFIX = "android";
  public static final String ANDROID_NAMESPACE = "urn:intellij-haxe:lime-android";

  @Override
  public boolean isAvailable(PsiFile file) {
    return file instanceof XmlFile xmlFile && LimeOpenFlUtil.isLimeDialectFile(xmlFile);
  }

  @Override
  public String @Nullable [][] getNamespacesFromDocument(@NotNull XmlDocument parent, boolean declarationsExist) {
    String[][] namespaces = super.getNamespacesFromDocument(parent, declarationsExist);
    String[][] implicit = {{CONFIG_PREFIX, CONFIG_NAMESPACE}, {ANDROID_PREFIX, ANDROID_NAMESPACE}};
    return namespaces == null ? implicit : ArrayUtil.mergeArrays(namespaces, implicit);
  }
}
