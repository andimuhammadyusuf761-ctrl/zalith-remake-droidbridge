package com.movtery.zalithlauncher.ui.fragment

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.movtery.anim.AnimPlayer
import com.movtery.anim.animations.Animations
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.feature.macro.Macro
import com.movtery.zalithlauncher.feature.macro.MacroEngine
import com.movtery.zalithlauncher.feature.macro.MacroStep
import com.movtery.zalithlauncher.feature.macro.MacroStepType
import com.movtery.zalithlauncher.feature.macro.MacroStore
import com.movtery.zalithlauncher.utils.ZHTools

/**
 * Settings → Controls → Macros entry point.
 *
 * Two modes presented in a single fragment:
 *  - List mode: cards for each existing macro with edit / delete / test buttons.
 *  - Editor mode: a name field, a loop count, and a step list with per-step delays.
 *
 * Implemented as one fragment to keep the back-stack short — pressing back from the
 * editor returns to the list, and back from the list returns to the settings page.
 */
class MacroManagerFragment : FragmentWithAnim(R.layout.fragment_macro_list) {
    companion object {
        const val TAG = "MacroManagerFragment"
    }

    private lateinit var listRoot: View
    private lateinit var recycler: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var addButton: Button

    private val macros = mutableListOf<Macro>()
    private val listAdapter = MacroListAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        listRoot = inflater.inflate(R.layout.fragment_macro_list, container, false)
        return listRoot
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recycler = view.findViewById(R.id.macro_recycler_view)
        emptyText = view.findViewById(R.id.macro_empty_text)
        addButton = view.findViewById(R.id.macro_add_button)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = listAdapter

        addButton.setOnClickListener { openEditor(Macro(name = "Macro " + (macros.size + 1))) }
        loadMacros()
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(listRoot, Animations.BounceInDown))
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(listRoot, Animations.FadeOutUp))
    }

    private fun loadMacros() {
        macros.clear()
        macros.addAll(MacroStore.load(requireContext()))
        listAdapter.notifyDataSetChanged()
        emptyText.visibility = if (macros.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openEditor(macro: Macro) {
        val ctx = requireContext()
        val editor = MacroEditorView(ctx, macro,
            onSave = { saved ->
                MacroStore.upsert(ctx, saved)
                Toast.makeText(ctx, R.string.generic_saved, Toast.LENGTH_SHORT).show()
                loadMacros()
            }
        )
        AlertDialog.Builder(ctx)
            .setTitle(R.string.macro_edit_title)
            .setView(editor)
            .setPositiveButton(R.string.generic_save) { _, _ -> editor.commit() }
            .setNegativeButton(R.string.generic_close, null)
            .show()
    }

    private inner class MacroListAdapter : RecyclerView.Adapter<MacroListAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.macro_item_name)
            val sub: TextView = v.findViewById(R.id.macro_item_summary)
            val run: ImageButton = v.findViewById(R.id.macro_item_run)
            val edit: ImageButton = v.findViewById(R.id.macro_item_edit)
            val del: ImageButton = v.findViewById(R.id.macro_item_delete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_macro, parent, false)
            return VH(v)
        }

        override fun getItemCount(): Int = macros.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val m = macros[position]
            holder.name.text = m.name
            val loops = if (m.loopCount == 0) "∞" else m.loopCount.toString()
            holder.sub.text = holder.itemView.context.getString(
                R.string.macro_summary, m.steps.size, m.totalDelayMs().toInt(), loops
            )
            holder.run.setOnClickListener {
                if (MacroEngine.isRunning(m.id)) MacroEngine.stop(m.id)
                else MacroEngine.run(m)
            }
            holder.edit.setOnClickListener { openEditor(m.deepCopy()) }
            holder.del.setOnClickListener {
                AlertDialog.Builder(holder.itemView.context)
                    .setMessage(holder.itemView.context.getString(R.string.macro_edit) + ": " + m.name + " ?")
                    .setPositiveButton(R.string.generic_delete) { _, _ ->
                        MacroStore.delete(holder.itemView.context, m.id)
                        loadMacros()
                    }
                    .setNegativeButton(R.string.generic_close, null)
                    .show()
            }
        }
    }
}

