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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Wraps classes such that classes that have been renamed can be used
 * in a source-compatible way across intellij-haxe releases.  This is used
 * for source compatibility among multiple IDEA code lines.
 *
 * This is really more of a class loader, but it's named similarly to the other
 * XxxWrapper classes to be cohesive.
 *
 * Created by ebishton on 9/19/16.
 */
public class ClassWrapper<T> {
  private Class<T> myClass;
  private String myDebugName;

  /** Generally, className should be the fully qualified class name.  However,
   * any format acceptable to Class.forName() will do.
   *
   * Due to how Class.forName() works, this constructor will not find
   * inner classes using only dotted notation.  It will
   * find inner classes using the '$' binary name notation.  Code may be
   * easier to read if you use the alternate constructors that use inner class
   * names.
   *
   * @param className - a fully qualified class name, possibly prefixed with
   *                  array annotations as documented by Class.forName().
   */
  public ClassWrapper( String className ) {
    if (null == className || className.isEmpty()) {
      throw new UnsupportedClassException("Empty class name.");
    }
    myDebugName = className;
    try {
      myClass = (Class<T>) Class.forName(className);
    }
    catch (ClassNotFoundException e) {
      throw new UnsupportedClassException("Could not find class " + myDebugName + ";", e);
    }

    // XXX: Class may be an internal class.  Should we deal with that silently in
    //      this class, or expect a sub-class to do it?

    if (null != myClass) {
      myDebugName = myClass.getCanonicalName();
    }
  }

  /**
   * Wrap an inner class.
   * @param klass
   * @param innerClassName
   */
  public ClassWrapper( Class klass, String innerClassName ) {
    if (null == klass) {
      throw new UnsupportedClassException("NULL parent class.");
    }
    if (null == innerClassName || innerClassName.isEmpty()) {
      throw new UnsupportedClassException("Empty class name.");
    }

    myDebugName = klass.getCanonicalName() + "$" + innerClassName;
    // Find the public inner class.
    for (Class<?> inner : klass.getClasses()) {
      if (innerClassName.equals(inner.getSimpleName())) {
        myClass = (Class<T>) inner;
        myDebugName = myClass.getCanonicalName();
        return;
      }
    }
    throw new UnsupportedClassException("Could not find class " + myDebugName + ";", null);
  }

  public ClassWrapper( ClassWrapper wrapped, String innerClassName ) {
    this(wrapped.getWrappedClass(), innerClassName);
  }

  public Class<T> getWrappedClass() {
    return myClass;
  }

  /**
   * Creates a new instance of the wrapped class, using the constructor matching
   * the arguments given.  If no constructor is found, or another error occurs,
   * an UnsupportedClassException will be thrown.
   *
   * @param args - The list of arguments to be used when creating the new class
   *             instance.
   * @return a new instance of the wrapped class.
   */
  public T newInstance(Object... args) {
    T inst;

    Class<?> argTypes[] = new Class<?>[args.length];
    for (int i=0; i< args.length; i++) {
      argTypes[i] = args.getClass();
    }

    try {
      Constructor<T> ctor = myClass.getDeclaredConstructor(argTypes);
      inst = ctor.newInstance(args);
    }
    catch (NoSuchMethodException e) {
      throw new UnsupportedClassException("Could not find constructor for " + myDebugName + "(" + argTypes.toString() + ");", e);
    }
    catch (IllegalAccessException e) {
      throw new UnsupportedClassException("Cannot create instance of " + myDebugName + ";", e);
    }
    catch (InvocationTargetException e) {
      throw new UnsupportedClassException("Cannot create instance of " + myDebugName + ";", e);
    }
    catch (InstantiationException e) {
      throw new UnsupportedClassException("Cannot create instance of " + myDebugName + ";", e);
    }

    return inst;
  }

}


