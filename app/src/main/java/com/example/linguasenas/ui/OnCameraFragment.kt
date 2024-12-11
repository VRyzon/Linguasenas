package com.example.linguasenas.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.linguasenas.ml.Model
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.example.linguasenas.R
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController

class OnCameraFragment : Fragment(R.layout.oncamera_fragment) {

    private lateinit var camera: Button
    private lateinit var gallery: Button
    private lateinit var imageView: ImageView
    private lateinit var result: TextView
    private val imageSize = 64
    private lateinit var buttonCameraRealTime: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        camera = view.findViewById(R.id.button)
        gallery = view.findViewById(R.id.button2)
        result = view.findViewById(R.id.result)
        imageView = view.findViewById(R.id.imageView)

        camera.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cameraIntent, 3)
            } else {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), 100)
            }
        }

        gallery.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, 1)
        }

        buttonCameraRealTime = view.findViewById(R.id.button_camera_real_time)

        buttonCameraRealTime.setOnClickListener {
            findNavController().navigate(R.id.action_onCameraFragment_to_cameraRealTimeFragment)
        }
    }

    private fun classifyImage(image: Bitmap) {
        try {
            val model = Model.newInstance(requireContext())

            // Create input tensor.
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 64, 64, 3), DataType.FLOAT32)
            val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
            byteBuffer.order(ByteOrder.nativeOrder())

            val intValues = IntArray(imageSize * imageSize)
            image.getPixels(intValues, 0, image.width, 0, 0, image.width, image.height)
            var pixel = 0

            // Extract R, G, B values for each pixel and add them to the byte buffer.
            for (i in 0 until imageSize) {
                for (j in 0 until imageSize) {
                    val pixelValue = intValues[pixel++] // RGB
                    byteBuffer.putFloat(((pixelValue shr 16) and 0xFF) / 255.0f)  // R
                    byteBuffer.putFloat(((pixelValue shr 8) and 0xFF) / 255.0f)   // G
                    byteBuffer.putFloat((pixelValue and 0xFF) / 255.0f)          // B
                }
            }

            inputFeature0.loadBuffer(byteBuffer)

            // Run model inference and get the result.
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer

            val confidences = outputFeature0.floatArray

            // Define possible classes.
            val classes = arrayOf("A", "B", "C", "D", "E", "F","N/A")

            // Log each confidence with its corresponding class.
            val logString = StringBuilder("Classifications:\n")
            for (i in confidences.indices) {
                logString.append("Class ").append(classes[i])
                    .append(": ").append(confidences[i]).append("\n")
            }

            Log.d("TensorFlowLiteOutput", logString.toString())

            // Find the index of the class with the highest confidence.
            var maxPos = 0
            var maxConfidence = 0f
            for (i in confidences.indices) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i]
                    maxPos = i
                }
            }
            result.text = classes[maxPos]

            // Release model resources if no longer used.
            model.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == AppCompatActivity.RESULT_OK) {
            if (requestCode == 3) {
                val image = data?.extras?.get("data") as Bitmap
                val dimension = Math.min(image.width, image.height)
                val thumbnail = ThumbnailUtils.extractThumbnail(image, dimension, dimension)
                imageView.setImageBitmap(thumbnail)

                val scaledImage = Bitmap.createScaledBitmap(thumbnail, imageSize, imageSize, false)
                classifyImage(scaledImage)
            } else {
                val imageUri = data?.data
                var image: Bitmap? = null
                try {
                    image = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, imageUri)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                imageView.setImageBitmap(image)

                val scaledImage = Bitmap.createScaledBitmap(image ?: return, imageSize, imageSize, false)
                classifyImage(scaledImage)
            }
        }
    }
}
