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

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Access a field using reflection.
 *
 * Typically, we are using this for backward code compatibility, so that we
 * can get around the compile-time requirement for missing fields to be
 * present (e.g. have syntactically correct code when the underlying
 * sources have changed).
 *
 * Created by ebishton on 1/19/16.
 */
public class FieldWrapper<FieldType> {
  String myDebugName;
  Field myField;

  public FieldWrapper(@NotNull Class klass, String fieldName) {
    try {
      myDebugName = klass.getName() + "." + fieldName;
      // XXX: Might want to walk the hierarchy ourselves, rather than expect
      //      the proper superclass to be passed in.  Do that later, if we
      //      need access to protected fields in superclasses.
      myField = klass.getDeclaredField(fieldName);
      myField.setAccessible(true); // IMPORTANT: Give access to the variable.
    } catch (NoSuchFieldException e) {
      throw new UnsupportedFieldException("Field " + myDebugName + " was not found.", e);
    }

  }

  public FieldType get(Object o) {
    try {
      return (FieldType)myField.get(o);
    } catch (IllegalAccessException e) {
      throw new UnsupportedFieldException("Illegal access of field " + myDebugName, e);
    }
  }

  public FieldType set(@NotNull Object o, FieldType val) {
    try {
      myField.set(o, val);
      return val;
    } catch (IllegalAccessException e) {
      throw new UnsupportedFieldException("Illegal access of field " + myDebugName, e);
    }
  }

}
