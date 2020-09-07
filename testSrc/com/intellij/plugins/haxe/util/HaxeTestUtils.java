/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
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
package com.intellij.plugins.haxe.util;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.build.IdeaTarget;
import com.intellij.plugins.haxe.build.MethodWrapper;
import com.intellij.plugins.haxe.build.UnsupportedMethodException;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.util.ReflectionUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.function.Consumer;

/**
 * Created by fedorkorotkov.
 */
public class HaxeTestUtils {
  public static final Logger LOG = Logger.getInstance("#HaxeTestUtils");

  public static final String HAXE_TOOLKIT_BASE_DIR = "haxe";
  public static final String HAXE_STDLIB_DIR = "std";
  public static final String VERSION_4_0_5 = "4.0.5";  // Path element.
  public static final String LATEST = VERSION_4_0_5;

  /**
   * The root of the test data directory
   */
  public static final String BASE_TEST_DATA_PATH = findTestDataPath();

  /**
   * Get the toolkit (from the testData directory) path relative to the current test's
   * testData (sub-directory).  For example, with a test using 'testData/annotation',
   * the resultant directory would be something like '../haxe/4.0.5/std', meaning that
   * the standard library is at '.../testData/haxe/4.0.5/std'.
   *
   * @param testDataPath - Path to test data home (e.g. {@link HaxeCodeInsightFixtureTestCase#getTestDataPath()})
   * @param version - Version of the toolkit to use. (e.g. {@link HaxeTestUtils#VERSION_4_0_5})
   * @return The toolkit's standard library path relative to the current test's testData.
   */
  public static String getTestRelativeHaxeStandardLibraryPath(String testDataPath, String version) {
    String toolkitPath = HaxeFileUtil.joinPath(HAXE_TOOLKIT_BASE_DIR, version, HAXE_STDLIB_DIR);

    String relativePath = testDataPath.substring(BASE_TEST_DATA_PATH.length() + 1);
    List<String> splitPath = HaxeFileUtil.splitPath(relativePath);
    for (int i = 0; i < splitPath.size(); ++i) {
      splitPath.set(i, "..");
    }
    String relativeParent = HaxeFileUtil.joinPath(splitPath);

    return HaxeFileUtil.joinPath(relativeParent, toolkitPath);
  }

  /**
   * Gets the absolute directory name for the toolkit path in the testdata
   * @param version - Version of the toolkit to use. (e.g. {@link HaxeTestUtils#VERSION_4_0_5})
   * @return platform-specific directory name of the test toolkit location (under the testData directory).
   */
  public static String getAbsoluteToolkitPath(String version) {
    String tkPath = HaxeFileUtil.joinPath(HAXE_TOOLKIT_BASE_DIR, version, HAXE_STDLIB_DIR);
    return HaxeFileUtil.joinPath(BASE_TEST_DATA_PATH, tkPath);
    //VirtualFile vfile = HaxeFileUtil.locateFile(HaxeFileUtil.joinPath(BASE_TEST_DATA_PATH, tkPath));
    //assert null != vfile: "Could not locate absolute Toolkit path.";
    //return vfile.getPath();
  }

  /**
   * Converts a list of (std lib) file names to their equivalent paths in the common toolkit directory,
   *
   * @param testCase - The Test case to use the toolkit with.
   * @param version - Version of the toolkit to use. (e.g. {@link HaxeTestUtils#VERSION_4_0_5})
   * @param files - Files to use from the toolkit.
   * @return a list of toolkit file names in the common toolkit directory.
   */
  public static List<String> useToolkitFiles(HaxeCodeInsightFixtureTestCase testCase, String version, String... files) {
    String testPath = testCase.getTestDataPath();
    String relativeToolkit = getTestRelativeHaxeStandardLibraryPath(testPath, version);
    List<String> toolkitFiles = new ArrayList<>();
    for (String file : files) {
      if (file.startsWith("std/")) {
        file = file.substring(4);
      }
      String toolkitFile = HaxeFileUtil.joinPath(relativeToolkit, file);
      toolkitFiles.add(toolkitFile);

      String fullName = HaxeFileUtil.joinPath(testPath, toolkitFile);
      assert null != fullName && new File(fullName).exists():
        "Toolkit file " + file + " is not found, does it need to be added to the " +
        getAbsoluteToolkitPath(version) + " directory?";
    }

    return toolkitFiles;
  }

