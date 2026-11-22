package cz.kutner.why.ui

import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.annotation.StringRes
import cz.kutner.why.R
import cz.kutner.why.domain.Trick
import cz.kutner.why.domain.TrickGroup

@get:StringRes
val Trick.title: Int
    get() = when (this) {
        Trick.Pause -> R.string.trick_pause
        Trick.Stagger -> R.string.trick_stagger
        Trick.EarlyReset -> R.string.trick_early_reset
        Trick.Hold -> R.string.trick_hold
        Trick.SecondCheck -> R.string.trick_second_check
        Trick.Shuffle -> R.string.trick_shuffle
        Trick.HabitTrap -> R.string.trick_habit_trap
        Trick.NewGrid -> R.string.trick_new_grid
        Trick.Wild -> R.string.trick_wild
        Trick.Drift -> R.string.trick_drift
        Trick.Pebbles -> R.string.trick_pebbles
        Trick.Scramble -> R.string.trick_scramble
        Trick.TypeIt -> R.string.trick_type_it
        Trick.Question -> R.string.trick_question
        Trick.Tilt -> R.string.trick_tilt
        Trick.Breath -> R.string.trick_breath
        Trick.DragToJar -> R.string.trick_drag
    }

/** Also the hint shown the first time the trick appears, so it says what to do. */
@get:StringRes
val Trick.description: Int
    get() = when (this) {
        Trick.Pause -> R.string.trick_pause_text
        Trick.Stagger -> R.string.trick_stagger_text
        Trick.EarlyReset -> R.string.trick_early_reset_text
        Trick.Hold -> R.string.trick_hold_text
        Trick.SecondCheck -> R.string.trick_second_check_text
        Trick.Shuffle -> R.string.trick_shuffle_text
        Trick.HabitTrap -> R.string.trick_habit_trap_text
        Trick.NewGrid -> R.string.trick_new_grid_text
        Trick.Wild -> R.string.trick_wild_text
        Trick.Drift -> R.string.trick_drift_text
        Trick.Pebbles -> R.string.trick_pebbles_text
        Trick.Scramble -> R.string.trick_scramble_text
        Trick.TypeIt -> R.string.trick_type_it_text
        Trick.Question -> R.string.trick_question_text
        Trick.Tilt -> R.string.trick_tilt_text
        Trick.Breath -> R.string.trick_breath_text
        Trick.DragToJar -> R.string.trick_drag_text
    }

@get:StringRes
val TrickGroup.title: Int
    get() = when (this) {
        TrickGroup.Timing -> R.string.trick_group_timing
        TrickGroup.Layout -> R.string.trick_group_layout
        TrickGroup.Words -> R.string.trick_group_words
        TrickGroup.Body -> R.string.trick_group_body
    }

@get:StringRes
val questions = listOf(R.string.prompt_question_1, R.string.prompt_question_2, R.string.prompt_question_3, R.string.prompt_question_4)

/** A screen reader that explores by touch, such as TalkBack, is on. */
fun Context.screenReaderOn(): Boolean = getSystemService(AccessibilityManager::class.java).isTouchExplorationEnabled

/** The system setting Remove animations is on. */
fun Context.animationsOff(): Boolean = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
