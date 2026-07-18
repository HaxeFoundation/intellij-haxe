/*
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

import org.jetbrains.annotations.NotNull;

/**
 * Process-tree termination helpers built only on the JDK's ProcessHandle API, so they can be
 * used both in the IDE and inside the external JPS build process (whose classpath does not
 * include the platform's KillableProcessHandler/OSProcessUtil classes).
 *
 * A Haxe build is typically a process tree (haxelib -> haxe -> hxcpp -> cl/g++ per file),
 * so terminating just the root process leaves the native compilers running.
 */
public final class HaxeProcessTreeUtil {

  private HaxeProcessTreeUtil() {
  }

  /**
   * Request termination of the process and all of its descendants.
   * On Unix this delivers SIGTERM, giving the toolchain a chance to clean up;
   * on Windows there is no graceful equivalent and processes are terminated outright.
   */
  public static void destroyProcessTree(@NotNull Process process) {
    // snapshot the descendants before touching the root: once the root dies,
    // children are re-parented and can no longer be enumerated through it
    process.descendants().forEach(ProcessHandle::destroy);
    process.destroy();
  }

  /**
   * Forcibly kill the process and all of its descendants (SIGKILL / TerminateProcess).
   */
  public static void killProcessTree(@NotNull Process process) {
    process.descendants().forEach(ProcessHandle::destroyForcibly);
    process.destroyForcibly();
  }
}
