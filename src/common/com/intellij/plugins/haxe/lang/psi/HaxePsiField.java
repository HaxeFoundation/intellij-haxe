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
package com.intellij.plugins.haxe.lang.psi;

import com.intellij.psi.PsiField;

/**
 * Created by srikanthg on 10/9/14.
 */
//
// NOTE: This class MUST derive from HaxeComponent -- not HaxeNamedComponent for
// certain refactorings (that use HaxeQualifiedNameProvider) to work correctly.
// If the derivation changes, an exception is thrown out of than class, and
// refactorings requiring names silently fail (becase of a cast exception).
// See testSrc/com/intellij/plugins/haxe/ide/refactoring/introduce/HaxeIntroduceVariableTest#ReplaceAll3()
//
//                                    |||||||||||||
//                                    vvvvvvvvvvvvv
public interface HaxePsiField extends HaxeComponent, PsiField {

}
