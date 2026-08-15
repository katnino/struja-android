package ba.noniboy.struja.ui.components

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

/**
 * CameraX preview for capturing meter photos.
 *
 * Usage in Compose:
 * ```
 * CameraPreview(
 *     modifier = Modifier.fillMaxWidth(),
 *     cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
 *     onImageCaptureReady = { imageCapture -> /* store for later use */ }
 * )
 * ```
 *
 * Note: Requires CAMERA permission to be granted before use.
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    onPreviewViewReady: ((PreviewView) -> Unit)? = null,
    onImageCaptureReady: ((ImageCapture) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val imageCapture = ImageCapture.Builder()
                        .setTargetRotation(previewView.display?.rotation ?: 0)
                        .build()

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )

                    onPreviewViewReady?.invoke(previewView)
                    onImageCaptureReady?.invoke(imageCapture)
                } catch (e: Exception) {
                    // Camera permission not granted or camera unavailable
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}
