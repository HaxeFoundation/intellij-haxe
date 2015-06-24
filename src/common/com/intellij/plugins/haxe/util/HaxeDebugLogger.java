/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
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
package com.intellij.plugins.haxe.util;

import org.apache.log4j.*;
import org.apache.log4j.spi.LoggerFactory;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.varia.NullAppender;

import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A logger that can be controlled independent of (and, more importantly, can suppress)
 * the underlying log4j logger that is used in the IDEA code.  Loggers allocated through
 * this interface are not part of IDEA's Logger hierarchy; their hierarchy is independent.
 *
 * Created by ebishton on 6/12/15.
 */
public class HaxeDebugLogger extends org.apache.log4j.Logger {

  private static final HaxeDebugLoggerManager manager = new HaxeDebugLoggerManager();

  public static interface Factory extends LoggerFactory {
    public HaxeDebugLogger makeNewRootLoggerInstance();
  }

  public static class DefaultFactory implements Factory {
    public DefaultFactory() { }
    public HaxeDebugLogger makeNewLoggerInstance(String name) {
      HaxeDebugLogger logger = new HaxeDebugLogger(name);
      return logger;
    }
    public HaxeDebugLogger makeNewRootLoggerInstance() {
      HaxeDebugLogger logger = new HaxeDebugLogger("root");
      return logger;
    }
  }

  public static interface HierarchyManipulator {
    public void restore() throws IllegalStateException;
    public void mute(LoggerRepository hierarchy) throws IllegalStateException;
  }

  /**
   * Simply configure the primary log4j Loggers to send all output to the bit bucket.
   */
  public static void configurePrimaryLoggerToSwallowLogs() {
    org.apache.log4j.BasicConfigurator.configure(new NullAppender());
  }

  /**
   * Simply configure the primary log4j Loggers to send all output to the console.
   */
  public static void configurePrimaryLoggerToStdout() {
    org.apache.log4j.BasicConfigurator.configure();
  }

  private static String getCallingClassName() {
    // Get the current call stack.  Then walk it until we find the first
    // entry that isn't *this* class.
    String className = "<Unknown class>";
    StackTraceElement[] stack = new Exception().getStackTrace();
    for (StackTraceElement frame : stack) {
      String frameClassName = frame.getClassName();
      if (!frameClassName.equals(HaxeDebugLogger.class.getName())) {
        className = frameClassName;
        break;
      }
    }
    return className;
  }

  // These deprecated versions remain here to override the default deprecated methods.
  @Deprecated
  public static HaxeDebugLogger getInstance() {
    return getLogger(getCallingClassName(), manager.getFactory());
  }
  public static HaxeDebugLogger getInstance(String name) {
    return getLogger(name, manager.getFactory());
  }
  public static HaxeDebugLogger getInstance(Class clazz) {
    return getLogger(clazz.getName(), manager.getFactory());
  }

  public static HaxeDebugLogger getLogger() {
    return getLogger(getCallingClassName(), manager.getFactory());
  }
  public static HaxeDebugLogger getLogger(String name) {
    return getLogger(name, manager.getFactory());
  }
  public static HaxeDebugLogger getLogger(Class clazz) {
    return getLogger(clazz.getName(), manager.getFactory());
  }
  public static HaxeDebugLogger getLogger(String name, LoggerFactory factory) {
    Logger logger = manager.getHierarchy().getLogger(name, factory);
    assert(logger instanceof HaxeDebugLogger);
    if (manager.getLogConfiguration().contains(name)) {
      logger.setLevel(manager.getLogConfiguration().getLevel(name));
    } else {
      manager.getLogConfiguration().addConfiguration(name, manager.getDefaultLevel());
      logger.setLevel(manager.getDefaultLevel());
    }
    logger.addAppender(manager.getDefaultAppender());
    return (HaxeDebugLogger)logger;
  }

  public static HierarchyManipulator mutePrimaryConfiguration() {
    HierarchyConfiguration config = new HierarchyConfiguration();
    config.mute(Logger.getRootLogger().getLoggerRepository());
    return config;
  }

  private HaxeDebugLogger(String name) {
    super(name);
  }

  public void setLevel(Level lvl) {
    configure(this.getName(), lvl);
    super.setLevel(lvl);
  }

  public static void configure(Class clazz, Level lvl) {
    configure(clazz.getName(), lvl);
  }

  public static void configure(String name, Level lvl) {
    manager.getLogConfiguration().addConfiguration(name, lvl);
  }
}

/**
 * Centralize global data for the HaxeDebugLogger.
 */
class HaxeDebugLoggerManager {
  private static final Level DEFAULT_LEVEL = Level.OFF;
  private Level defaultLevel = DEFAULT_LEVEL;

