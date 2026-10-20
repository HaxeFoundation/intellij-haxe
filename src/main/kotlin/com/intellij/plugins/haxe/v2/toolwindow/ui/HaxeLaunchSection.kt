package com.intellij.plugins.haxe.v2.toolwindow.ui

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.plugins.haxe.HaxeBundle
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.Launch
import com.intellij.plugins.haxe.v2.runconfig.HaxeLaunchKind
import com.intellij.plugins.haxe.v2.runconfig.HaxeLaunchKind.Parameter
import com.intellij.plugins.haxe.v2.runconfig.HaxeLaunchKind.Parameter.Shape
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.CollapsibleRow
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.listCellRenderer.listCellRenderer
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer
import com.intellij.ui.layout.selectedValueMatches
import com.intellij.util.ui.EmptyIcon
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.EnumMap
import java.util.function.Function
import javax.swing.DefaultComboBoxModel
import javax.swing.Icon
import javax.swing.JComponent

/**
 * The launch part of the custom action dialog: two collapsed groups, at most
 * one of them in use; choosing in one resets the other.
 * "Create run configuration" picks the Haxe kind that launches the build's
 * output and shows the kind's parameters, each with the build file's default
 * as empty text; a blank field keeps following that default.
 * "Run existing configuration" picks a configuration of any type; the
 * action's command becomes its build step.
 * A file parameter browses with a file-or-folder chooser: the artifact usually
 * does not exist before the first build, so choosing a folder keeps the
 * field's file name. A directory parameter browses with a folder chooser.
 */
