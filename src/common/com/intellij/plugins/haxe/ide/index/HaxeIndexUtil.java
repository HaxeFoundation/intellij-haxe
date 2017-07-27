/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017 Eric Bishton
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

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.util.HaxeDebugLogger;
import com.intellij.plugins.haxe.util.HaxeDebugUtil;
import org.apache.log4j.Level;

/**
 * Created by fedorkorotkov.
 */
public class HaxeIndexUtil {
  public static int BASE_INDEX_VERSION = 1;

  public static final HaxeDebugLogger LOG = HaxeDebugLogger.getLogger();
  static {
    // Logs can be turned on externally (and before they're allocated!).  Make sure that
    // our warnings still get out.
    if (!LOG.isEnabledFor(Level.WARN)) {
      LOG.setLevel(Level.WARN);
    }
  }

  public static boolean warnIfDumbMode(Project project) {
    if (DumbService.isDumb(project)) {
      LOG.warn("Unexpected index activity while in dumb mode at " + HaxeDebugUtil.printCallers(1));
      return false;
    }
    return true;
  }
}
