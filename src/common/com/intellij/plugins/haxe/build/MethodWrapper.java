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

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by ebishton on 1/16/16.
 */
public class MethodWrapper<R> {

  private Method myMethodToInvoke;
  private String myDebugName;

  public MethodWrapper(@NotNull Class klass, @NotNull String methodName, Class<?>... argTypes) {
    myDebugName = "";
    try {
      myDebugName = klass.getName() + "." + methodName;
      // XXX: Could use getDeclaredMethod() if we want to get protected methods, but we
      //      will have to walk the class hierarchy ourselves.  See the Java
      //      Reflection sources for how to do that.
      myMethodToInvoke = klass.getMethod(methodName, argTypes);
    } catch (NoSuchMethodException e) {
      throw new UnsupportedMethodException("Didn't find " + myDebugName + ";", e);
    }
  }

  public R invoke(Object instance, Object... args) {
    try {
      return (R) myMethodToInvoke.invoke(instance, args);
    } catch (IllegalAccessException e) {
      throw new UnsupportedMethodException("Error invoking " + myDebugName + ";", e);
    } catch (InvocationTargetException e) {
      throw new UnsupportedMethodException("Error invoking " + myDebugName + ";", e.getCause());
    }
  }
}