class HaxeLaunchSection(
  private val project: Project,
  private val defaults: Function<HaxeLaunchKind, Map<Parameter, String>>,
  initial: Launch?,
) {
  /** An entry of the existing-configuration combo; `icon` is null when the stored name matches no configuration. */
  private data class ConfigurationChoice(val name: String, val icon: Icon?)

  private val kindCombo = ComboBox(DefaultComboBoxModel(kindChoices()))
  private val fields = EnumMap<Parameter, JBTextField>(Parameter::class.java)
  private val editors = EnumMap<Parameter, JComponent>(Parameter::class.java)
  private val createDebugArguments = JBCheckBox(HaxeBundle.message("haxe.custom.action.dialog.launch.debug.arguments"), true)
  private val configurationCombo = ComboBox(DefaultComboBoxModel(configurationChoices(initial)))
  private val existingDebugArguments = JBCheckBox(HaxeBundle.message("haxe.custom.action.dialog.launch.debug.arguments"), true)

  val component: DialogPanel

  init {
    kindCombo.renderer = textListCellRenderer { kind ->
      kind?.displayName() ?: HaxeBundle.message("haxe.custom.action.dialog.launch.none")
    }
    // the run dropdown's look: the configuration's icon (temporary badge included) before its name
    configurationCombo.renderer = listCellRenderer {
      val choice = value
      icon(choice?.icon ?: if (choice != null) AllIcons.Actions.Help else EmptyIcon.ICON_16)
      text(choiceText(choice))
    }
    for (parameter in Parameter.values()) {
      val field = JBTextField()
      fields[parameter] = field
      editors[parameter] = editorFor(parameter, field)
    }
    // the two groups sit flush; each keeps its spacing inside, above its first row and below its hint
    lateinit var create: CollapsibleRow
    lateinit var existing: CollapsibleRow
    component = panel {
      create = collapsibleGroup(HaxeBundle.message("haxe.custom.action.dialog.launch.section")) {
        row(HaxeBundle.message("haxe.custom.action.dialog.launch.type")) {
          cell(kindCombo)
        }.topGap(TopGap.SMALL)
        for (parameter in Parameter.values()) {
          row(parameter.label()) {
            cell(editors.getValue(parameter)).align(AlignX.FILL)
          }.visibleIf(kindCombo.selectedValueMatches { it != null && parameter in it.parameters() })
        }
        row {
          cell(createDebugArguments)
        }.visibleIf(kindCombo.selectedValueMatches { it != null })
        row {
          comment(HaxeBundle.message("haxe.custom.action.dialog.launch.hint"))
        }.bottomGap(BottomGap.SMALL)
      }
      create.topGap(TopGap.NONE).bottomGap(BottomGap.NONE)
      existing = collapsibleGroup(HaxeBundle.message("haxe.custom.action.dialog.existing.section")) {
        row(HaxeBundle.message("haxe.custom.action.dialog.existing.configuration")) {
          cell(configurationCombo)
        }.topGap(TopGap.SMALL)
        row {
          cell(existingDebugArguments)
        }.visibleIf(configurationCombo.selectedValueMatches { it != null })
        row {
          comment(HaxeBundle.message("haxe.custom.action.dialog.existing.hint"))
        }.bottomGap(BottomGap.SMALL)
      }
      existing.topGap(TopGap.NONE).bottomGap(BottomGap.NONE)
    }
    // set once the panel exists: the group in use opens, the other stays folded
    create.expanded = initial is Launch.Create
    existing.expanded = initial is Launch.Existing
    when (initial) {
      is Launch.Create -> {
        kindCombo.selectedItem = HaxeLaunchKind.byId(initial.kindId())
        Parameter.fromKeys(initial.parameters()).forEach { (parameter, value) -> fields.getValue(parameter).text = value }
        createDebugArguments.isSelected = initial.injectDebugArguments()
      }
      is Launch.Existing -> {
        configurationCombo.selectedItem = choiceNamed(initial.configurationName())
        existingDebugArguments.isSelected = initial.injectDebugArguments()
      }
      null -> { }
    }
    // either/or: picking in one group clears the other
    kindCombo.addActionListener {
      if (kindCombo.selectedItem != null) configurationCombo.selectedItem = null
      refreshDefaults()
    }
    configurationCombo.addActionListener {
      if (configurationCombo.selectedItem != null) kindCombo.selectedItem = null
    }
    refreshDefaults()
  }

  /** The edited launch, or null for a plain action. Only typed parameter values are kept: a blank field means the default. */
  fun launch(): Launch? {
    val configuration = configurationCombo.selectedItem as? ConfigurationChoice
    if (configuration != null) {
      return Launch.Existing(configuration.name, existingDebugArguments.isSelected)
    }
    val kind = kindCombo.selectedItem as? HaxeLaunchKind ?: return null
    val typed = EnumMap<Parameter, String>(Parameter::class.java)
    for (parameter in kind.parameters()) {
      val text = fields.getValue(parameter).text.trim()
      if (text.isNotEmpty()) typed[parameter] = text
    }
    return Launch.Create(kind.name, Parameter.toKeys(typed), createDebugArguments.isSelected)
  }

  private fun editorFor(parameter: Parameter, field: JBTextField): JComponent = when (parameter.shape()) {
    Shape.TEXT -> field
    Shape.DIRECTORY -> TextFieldWithBrowseButton(field).also { editor ->
      val descriptor = FileChooserDescriptorFactory.singleDir().withTitle(titleOf(parameter))
      editor.addBrowseFolderListener(project, descriptor)
    }
    Shape.FILE -> TextFieldWithBrowseButton(field).also { editor ->
      val descriptor = FileChooserDescriptorFactory.singleFileOrDir()
        .withTitle(titleOf(parameter))
        .withDescription(HaxeBundle.message("haxe.custom.action.dialog.launch.browse.file"))
      editor.addBrowseFolderListener(FileOrFolderListener(descriptor, field))
    }
  }

  /**
   * Opens where the field points, or where its default does while it is blank;
   * a chosen folder keeps the field's file name, so an artifact the build has
   * not written yet can still be pointed at.
   */
  private inner class FileOrFolderListener(descriptor: FileChooserDescriptor, private val field: JBTextField)
    : TextBrowseFolderListener(descriptor, project) {

    override fun getComponentText(): String = field.text.trim().ifEmpty { field.emptyText.text }

    override fun chosenFileToResultingText(chosenFile: VirtualFile): String {
      val path = super.chosenFileToResultingText(chosenFile)
      if (!chosenFile.isDirectory) return path
      val name = pathOf(getComponentText())?.fileName?.toString() ?: return path
      return Path.of(path, name).toString()
    }
  }

  private fun refreshDefaults() {
    val kind = kindCombo.selectedItem as? HaxeLaunchKind
    val values = if (kind == null) emptyMap() else defaults.apply(kind)
    for ((parameter, field) in fields) {
      field.emptyText.text = values[parameter] ?: ""
    }
  }

  /** "None" first, then every registered configuration; a stored name no configuration carries is listed as not found. */
  private fun configurationChoices(initial: Launch?): Array<ConfigurationChoice?> {
    val registered = RunManager.getInstance(project).allSettings
      .map { ConfigurationChoice(it.name, ProgramRunnerUtil.getConfigurationIcon(it, false)) }
      .sortedBy { it.name.lowercase() }
    val choices = mutableListOf<ConfigurationChoice?>(null)
    if (initial is Launch.Existing && registered.none { it.name == initial.configurationName() }) {
      choices.add(ConfigurationChoice(initial.configurationName(), null))
    }
    choices.addAll(registered)
    return choices.toTypedArray()
  }

  private fun choiceNamed(name: String): ConfigurationChoice? {
    val model = configurationCombo.model
    return (0 until model.size).map { model.getElementAt(it) }.firstOrNull { it?.name == name }
  }

  private fun choiceText(choice: ConfigurationChoice?): String = when {
    choice == null -> HaxeBundle.message("haxe.custom.action.dialog.existing.none")
    choice.icon == null -> HaxeBundle.message("haxe.custom.action.dialog.existing.missing", choice.name)
    else -> choice.name
  }

  private companion object {
    /** "None" first, then every kind a custom action may declare. */
    fun kindChoices(): Array<HaxeLaunchKind?> = (listOf<HaxeLaunchKind?>(null) + HaxeLaunchKind.customizable()).toTypedArray()

    /** The field's label without its trailing colon, as a dialog title. */
    fun titleOf(parameter: Parameter): String = parameter.label().removeSuffix(":")

    fun pathOf(text: String): Path? = try {
      if (text.isEmpty()) null else Path.of(text)
    }
    catch (e: InvalidPathException) {
      null
    }
  }
}
