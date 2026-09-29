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

import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeMethodStub;
import com.intellij.plugins.haxe.lang.psi.stubs.type.HaxeStubbedElement;

/**
 * Because people should not be coding to PsiMixin classes directly, this
 * is effectively an alias for that class.
 *
 * Created by ebishton on 9/28/14.
 */
public interface HaxeMethod extends HaxeMethodPsiMixin, HaxeStubbedElement<HaxeMethodStub> {

    boolean isAbstract();

    boolean isMacro();

    /**
     * The DECLARED visibility only — never resolves the parent chain, so it is
     * safe where resolve is forbidden (stub building, file-based indexers).
     * Differs from {@link #isPublic()} for an inherited-visibility override
     * (an {@code override} without public/private), where the returned value
     * is a public-leaning placeholder and the real visibility is deferred
     * through the stub's visibility-inherited flag.
     */
    boolean isDeclaredPublic();
}
