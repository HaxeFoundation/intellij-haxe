/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
package com.intellij.plugins.haxe.ide.index;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.util.indexing.FileBasedIndex;

public class HaxeSdkInputFilter implements FileBasedIndex.InputFilter {
  public static HaxeSdkInputFilter INSTANCE = new HaxeSdkInputFilter();

  private HaxeSdkInputFilter() {
  }

  @Override
  public boolean acceptInput(VirtualFile file) {
    // ignore std stubs for different platforms
    return file.getFileType() == HaxeFileType.INSTANCE && !"_std".equals(file.getParent().getName());
  }
}

/*public class HaxeSdkInputFilter extends DefaultFileTypeSpecificInputFilter {
  public static HaxeSdkInputFilter INSTANCE = new HaxeSdkInputFilter();

  private HaxeSdkInputFilter() {
    super(HaxeFileType.HAXE_FILE_TYPE);
  }

  @Override
  public boolean acceptInput(VirtualFile file) {
    // ignore std stubs for different platforms
    return !"_std".equals(file.getParent().getName());
  }
}*/
