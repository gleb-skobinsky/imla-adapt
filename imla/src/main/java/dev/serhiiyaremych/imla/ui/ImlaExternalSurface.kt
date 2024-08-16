package dev.serhiiyaremych.imla.ui

import android.graphics.PixelFormat
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

public interface SurfaceScope {
    /**
     * Invokes [onChanged] when the surface's geometry (width and height) changes.
     * Always invoked on the main thread.
     */
    public fun Surface.onChanged(onChanged: Surface.(width: Int, height: Int) -> Unit)

    /**
     * Invokes [onDestroyed] when the surface is destroyed. All rendering into
     * the surface should stop immediately after [onDestroyed] is invoked.
     * Always invoked on the main thread.
     */
    public fun Surface.onDestroyed(onDestroyed: Surface.() -> Unit)
}

public interface SurfaceCoroutineScope : SurfaceScope, CoroutineScope

public interface AndroidExternalSurfaceScope {
    /**
     * Invokes [onSurface] when a new [Surface] is created. The [onSurface] lambda
     * is invoked on the main thread as part of a [SurfaceCoroutineScope] to provide
     * a coroutine context. Always invoked on the main thread.
     *
     * @param onSurface Callback invoked when a new [Surface] is created. The initial
     *                  dimensions of the surface are provided.
     */
    public fun onSurface(
        onSurface: suspend SurfaceCoroutineScope.(surface: Surface, width: Int, height: Int) -> Unit
    )
}

private abstract class BaseAndroidExternalSurfaceState(val scope: CoroutineScope) :
    AndroidExternalSurfaceScope, SurfaceScope {

    private var onSurface:
            (suspend SurfaceCoroutineScope.(surface: Surface, width: Int, height: Int) -> Unit)? =
        null
    private var onSurfaceChanged: (Surface.(width: Int, height: Int) -> Unit)? = null
    private var onSurfaceDestroyed: (Surface.() -> Unit)? = null

    private var job: Job? = null

    override fun onSurface(
        onSurface: suspend SurfaceCoroutineScope.(surface: Surface, width: Int, height: Int) -> Unit
    ) {
        this.onSurface = onSurface
    }

    override fun Surface.onChanged(onChanged: Surface.(width: Int, height: Int) -> Unit) {
        onSurfaceChanged = onChanged
    }

    override fun Surface.onDestroyed(onDestroyed: Surface.() -> Unit) {
        onSurfaceDestroyed = onDestroyed
    }

    /**
     * Dispatch a surface creation event by launching a new coroutine in [scope].
     * Any previous job from a previous surface creation dispatch is cancelled.
     */
    fun dispatchSurfaceCreated(surface: Surface, width: Int, height: Int) {
        if (onSurface != null) {
            job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                job?.cancelAndJoin()
                val receiver =
                    object : SurfaceCoroutineScope,
                        SurfaceScope by this@BaseAndroidExternalSurfaceState,
                        CoroutineScope by this {}
                onSurface?.invoke(receiver, surface, width, height)
            }
        }
    }

    /**
     * Dispatch a surface change event, providing the surface's new width and height.
     * Must be invoked from the main thread.
     */
    fun dispatchSurfaceChanged(surface: Surface, width: Int, height: Int) {
        onSurfaceChanged?.invoke(surface, width, height)
    }

    /**
     * Dispatch a surface destruction event. Any pending job from [dispatchSurfaceCreated]
     * is cancelled before dispatching the event. Must be invoked from the main thread.
     */
    fun dispatchSurfaceDestroyed(surface: Surface) {
        onSurfaceDestroyed?.invoke(surface)
        job?.cancel()
        job = null
    }
}

private class AndroidExternalSurfaceState(scope: CoroutineScope) :
    BaseAndroidExternalSurfaceState(scope), SurfaceHolder.Callback {

    var lastWidth = -1
    var lastHeight = -1

    override fun surfaceCreated(holder: SurfaceHolder) {
        val frame = holder.surfaceFrame
        lastWidth = frame.width()
        lastHeight = frame.height()

        dispatchSurfaceCreated(holder.surface, lastWidth, lastHeight)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (lastWidth != width || lastHeight != height) {
            lastWidth = width
            lastHeight = height

            dispatchSurfaceChanged(holder.surface, width, height)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        dispatchSurfaceDestroyed(holder.surface)
    }
}

@Composable
private fun rememberAndroidExternalSurfaceState(): AndroidExternalSurfaceState {
    val scope = rememberCoroutineScope()
    return remember { AndroidExternalSurfaceState(scope) }
}

@JvmInline
public value class AndroidExternalSurfaceZOrder private constructor(public val zOrder: Int) {
    public companion object {
        /**
         * The [Surface]'s window layer is positioned behind the parent window.
         */
        public val Behind: AndroidExternalSurfaceZOrder = AndroidExternalSurfaceZOrder(0)

        /**
         * The [Surface]'s window layer is positioned behind the parent window but
         * above other [Surface] window layers marked [Behind].
         */
        public val MediaOverlay: AndroidExternalSurfaceZOrder = AndroidExternalSurfaceZOrder(1)

        /**
         * The [Surface]'s window layer is positioned above the parent window.
         */
        public val OnTop: AndroidExternalSurfaceZOrder = AndroidExternalSurfaceZOrder(2)
    }
}

@Composable
public fun ImlaExternalSurface(
    modifier: Modifier = Modifier,
    isOpaque: Boolean = true,
    surfaceSize: IntSize = IntSize.Zero,
    zOrder: AndroidExternalSurfaceZOrder = AndroidExternalSurfaceZOrder.Behind,
    isSecure: Boolean = false,
    onUpdate: SurfaceView.(Surface) -> Unit,
    onInit: AndroidExternalSurfaceScope.() -> Unit
) {
    val state = rememberAndroidExternalSurfaceState()
    AndroidView(
        factory = { context ->
            SurfaceView(context).apply {
                state.onInit()
                holder.addCallback(state)
            }
        },
        modifier = modifier,
        onReset = { },
        update = { view ->
            if (surfaceSize != IntSize.Zero) {
                view.holder.setFixedSize(surfaceSize.width, surfaceSize.height)
            } else {
                view.holder.setSizeFromLayout()
            }

            view.holder.setFormat(
                if (isOpaque) {
                    PixelFormat.OPAQUE
                } else {
                    PixelFormat.TRANSLUCENT
                }
            )

            when (zOrder) {
                AndroidExternalSurfaceZOrder.Behind -> view.setZOrderOnTop(false)
                AndroidExternalSurfaceZOrder.MediaOverlay -> view.setZOrderMediaOverlay(true)
                AndroidExternalSurfaceZOrder.OnTop -> view.setZOrderOnTop(true)
            }

            view.setSecure(isSecure)

            if (view.holder.surface.isValid) {
                view.onUpdate(view.holder.surface)
            }
        }
    )
}