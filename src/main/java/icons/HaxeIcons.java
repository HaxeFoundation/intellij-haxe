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

  public static final Icon Class = load("/icons/nodes/class.svg");
  public static final Icon Enum = load("/icons/nodes/enum.svg");
  public static final Icon Typedef = load("/icons/nodes/typedef.svg");
  public static final Icon Interface = load("/icons/nodes/interface.svg");
  public static final Icon Method = load("/icons/nodes/method.svg");
  public static final Icon Field = load("/icons/nodes/field.svg");
  public static final Icon Parameter = load("/icons/nodes/parameter.svg");
  public static final Icon Variable = load("/icons/nodes/variable.svg");
  public static final Icon Module = load("/icons/nodes/module.svg");
  public static final Icon Abstract = load("/icons/nodes/abstract.svg");
  //TODO create Icon
  public static final Icon Anonymous = load("/icons/nodes/abstract.svg");

  public static final Icon TYPEDEF_GUTTER = load("/icons/nodes/typedefGutter.svg");

  public static final Icon HAXELIB_JSON = load("/icons/nodes/file.svg");

  public static final Icon HAXE_LOGO = load("/icons/Haxe_logo.svg");
  public static final Icon NMML_LOGO = load("/icons/buildsystem/nme.svg");
  public static final Icon LIME_LOGO = load("/icons/buildsystem/lime.svg");
  public static final Icon OPENFL_LOGO = load("/icons/buildsystem/openfl.svg");

  public static final Icon HAXE_RELOAD = load("/icons/Haxe_reload.svg");

  public static final Icon HASHLINK = load("/icons/other/HashLink_logo.svg");
  public static final Icon VSHAXE= load("/icons/other/VsHaxe-logo.svg");

  public static final Icon DEBUGGER_VSHAXE= load("/icons/debugger/Haxe_logo_vshaxe.svg");
  public static final Icon DEBUGGER_EVAL= load("/icons/debugger/Haxe_logo_eval.svg");
  public static final Icon DEBUGGER_WEB= load("/icons/debugger/Haxe_logo_web.svg");
  public static final Icon DEBUGGER_HASHLINK= load("/icons/debugger/Haxe_logo_hashlink.svg");
  public static final Icon DEBUGGER_HXCPP= load("/icons/debugger/Haxe_logo_hxcpp.svg");


  public static final Icon HAXE_LOGO_GRAY_13 = load("/icons/Haxe_logo_gray_13.svg");


}
