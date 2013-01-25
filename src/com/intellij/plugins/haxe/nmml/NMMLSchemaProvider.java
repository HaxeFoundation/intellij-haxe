package com.intellij.plugins.haxe.nmml;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.xml.XmlSchemaProvider;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;

/**
 * @author: Fedor.Korotkov
 */
public class NMMLSchemaProvider extends XmlSchemaProvider {
  @Override
  public XmlFile getSchema(@NotNull @NonNls String url, @Nullable Module module, @NotNull PsiFile baseFile) {
    final URL resource = NMMLSchemaProvider.class.getResource("/nmml.xsd");
    final VirtualFile fileByURL = VfsUtil.findFileByURL(resource);
    PsiFile result = baseFile.getManager().findFile(fileByURL);
    if (result instanceof XmlFile) {
      return (XmlFile)result.copy();
    }
    return null;
  }

  @Override
  public boolean isAvailable(final @NotNull XmlFile file) {
    return FileUtilRt.extensionEquals(file.getName(), NMMLFileType.INSTANCE.getDefaultExtension());
  }
}
