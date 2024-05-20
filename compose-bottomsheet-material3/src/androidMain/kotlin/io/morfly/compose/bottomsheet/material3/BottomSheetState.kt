/*
 * Copyright 2024 Pavlo Stavytskyi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.morfly.compose.bottomsheet.material3

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch

/**
 * State of a [BottomSheetScaffold] composable.
 *
 * Manages values of the bottom sheet and transitions between them. It also provides the dimension
 * information about the bottom sheet.
 *
 * @param draggableState the [AnchoredDraggableState] that controls the bottom sheet values and drag
 * animations
 * @param defineValues a lambda that defines the values of the bottom sheet
 * @param density the [Density] instance
 */
@ExperimentalFoundationApi
@Stable
class BottomSheetState<T : Any>(
    val draggableState: AnchoredDraggableState<T>,
    internal val defineValues: BottomSheetValuesConfig<T>.() -> Unit,
    internal val density: Density
) {
    /**
     * Callbacks invoked by [refreshValues].
     */
    internal val onRefreshValues = mutableSetOf<(Int, T, Boolean) -> Unit>()

    /**
     * The values of the bottom sheet.
     */
    val values: DraggableAnchors<T> get() = draggableState.anchors

    /**
     * Height of a layout containing a bottom sheet scaffold in pixels.
     */
    var layoutHeight: Int by mutableIntStateOf(Int.MAX_VALUE)
        internal set

    /**
     * Full height of a bottom sheet content including an offscreen part in pixels.
     */
    var sheetFullHeight: Int by mutableIntStateOf(Int.MAX_VALUE)
        internal set

    /**
     * Height of the visible part of a bottom sheet content in pixels.
     */
    val sheetVisibleHeight: Float by derivedStateOf {
        layoutHeight - offset
    }

    val offset: Float get() = draggableState.offset

    /**
     * The current value of the bottom sheet.
     */
    val currentValue: T get() = draggableState.currentValue

    /**
     * The target value. This is the closest value to the current offset, taking into account
     * positional thresholds. If no interactions like animations or drags are in progress, this
     * will be the current value.
     */
    val targetValue: T get() = draggableState.targetValue

    fun requireLayoutHeight(): Int {
        check(layoutHeight != Int.MAX_VALUE) {
            "The layoutHeight was read before being initialized. Did you access the " +
                    "layoutHeight in a phase before layout, like effects or composition?"
        }
        return layoutHeight
    }

    fun requireSheetFullHeight(): Int {
        check(sheetFullHeight != Int.MAX_VALUE) {
            "The sheetFullHeight was read before being initialized. Did you access the " +
                    "sheetFullHeight in a phase before layout, like effects or composition?"
        }
        return sheetFullHeight
    }

    fun requireSheetVisibleHeight(): Float {
        check(!sheetVisibleHeight.isNaN()) {
            "The sheetVisibleHeight was read before being initialized. Did you access the " +
                    "sheetVisibleHeight in a phase before layout, like effects or composition?"
        }
        return sheetVisibleHeight
    }

    fun requireOffset() = draggableState.requireOffset()

    /**
     * Initiate the reconfiguration of the bottom sheet values by calling the [defineValues] lambda.
     *
     * @param targetValue the target value of the bottom sheet after the update
     * @param animate animate the transition to a [targetValue] or snap without any animation.
     */
    fun refreshValues(
        targetValue: T = this.targetValue,
        animate: Boolean = true
    ) {
        if (sheetFullHeight != Int.MAX_VALUE && !offset.isNaN()) {
            onRefreshValues.forEach { call -> call(requireSheetFullHeight(), targetValue, animate) }
        }
    }

    /**
     * Animate to a [targetValue].
     *
     * @param targetValue The target value of the animation
     * @param velocity The velocity the animation should start with
     */
    suspend fun animateTo(
        targetValue: T,
        velocity: Float = draggableState.lastVelocity,
    ) = draggableState.animateTo(targetValue, velocity)

    /**
     * Snap to a [targetValue] without any animation.
     *
     * @param targetValue The target value of the animation
     */
    suspend fun snapTo(
        targetValue: T
    ) = draggableState.snapTo(targetValue)

    companion object {

        /**
         * The default [Saver] implementation for [BottomSheetState].
         */
        fun <T : Any> Saver(
            defineValues: BottomSheetValuesConfig<T>.() -> Unit,
            density: Density
        ) = Saver<BottomSheetState<T>, AnchoredDraggableState<T>>(
            save = { it.draggableState },
            restore = { draggableState ->
                BottomSheetState(draggableState, defineValues, density)
            }
        )
    }
}

