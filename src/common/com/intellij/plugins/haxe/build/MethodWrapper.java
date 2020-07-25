/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2016 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2020 Eric Bishton
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

  public <T> MethodWrapper(@NotNull Class<T> klass, boolean searchSupers, @NotNull String methodName, Class<?>... argTypes) {
    myDebugName = "";
    String searchName = klass.getName() + "." + methodName;
    Class<? super T> searchKlass = klass;
    Exception firstCaught = null;
    do {
      try {
        myDebugName = searchKlass.getName() + "." + methodName;

        if (false) dumpMethods(searchKlass);

        myMethodToInvoke = searchKlass.getDeclaredMethod(methodName, argTypes);
      }
      catch (NoSuchMethodException e) {
        if (null == firstCaught)
          firstCaught = e;
      }

      searchKlass = searchSupers ? searchKlass.getSuperclass() : null;

    } while (null == myMethodToInvoke && null != searchKlass);

    if (null == myMethodToInvoke) {
      throw new UnsupportedMethodException("Didn't find " + searchName + ";", firstCaught);
    }
  }

  public R invoke(Object instance, Object... args) {
    try {
      return (R) myMethodToInvoke.invoke(instance, args);
    } catch (IllegalAccessException e) {
      throw new UnsupportedMethodException("Error invoking " + myDebugName + ";", e);
    } catch (InvocationTargetException | IllegalArgumentException e) {
      throw new UnsupportedMethodException("Error invoking " + myDebugName + ";", e.getCause());
    }
  }

  private <T> void dumpMethods(Class<T> klass) {
    Method[] methods = klass.getDeclaredMethods();
    System.out.println("Methods on " + klass.getName());
    for (Method m : methods) {
      StringBuilder b = new StringBuilder(": ");
      b.append(m.toGenericString());
      b.append("  Internal parameter types:");
      for (Class<?> c : m.getParameterTypes()) {
        b.append(c.getTypeName());
        b.append(", ");
      }
      System.out.println(b.toString());
    }
  }

}
