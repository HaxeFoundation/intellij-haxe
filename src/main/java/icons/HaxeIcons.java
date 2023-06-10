/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
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
package icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;


public class HaxeIcons {
  private static Icon load(String path) {
    return IconLoader.getIcon(path, HaxeIcons.class);
  }

  public static final Icon Class = load("/icons/class.svg");
  public static final Icon Enum = load("/icons/enum.svg");
  public static final Icon Typedef = load("/icons/typedef.svg");
  public static final Icon Interface = load("/icons/interface.svg");
  public static final Icon Method = load("/icons/method.svg");
  public static final Icon Field = load("/icons/field.svg");


  public static final Icon HAXE_LOGO = load("/icons/Haxe_logo.svg");
  public static final Icon NMML_LOGO = load("/icons/buildsystem/nme.svg");
  public static final Icon LIME_LOGO = load("/icons/buildsystem/lime.svg");
  public static final Icon OPENFL_LOGO = load("/icons/buildsystem/openfl.svg");


}