@ExperimentalFoundationApi
val <T : Any> BottomSheetState<T>.layoutHeightDp: Dp
    get() = with(density) { layoutHeight.toDp() }

@ExperimentalFoundationApi
val <T : Any> BottomSheetState<T>.sheetFullHeightDp: Dp
    get() = with(density) { sheetFullHeight.toDp() }

@ExperimentalFoundationApi
val <T : Any> BottomSheetState<T>.sheetVisibleHeightDp: Dp
    get() = with(density) { sheetVisibleHeight.toDp() }

@ExperimentalFoundationApi
val <T : Any> BottomSheetState<T>.offsetDp: Dp
    get() = with(density) { offset.toDp() }

@ExperimentalFoundationApi
fun <T : Any> BottomSheetState<T>.requireLayoutHeightDp(): Dp {
    return with(density) { requireLayoutHeight().toDp() }
}

@ExperimentalFoundationApi
fun <T : Any> BottomSheetState<T>.requireSheetFullHeightDp(): Dp {
    return with(density) { requireSheetFullHeight().toDp() }
}

@ExperimentalFoundationApi
fun <T : Any> BottomSheetState<T>.requireSheetVisibleHeightDp(): Dp {
    return with(density) { requireSheetVisibleHeight().toDp() }
}

@ExperimentalFoundationApi
fun <T : Any> BottomSheetState<T>.requireOffsetDp(): Dp {
    return with(density) { requireOffset().toDp() }
}

/**
 * Create and [remember] a [BottomSheetState].
 *
 * @param initialValue the initial value of the state
 * @param defineValues a lambda that defines the values of the bottom sheet
 * @param positionalThreshold the positional threshold, in px, to be used when calculating the
 * target state while a drag is in progress and when settling after the drag ends. This is the
 * distance from the start of a transition. It will be, depending on the direction of the
 * interaction, added or subtracted from/to the origin offset. It should always be a positive value
 * @param velocityThreshold the velocity threshold (in px per second) that the end velocity has to
 * exceed in order to animate to the next state, even if the [positionalThreshold] has not been
 * reached
 * @param animationSpec the default animation that will be used to animate to a new state
 * @param confirmValueChange optional callback invoked to confirm or veto a pending state change
 */
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun <T : Any> rememberBottomSheetState(
    initialValue: T,
    defineValues: BottomSheetValuesConfig<T>.() -> Unit,
    positionalThreshold: (totalDistance: Float) -> Float = BottomSheetDefaults.PositionalThreshold,
    velocityThreshold: () -> Float = BottomSheetDefaults.VelocityThreshold,
    animationSpec: AnimationSpec<Float> = BottomSheetDefaults.AnimationSpec,
    confirmValueChange: BottomSheetState<T>.(T) -> Boolean = { true }
): BottomSheetState<T> {
    lateinit var state: BottomSheetState<T>
    lateinit var draggableState: AnchoredDraggableState<T>

    val scope = rememberCoroutineScope()
    var prevOffset by remember { mutableFloatStateOf(0f) }

    draggableState = rememberAnchoredDraggableState(
        initialValue = initialValue,
        positionalThreshold = positionalThreshold,
        velocityThreshold = velocityThreshold,
        animationSpec = animationSpec,
        confirmValueChange = { value ->
            // TODO comment bug workaround
            with(draggableState) {
                val currentOffset = requireOffset()
                val searchUpwards =
                    if (prevOffset == currentOffset) null
                    else prevOffset < currentOffset

                prevOffset = currentOffset
                if (!anchors.hasAnchorFor(value)) {
                    val closest = if (searchUpwards != null) {
                        anchors.closestAnchor(currentOffset, searchUpwards)
                    } else {
                        anchors.closestAnchor(currentOffset)
                    }
                    if (closest != null) {
                        scope.launch { animateTo(closest) }
                    }
                    false
                } else {
                    state.confirmValueChange(value)
                }
            }
        }
    )

    state = rememberBottomSheetState(draggableState, defineValues)
    return state
}

@ExperimentalFoundationApi
@Composable
internal fun <T : Any> rememberBottomSheetState(
    draggableState: AnchoredDraggableState<T>,
    defineValues: BottomSheetValuesConfig<T>.() -> Unit,
): BottomSheetState<T> {
    val density = LocalDensity.current

    return remember(draggableState) {
        BottomSheetState(draggableState, defineValues, density)
    }
}