  private static final HaxeDebugLogger.Factory DEFAULT_FACTORY = new HaxeDebugLogger.DefaultFactory();
  private HaxeDebugLogger.Factory configuredFactory = DEFAULT_FACTORY;

  private static final Appender DEFAULT_APPENDER = new ConsoleAppender(new PatternLayout("%p [%c{1}] %x - %m%n"));
  private static final Appender METHOD_APPENDER = new ConsoleAppender(new PatternLayout("%p [%C{1}.%M] %x - %m%n"));
  private Appender defaultAppender = METHOD_APPENDER;

  private org.apache.log4j.Hierarchy hierarchy;
  private LogConfiguration logConfiguration;

  public HaxeDebugLoggerManager() {
    logConfiguration = new LogConfiguration();
    hierarchy = new Hierarchy(configuredFactory.makeNewRootLoggerInstance());
  }

  public LoggerFactory getFactory() {
    return configuredFactory;
  }

  public LogConfiguration getLogConfiguration() {
    return logConfiguration;
  }

  public void setDefaultLevel(Level lvl) {
    // Null is a valid default.  It means: check my parents.
    defaultLevel = lvl;
  }
  public Level getDefaultLevel() {
    return defaultLevel;
  }

  public Hierarchy getHierarchy() {
    return hierarchy;
  }

  /**
   * Gets the Appender that is to be set up on each logger, by default.
   */
  public Appender getDefaultAppender() {
    return defaultAppender;
  }
}

// ///////////////////////////////////////////////////////////////////////////
/**
 * Tracks the level configuration for loggers.  Level configuration can be
 * set before the logger is allocated.
 */
class LogConfiguration {
  private Map<String, Level> map;

  public LogConfiguration() {
    map = new ConcurrentHashMap<String, Level>();
  }

  public void addConfiguration(String name, Level lvl) {
    // It's not an error to add the same logger configuration again; update the level.
    // A null level is a valid configuration.
    if (null != name) {
      map.put(name, lvl);
    }
  }

  public boolean contains(String name) {
    return map.containsKey(name);
  }

  public Level getLevel(String name) {
    return map.get(name);
  }

  public void clear() {
    map.clear();
  }

  public Set<String> getAllLogNames() {
    return map.keySet();
  }
}


// ///////////////////////////////////////////////////////////////////////////
/**
 * HierarchyConfiguration
 *
 * Stores the names and level configuration of an entire LoggerHierarchy.
 */
class HierarchyConfiguration implements HaxeDebugLogger.HierarchyManipulator {
  private LogConfiguration capturedConfiguration;
  private LoggerRepository capturedHierarchy;
  private Level threshold;

  public HierarchyConfiguration() {
    threshold = Level.ALL;
    capturedConfiguration = new LogConfiguration();
    capturedHierarchy = null;
  }

  private synchronized void clear() {
    capturedConfiguration.clear();
    capturedHierarchy = null;
    threshold = Level.ALL;
  }

  private synchronized void captureState(LoggerRepository hierarchy) throws IllegalStateException {
    if (null == hierarchy) {
      throw new IllegalArgumentException("Null Hierarchy is not allowed.");
    }
    if (null != capturedHierarchy && capturedHierarchy != hierarchy) {
      throw new IllegalStateException("Log configurations is already holding a captured hierarchy.");
    }
    capturedHierarchy = hierarchy;
    threshold = hierarchy.getThreshold();
    Enumeration e = hierarchy.getCurrentLoggers();
    while (e.hasMoreElements()) {
      Logger l = (Logger)e.nextElement();
      Level v = l.getLevel();
      // No point in capturing or restoring NULL loggers.
      if (null != v) {
        capturedConfiguration.addConfiguration(l.getName(), v);
      }
    }
  }

  public synchronized void restore() throws IllegalStateException {
    if (null == capturedHierarchy) {
      throw new IllegalStateException("No log configuration to restore.");
    }
    for(String key : capturedConfiguration.getAllLogNames()) {
      Logger l = capturedHierarchy.getLogger(key);
      l.setLevel(capturedConfiguration.getLevel(key));
    }
    capturedHierarchy.setThreshold(threshold);
    clear();
  }

  public synchronized void mute(LoggerRepository hierarchy) throws IllegalStateException {
    if (null == hierarchy) {
      throw new IllegalArgumentException("Null Hierarchy is not allowed.");
    }
    if (null != capturedHierarchy && capturedHierarchy != hierarchy) {
      throw new IllegalStateException("Trying to mute a hierarchy that is not the one previously captured.");
    }
    captureState(hierarchy);
    hierarchy.setThreshold(Level.OFF);
  }
}


