package com.example.linguasenas.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.linguasenas.R
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.camera.view.PreviewView
import com.example.imagepicker1.Classifier
import kotlinx.coroutines.launch

class CameraRealTimeFragment : Fragment(R.layout.camera_real_time_fragment) {

    private lateinit var previewView: PreviewView
    private lateinit var textView: TextView
    private lateinit var classifier: Classifier

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.camera_real_time_fragment, container, false)

        previewView = view.findViewById(R.id.previewView)
        textView = view.findViewById(R.id.textView)

        // Initialize the Classifier (Model)
        classifier = Classifier(requireContext().assets, "model.tflite", "labels.txt", 64)

        // Request Camera permission
        if (ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.CAMERA
            ) == PermissionChecker.PERMISSION_GRANTED
        ) {
            if (isAdded) {
                startCamera()
            }
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(android.Manifest.permission.CAMERA), 0
            )
        }

        return view
    }

    private fun startCamera() {
        // Inicializar CameraX
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Caso de uso de vista previa
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // Caso de uso de ImageAnalysis para capturar fotogramas de la cámara
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1920, 1080)) // Resolución más alta
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
                .build()

            // Usar el executor para analizar las imágenes
            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                processImage(imageProxy)
            }

            // Selector de cámara
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK) // Usar la cámara trasera
                .build()

            try {
                // Desvincular los casos de uso previos
                cameraProvider.unbindAll()

                // Vincular la cámara con Preview y ImageAnalysis
                cameraProvider.bindToLifecycle(
                    this as LifecycleOwner, cameraSelector, preview, imageAnalysis
                )
            } catch (exc: Exception) {
                Log.e("CameraX", "Binding use case failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // Function to run inference on the camera frames
    private var frameCount = 0
    private val frameInterval = 20  // Procesar cada 5 fotogramas

    private fun processImage(image: ImageProxy) {
        // Incrementar el contador de fotogramas
        frameCount++

        // Procesar cada fotograma si el contador alcanza el intervalo (20 fotogramas para 1 segundo)
        if (frameCount >= frameInterval) {
            // Convertir ImageProxy a Bitmap (usa el método necesario)
            val bitmap = image.toBitmap()

            // Ejecutar la inferencia y obtener el resultado
            val result = classifier.recognizeImageTopLabels(bitmap)
            Log.d("Classifier", result.toString())

            // Asegúrate de actualizar la UI en el hilo principal
            lifecycleScope.launch {
                // Verificar si el fragmento sigue adjunto antes de actualizar la UI
                if (isAdded) {
                    textView.text = "$result"  // Actualiza el TextView
                }
            }

            // Reiniciar el contador de fotogramas
            frameCount = 0
        }

        // Cerrar la imagen para evitar fugas de memoria
        image.close()
    }

    // You should implement a method to convert ImageProxy to Bitmap if necessary
    private fun ImageProxy.toBitmap(): Bitmap {
        // Conversion logic
        val buffer: ByteBuffer = this.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}