/**
 * Inline editor view dropped into an [AlertDialog] so the editor doesn't require its
 * own back-stack entry.
 */
private class MacroEditorView(
    context: Context,
    private val original: Macro,
    private val onSave: (Macro) -> Unit
) : LinearLayout(context) {
    private val editing = original.deepCopy()
    private val stepsRecycler: RecyclerView
    private val nameField: EditText
    private val loopField: EditText
    private val stepsAdapter = StepsAdapter()

    init {
        orientation = VERTICAL
        val pad = (resources.displayMetrics.density * 12).toInt()
        setPadding(pad, pad, pad, pad)

        nameField = EditText(context).apply {
            hint = context.getString(R.string.macro_name_hint)
            setText(editing.name)
        }
        addView(nameField, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val loopRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
        }
        val loopLabel = TextView(context).apply {
            text = context.getString(R.string.macro_loop_hint)
        }
        loopField = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(editing.loopCount.toString())
            hint = context.getString(R.string.macro_loop_helper)
        }
        loopRow.addView(loopLabel, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        loopRow.addView(loopField, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        addView(loopRow, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = pad / 2
        })

        val helper = TextView(context).apply {
            text = context.getString(R.string.macro_loop_helper)
            textSize = 11f
        }
        addView(helper)

        val stepsHeader = LinearLayout(context).apply {
            orientation = HORIZONTAL
        }
        val stepsTitle = TextView(context).apply {
            text = context.getString(R.string.macro_steps_title)
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 14f
        }
        val addStep = Button(context).apply {
            text = context.getString(R.string.macro_add_step)
            setOnClickListener {
                openStepEditor(MacroStep()) { newStep ->
                    editing.steps.add(newStep)
                    stepsAdapter.notifyItemInserted(editing.steps.size - 1)
                }
            }
        }
        val testRun = Button(context).apply {
            text = context.getString(R.string.macro_run_test)
            setOnClickListener {
                commit(autoSave = false)
                MacroEngine.run(editing.deepCopy())
            }
        }
        stepsHeader.addView(stepsTitle, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        stepsHeader.addView(testRun, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        stepsHeader.addView(addStep, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        addView(stepsHeader, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = pad
        })

        stepsRecycler = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = stepsAdapter
            minimumHeight = (resources.displayMetrics.density * 240).toInt()
        }
        addView(stepsRecycler, LayoutParams(LayoutParams.MATCH_PARENT, (resources.displayMetrics.density * 240).toInt()))
    }

    /**
     * Persist the current edits back to MacroStore (when [autoSave] true) and notify
     * the parent fragment via [onSave].
     */
    fun commit(autoSave: Boolean = true) {
        editing.name = nameField.text?.toString()?.takeIf { it.isNotBlank() } ?: editing.name
        editing.loopCount = loopField.text?.toString()?.toIntOrNull() ?: editing.loopCount
        if (editing.loopCount < 0) editing.loopCount = 0
        if (autoSave) onSave(editing.deepCopy())
    }

    private fun openStepEditor(step: MacroStep, onDone: (MacroStep) -> Unit) {
        val ctx = context
        val container = LinearLayout(ctx).apply {
            orientation = VERTICAL
            val p = (resources.displayMetrics.density * 12).toInt()
            setPadding(p, p, p, p)
        }

        val typeNames = listOf(
            ctx.getString(R.string.macro_step_type_key_tap),
            ctx.getString(R.string.macro_step_type_key_hold),
            ctx.getString(R.string.macro_step_type_key_release),
            ctx.getString(R.string.macro_step_type_mouse_left),
            ctx.getString(R.string.macro_step_type_mouse_right),
            ctx.getString(R.string.macro_step_type_mouse_middle),
            ctx.getString(R.string.macro_step_type_delay)
        )
        val typeOrder = listOf(
            MacroStepType.KEY_TAP,
            MacroStepType.KEY_HOLD,
            MacroStepType.KEY_RELEASE,
            MacroStepType.MOUSE_LEFT,
            MacroStepType.MOUSE_RIGHT,
            MacroStepType.MOUSE_MIDDLE,
            MacroStepType.DELAY
        )
        val typeLabel = TextView(ctx).apply { text = ctx.getString(R.string.macro_step_type) }
        val spinner = Spinner(ctx).apply {
            adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, typeNames)
            setSelection(typeOrder.indexOf(step.type).coerceAtLeast(0))
        }
        container.addView(typeLabel)
        container.addView(spinner)

        val keyLabel = TextView(ctx).apply { text = ctx.getString(R.string.macro_step_keycode) }
        val keyField = EditText(ctx).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
            setText(step.keycode.toString())
        }
        container.addView(keyLabel)
        container.addView(keyField)

        val delayLabel = TextView(ctx).apply { text = ctx.getString(R.string.macro_step_delay) }
        val delayField = EditText(ctx).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(step.delayMs.toString())
        }
        container.addView(delayLabel)
        container.addView(delayField)

        AlertDialog.Builder(ctx)
            .setTitle(R.string.macro_step_save)
            .setView(container)
            .setPositiveButton(R.string.macro_step_save) { _, _ ->
                step.type = typeOrder[spinner.selectedItemPosition]
                step.keycode = keyField.text?.toString()?.toIntOrNull() ?: 0
                step.delayMs = delayField.text?.toString()?.toLongOrNull()?.coerceAtLeast(0) ?: 0L
                onDone(step)
            }
            .setNegativeButton(R.string.generic_close, null)
            .show()
    }

    private inner class StepsAdapter : RecyclerView.Adapter<StepsAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val idx: TextView = v.findViewById(R.id.step_index)
            val label: TextView = v.findViewById(R.id.step_label)
            val sub: TextView = v.findViewById(R.id.step_sub)
            val up: ImageButton = v.findViewById(R.id.step_up)
            val down: ImageButton = v.findViewById(R.id.step_down)
            val edit: ImageButton = v.findViewById(R.id.step_edit)
            val del: ImageButton = v.findViewById(R.id.step_delete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_macro_step, parent, false)
            return VH(v)
        }

        override fun getItemCount(): Int = editing.steps.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val s = editing.steps[position]
            holder.idx.text = (position + 1).toString()
            holder.label.text = describeStep(holder.itemView.context, s)
            holder.sub.text = "delay " + s.delayMs + "ms"
            holder.up.setOnClickListener {
                if (position > 0) {
                    val tmp = editing.steps[position]
                    editing.steps[position] = editing.steps[position - 1]
                    editing.steps[position - 1] = tmp
                    notifyItemMoved(position, position - 1)
                }
            }
            holder.down.setOnClickListener {
                if (position < editing.steps.size - 1) {
                    val tmp = editing.steps[position]
                    editing.steps[position] = editing.steps[position + 1]
                    editing.steps[position + 1] = tmp
                    notifyItemMoved(position, position + 1)
                }
            }
            holder.edit.setOnClickListener {
                openStepEditor(s.copyOf()) { updated ->
                    editing.steps[position] = updated
                    notifyItemChanged(position)
                }
            }
            holder.del.setOnClickListener {
                editing.steps.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, editing.steps.size)
            }
        }

        private fun describeStep(ctx: Context, s: MacroStep): String {
            return when (s.type) {
                MacroStepType.KEY_TAP -> ctx.getString(R.string.macro_step_type_key_tap) + " (" + s.keycode + ")"
                MacroStepType.KEY_HOLD -> ctx.getString(R.string.macro_step_type_key_hold) + " (" + s.keycode + ")"
                MacroStepType.KEY_RELEASE -> ctx.getString(R.string.macro_step_type_key_release) + " (" + s.keycode + ")"
                MacroStepType.MOUSE_LEFT -> ctx.getString(R.string.macro_step_type_mouse_left)
                MacroStepType.MOUSE_RIGHT -> ctx.getString(R.string.macro_step_type_mouse_right)
                MacroStepType.MOUSE_MIDDLE -> ctx.getString(R.string.macro_step_type_mouse_middle)
                MacroStepType.DELAY -> ctx.getString(R.string.macro_step_type_delay)
            }
        }
    }
}
