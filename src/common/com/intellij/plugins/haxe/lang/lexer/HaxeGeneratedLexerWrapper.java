/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2017 AS3Boyan
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
package com.intellij.plugins.haxe.lang.lexer;

import com.intellij.openapi.project.Project;

/**
 * This class exists solely to add hooks to the generated _HaxeLexer.  A better
 * solution could be to use a JFlex skeleton that allows for hooks.  Since we
 * use the skeleton provided with grammar-kit, this is easier to maintain.
 *
 * Created by ebishton on 4/14/17.
 */
public class HaxeGeneratedLexerWrapper extends _HaxeLexer {
  public HaxeGeneratedLexerWrapper(Project project) {
    super(project);
  }

  public void reset(CharSequence buffer, int start, int end, int initialState) {
    super.reset(buffer, start, end, initialState);
    super.ccsupport.reset(super.context);
  }
}
