package com.intellij.plugins.haxe.v2.migration

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.GeneralModuleType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.LibraryDependency
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryTableId
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.execution.RunManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.plugins.haxe.HaxeProjectBundle
import com.intellij.plugins.haxe.config.HaxeTarget
import com.intellij.plugins.haxe.config.NMETarget
import com.intellij.plugins.haxe.config.OpenFLTarget
import com.intellij.plugins.haxe.haxelib.HaxelibNameUtil
import com.intellij.plugins.haxe.runner.HaxeRunConfigurationType
import com.intellij.plugins.haxe.runner.debugger.flash.FlashConfigurationFactory
import com.intellij.plugins.haxe.runner.debugger.flash.FlashRunConfiguration
import com.intellij.plugins.haxe.runner.debugger.hxcpp.legacy.LegacyHxcppRunConfiguration
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings
import com.intellij.plugins.haxe.ide.module.HaxeModuleType
import com.intellij.plugins.haxe.v2.buildtools.HxmlProjects
import com.intellij.plugins.haxe.v2.buildtools.LimeProjects
import com.intellij.plugins.haxe.v2.buildtools.NmeProjects
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeActiveBuildFileStore
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeTargetSelectionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Converts legacy (V1) HAXE_MODULE modules to the V2 project model: the V1
 * module settings seed the V2 stores (environment SDK, compile command from
 * the configured hxml/openfl/nmml file, project active file), then the module
 * type becomes a plain [GeneralModuleType] so the legacy machinery never
 * engages again. The old settings component stays in the .iml untouched —
 * harmless, and the user's backup keeps the pre-conversion state anyway.
 */
@Service(Service.Level.PROJECT)
class HaxeV1Migrator(private val project: Project, private val scope: CoroutineScope) {

  companion object {
    @JvmStatic
    fun getInstance(project: Project): HaxeV1Migrator = project.service()

    private const val LEGACY_TYPE_ID = "HAXE_MODULE"
    internal const val NOTIFICATION_GROUP_ID = "haxe.v1.migration"

    /** Whether the module still uses the legacy (V1) Haxe module type. */
    @JvmStatic
    fun isLegacy(module: Module): Boolean = ModuleType.get(module) === HaxeModuleType.getInstance()
  }

  /** The names of the modules that still use the legacy (V1) Haxe module type. */
  fun legacyModuleNames(): List<String> =
    ModuleManager.getInstance(project).modules.filter(::isLegacy).map { it.name }

  /**
   * Asks for confirmation, then converts every legacy module. Returns whether
   * the conversion started; it does not when the user cancels or no legacy
   * module is left. Call on the EDT.
   */
  fun confirmAndConvert(): Boolean {
    val moduleCount = legacyModuleNames().size
    if (moduleCount == 0) return false
    val answer = Messages.showYesNoDialog(
      project,
      HaxeProjectBundle.message("haxe.v1.migration.confirm.message", moduleCount),
      HaxeProjectBundle.message("haxe.v1.migration.confirm.title"),
      HaxeProjectBundle.message("haxe.v1.migration.confirm.yes"),
      Messages.getCancelButton(),
      Messages.getWarningIcon())
    if (answer != Messages.YES) return false
    convertAsync()
    return true
  }

  private fun convertAsync() {
    scope.launch {
      val converted = readAction { seedV2State() }
      val runConfigurationPlans = readAction { planRunConfigurationConversion() }
      WorkspaceModel.getInstance(project).update("Convert V1 haxe modules to V2") { builder ->
        builder.entities(ModuleEntity::class.java)
          .filter { it.type?.name == LEGACY_TYPE_ID }
          .toList()
          .forEach { entity ->
            removeV1ManagedLibraries(builder, entity)
            builder.modifyModuleEntity(entity) { type = ModuleTypeId(GeneralModuleType.TYPE_ID) }
          }
      }
      withContext(Dispatchers.EDT) {
        applyRunConfigurationPlans(runConfigurationPlans)
        notifyDone(converted)
      }
    }
  }

  /**
   * How one old-style run configuration converts. The old configuration's own
   * fields did not survive loading (its class is gone); the routing facts —
   * target, output artifact, flex SDK — always lived in the module settings.
   */
  private sealed interface ConfigurationPlan {
    val name: String
    val moduleName: String

    /** flash-targeted: becomes a Haxe Flash Application configuration. */
    data class Flash(override val name: String,
                     override val moduleName: String,
                     val swfPath: String,
                     val flexSdkName: String) : ConfigurationPlan

    /** cpp-targeted: stays legacy-hxcpp, with the executable filled in. */
    data class Hxcpp(override val name: String,
                     override val moduleName: String,
                     val executablePath: String) : ConfigurationPlan
  }

  private fun planRunConfigurationConversion(): List<ConfigurationPlan> {
    val plans = mutableListOf<ConfigurationPlan>()
    for (settings in RunManager.getInstance(project).allSettings) {
      val configuration = settings.configuration as? LegacyHxcppRunConfiguration ?: continue
      val module = configuration.configurationModule.module ?: continue
      if (!isLegacy(module)) continue
      val moduleSettings = HaxeModuleSettings.getInstance(module)
      val output = outputArtifact(moduleSettings)

      if (isFlashTargeted(moduleSettings)) {
        plans += ConfigurationPlan.Flash(settings.name, module.name, output ?: "", moduleSettings.flexSdkName ?: "")
      } else if (output != null) {
        plans += ConfigurationPlan.Hxcpp(settings.name, module.name, output)
      }
      // anything else keeps its (empty) legacy configuration - the tool
      // window's Build & run creates the right flavour on demand
    }
    return plans
  }

