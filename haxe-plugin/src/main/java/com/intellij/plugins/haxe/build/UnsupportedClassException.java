/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2016 AS3Boyan
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
package com.intellij.plugins.haxe.build;

/**
 * Created by ebishton on 9/19/16.
 */
public class UnsupportedClassException extends UnsupportedOperationException {
    public UnsupportedClassException() {
      super();
    }

    public UnsupportedClassException(String reason) {
      super(reason);
    }

    public UnsupportedClassException(String reason, Throwable cause) {
      super(reason + ":" + cause.getMessage(), cause);
    }
}
