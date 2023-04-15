/*
 * Copyright 2017-2018 Eric Bishton
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
package com.intellij.plugins.haxe.compilation;

import com.intellij.openapi.diagnostic.LogLevel;
import com.intellij.openapi.module.Module;
import com.intellij.plugins.haxe.hxml.HXMLFileType;
import com.intellij.plugins.haxe.hxml.model.HXMLProjectModel;

import com.intellij.psi.*;
import lombok.CustomLog;
import org.apache.log4j.Level;

import java.util.List;

/**
 * Utility functions for working with OpenFL/Lime project files.
 *
 * Created by ebishton on 9/8/2017.
 */
@CustomLog
public class LimeUtil {

  static {
    log.setLevel(LogLevel.INFO);
  }

  private static final StringBuffer EMPTY_STRINGBUFFER = new StringBuffer("\n");

  public static HXMLProjectModel getLimeProjectModel(Module module, boolean useDebugConfig) {

    HaxeCompilerServices cs = new HaxeCompilerServices(new HaxeCompilerUtil.ErrorNotifier(){
      public void notifyError(String message) {
        log.info(message);
      }
    });

    CharSequence displayData = concatList(cs.getLimeProjectConfiguration(module, useDebugConfig, null));

    PsiFile psi = PsiFileFactory.getInstance(module.getProject()).createFileFromText(
      "Lime.Display.Temp." + HXMLFileType.DEFAULT_EXTENSION, HXMLFileType.INSTANCE, displayData);

    return new HXMLProjectModel(psi);
  }


  private static CharSequence concatList(List<String> strings) {
    if (null == strings) {
      return EMPTY_STRINGBUFFER.subSequence(0,EMPTY_STRINGBUFFER.length());
    }

    // Running the list and calculating the required size first is faster than the reallocs.
    int initialSize = 0;
    for (String s : strings) {
      initialSize += s.length() + 2;
    }

    StringBuilder builder = new StringBuilder(initialSize);
    for (String s : strings) {
      builder.append(s);
      builder.append('\n');
    }

    return builder.subSequence(0, builder.length());
  }


}
