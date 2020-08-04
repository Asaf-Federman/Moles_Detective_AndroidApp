package yearly_project.frontend.kotlin

import android.annotation.SuppressLint
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import androidx.camera.core.*
import androidx.camera.view.PreviewView
import java.util.concurrent.TimeUnit

class FocusUtilities {
    private inline fun View.afterMeasured(crossinline block: () -> Unit) {
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (measuredWidth > 0 && measuredHeight > 0) {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    block()
                }
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    fun focusOnTap(previewView: PreviewView, camera: Camera){
        previewView.afterMeasured {
            previewView.setOnTouchListener { _, event ->
                return@setOnTouchListener when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                                previewView.width.toFloat(), previewView.height.toFloat()
                        )
                        val autoFocusPoint = factory.createPoint(event.x, event.y)
                        try {
                            camera.cameraControl.startFocusAndMetering(
                                    FocusMeteringAction.Builder(
                                            autoFocusPoint,
                                            FocusMeteringAction.FLAG_AF
                                    ).apply {
                                        //focus only when the user tap the preview
                                        disableAutoCancel()
                                    }.build()
                            )
                        } catch (e: CameraInfoUnavailableException) {
                            Log.d("ERROR", "cannot access camera", e)
                        }
                        true
                    }
                    else -> false // Unhandled event.
                }
            }
        }
    }

    fun autoFocus(previewView: PreviewView, camera: Camera){
        previewView.afterMeasured {
            val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                    previewView.width.toFloat(), previewView.height.toFloat())
            val centerWidth = previewView.width.toFloat() / 2
            val centerHeight = previewView.height.toFloat() / 2
            //create a point on the center of the view
            val autoFocusPoint = factory.createPoint(centerWidth, centerHeight)
            try {
                camera.cameraControl.startFocusAndMetering(
                        FocusMeteringAction.Builder(
                                autoFocusPoint,
                                FocusMeteringAction.FLAG_AF
                        ).apply {
                            //auto-focus every 1 seconds
                            setAutoCancelDuration(1, TimeUnit.SECONDS)
                        }.build()
                )
            } catch (e: CameraInfoUnavailableException) {
                Log.d("ERROR", "cannot access camera", e)
            }
        }
    }
}