/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