  private fun isFlashTargeted(settings: HaxeModuleSettings): Boolean {
    val haxeTargeted = settings.isUseHxmlToBuild || settings.isUseUserPropertiesToBuild
    return haxeTargeted && settings.haxeTarget == HaxeTarget.FLASH
           || settings.isUseOpenFLToBuild && settings.openFLTarget == OpenFLTarget.FLASH
           || settings.isUseNmmlToBuild && settings.nmeTarget == NMETarget.FLASH
  }

  private fun outputArtifact(settings: HaxeModuleSettings): String? {
    val folder = settings.outputFolder?.takeIf { it.isNotBlank() } ?: return null
    val fileName = settings.outputFileName?.takeIf { it.isNotBlank() } ?: return null
    return FileUtil.join(folder, fileName)
  }

  private fun applyRunConfigurationPlans(plans: List<ConfigurationPlan>) {
    if (plans.isEmpty()) return
    val runManager = RunManager.getInstance(project)
    val moduleManager = ModuleManager.getInstance(project)
    for (plan in plans) {
      val old = runManager.allSettings.firstOrNull { it.name == plan.name } ?: continue
      val module = moduleManager.findModuleByName(plan.moduleName)
      when (plan) {
        is ConfigurationPlan.Flash -> {
          val factory = HaxeRunConfigurationType.getInstance().getFactory(FlashConfigurationFactory::class.java)
          val replacement = runManager.createConfiguration(plan.name, factory)
          (replacement.configuration as FlashRunConfiguration).apply {
            setModule(module)
            swfFilePath = plan.swfPath
            flexSdkName = plan.flexSdkName
          }
          replacement.storeInLocalWorkspace()
          runManager.removeConfiguration(old)
          runManager.addConfiguration(replacement)
        }
        is ConfigurationPlan.Hxcpp -> {
          (old.configuration as? LegacyHxcppRunConfiguration)?.apply {
            setModule(module)
            executablePath = plan.executablePath
          }
        }
      }
    }
  }

  /**
   * The V1 engine maintained its own "Haxelib: name:version" module libraries
   * and cleaned them up itself. After conversion nothing owns them, and the
   * V2 sync adds its own "haxelib: " entries — without this the user sees
   * every dependency twice.
   */
  private fun removeV1ManagedLibraries(builder: MutableEntityStorage, module: ModuleEntity) {
    val tableId = LibraryTableId.ModuleLibraryTableId(ModuleId(module.name))
    builder.entities(LibraryEntity::class.java)
      .filter { it.tableId == tableId && HaxelibNameUtil.isManagedLibrary(it.name) }
      .toList()
      .forEach { builder.removeEntity(it) }
    builder.modifyModuleEntity(module) {
      dependencies.removeAll { dependency ->
        dependency is LibraryDependency && HaxelibNameUtil.isManagedLibrary(dependency.library.name)
      }
    }
  }

  /** Seeds the V2 stores from each legacy module's settings; returns the converted module names. */
  private fun seedV2State(): List<String> {
    val environment = HaxeEnvironmentStore.getInstance(project)
    val activeStore = HaxeActiveBuildFileStore.getInstance(project)
    val converted = mutableListOf<String>()
    for (module in ModuleManager.getInstance(project).modules) {
      if (!isLegacy(module)) continue
      converted += module.name
      val settings = HaxeModuleSettings.getInstance(module)

      ModuleRootManager.getInstance(module).sdk?.let { environment.setSdkName(module.name, it.name) }

      val buildFilePath = when {
        settings.isUseHxmlToBuild -> settings.hxmlPath
        settings.isUseOpenFLToBuild -> settings.openFLPath
        settings.isUseNmmlToBuild -> settings.nmmlPath
        else -> null
      }?.takeIf { it.isNotBlank() }

      val buildFile = buildFilePath?.let { LocalFileSystem.getInstance().findFileByPath(it) }
      if (buildFile != null && buildFile.isValid) {
        val action = when {
          settings.isUseHxmlToBuild -> HxmlProjects.BUILD_ACTION
          settings.isUseNmmlToBuild -> NmeProjects.BUILD_ACTION
          else -> LimeProjects.BUILD_ACTION
        }
        environment.setCompileCommand(
          module.name,
          HaxeEnvironmentStore.CompileCommand(buildFile.path, action, settings.arguments ?: ""))
        if (activeStore.activeFilePath.isNullOrBlank()) {
          activeStore.setActiveFile(buildFile.path)
        }
        // target selection ids are the configured target names, which the
        // built-in lists seed from the enums' display strings (toString)
        val legacyTarget = when {
          settings.isUseOpenFLToBuild -> settings.openFLTarget
          settings.isUseNmmlToBuild -> settings.nmeTarget
          else -> null
        }
        legacyTarget?.let { HaxeTargetSelectionStore.getInstance(project).setSelectedTargetId(buildFile, it.toString()) }
      }
    }
    return converted
  }

  private fun notifyDone(converted: List<String>) {
    if (project.isDisposed) return
    NotificationGroupManager.getInstance()
      .getNotificationGroup(NOTIFICATION_GROUP_ID)
      .createNotification(
        HaxeProjectBundle.message("haxe.v1.migration.done.title"),
        HaxeProjectBundle.message("haxe.v1.migration.done.content", converted.size, converted.joinToString(", ")),
        NotificationType.INFORMATION)
      .notify(project)
  }
}