  /**
   * Locate the test data.
   *
   * @return The file system path to test data.
   */
  private static String findTestDataPath() {
    File f = new File("testData");
    if (f.exists()) {
      return f.getAbsolutePath();
    }
    return PathManager.getHomePath() + "/plugins/haxe/testData";
  }

  /**
   * Finds unterminated UI timers (that probably have nothing to do with your tests) and shuts them down.
   *
   * Call this in your test's tearDown() method before calling super.tearDown().
   * @param exceptionHandler
   */
  public static void cleanupUnexpiredAppleUITimers(Consumer<Throwable> exceptionHandler) {
    // NOTE: On macOS, plugin tests fail because swing timers which have nothing to do with the plugin test aren't being disposed.
    // See TestApplicationManagerKt.checkJavaSwingTimersAreDisposed, from which this code is adapted.
    try {
      Class<?> timerQueueClass = Class.forName("javax.swing.TimerQueue");
      Method sharedInstance = timerQueueClass.getMethod("sharedInstance");
      sharedInstance.setAccessible(true);
      Object timerQueue = sharedInstance.invoke(null);
      DelayQueue<?> delayQueue = ReflectionUtil.getField(timerQueueClass, timerQueue, DelayQueue.class, "queue");

      // Iterator for DelayQueue is a snapshot, so manipulating delayQueue has no effect on the iterator.
      for (Object queuedObject: delayQueue) {
        if (queuedObject instanceof Delayed) {
          Delayed timer = (Delayed)queuedObject;

          Method getTimer = ReflectionUtil.getDeclaredMethod(timer.getClass(), "getTimer");
          Timer swingTimer = (Timer) getTimer.invoke(timer);
          LOG.warn("Timer still open:" + swingTimer.toString());
          for (ActionListener listener : swingTimer.getActionListeners()) {
            LOG.warn(" -> open listener:" + listener.toString());
            if (listener.toString().contains("AquaProgressBarUI")) {
              swingTimer.stop();
            }
          }

        }
      }
    } catch ( ClassNotFoundException
            | NoSuchMethodException
            | IllegalAccessException
            | InvocationTargetException e) {
      LOG.warn("Error trying to clean up timers: ", e);
      exceptionHandler.accept(e);
    }
  }

  /**
   * Common implementation of addSuppressedException to be used in all of the Haxe test classes.
   *
   * It is expected that the test class will implement addSuppressedException by
   * calling this.  However, addSuppressedException is only overridden in our tests for compatibility's sake.
   * It (and this implementation) can be deleted when we no longer test with pre 18.3 versions of idea.
   *
   * NOTE: It would have been better to place this code in a common base class for all Haxe test
   *       classes.  However, there is no such thing; our tests override many different test classes.
   *
   * @param e
   * @param testCase
   */
  public static void suppressException(@NotNull Throwable e, @NotNull UsefulTestCase testCase) {

    if (IdeaTarget.IS_VERSION_18_3_COMPATIBLE) {
      try {

        MethodWrapper sase = new MethodWrapper(testCase.getClass().getSuperclass(), true, "addSuppressedException", Throwable.class);
        sase.invoke(e);
        return;
      }
      catch (UnsupportedMethodException ex) {
        LOG.warn("Could not find or execute addSuppressedException() in any test super class. Was it removed?");
      }
    }

    String stackInfo = HaxeDebugUtil.getExceptionStackAsString(e);
    LOG.warn("Exception during teardown:" + e.getMessage() + "\n" + stackInfo);
  }
}
