/*
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
package com.intellij.plugins.haxe.lang.psi;

import com.intellij.psi.PsiJavaToken;

/**
 * Currently, simply allows Haxe to emulate Java.  We use the Java token
 * as the underlying implementation so that we can use IDEA's java-specific
 * functionality (such as UML diagrams and class-hierarchy-walking trees).
 *
 * Created by ebishton on 4/26/17.
 */
public interface HaxePsiToken extends PsiJavaToken {

}
