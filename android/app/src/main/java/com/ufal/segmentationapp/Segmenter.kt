package com.ufal.segmentationapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class Segmenter(context: Context) {

    private val interpreter: Interpreter
    private val inputSize = 256

    private val classColors = intArrayOf(
        Color.argb(180, 255, 0, 0),
        Color.argb(180, 0, 255, 0),
        Color.argb(180, 0, 0, 255)
    )

    init {
        interpreter = Interpreter(loadModelFile(context))
    }

    private fun loadModelFile(context: Context): java.nio.MappedByteBuffer {
        val afd = context.assets.openFd("model.tflite")
        val stream = FileInputStream(afd.fileDescriptor)
        return stream.channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
    }

    fun segment(bitmap: Bitmap): Bitmap {
        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val input = bitmapToByteBuffer(resized)

        val output = Array(1) { Array(inputSize) { Array(inputSize) { FloatArray(3) } } }

        interpreter.run(input, output)
        return buildMaskBitmap(output[0])
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
        buffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        for (pixel in pixels) {
            buffer.putFloat(Color.red(pixel) / 255f)
            buffer.putFloat(Color.green(pixel) / 255f)
            buffer.putFloat(Color.blue(pixel) / 255f)
        }
        return buffer
    }

    private fun buildMaskBitmap(output: Array<Array<FloatArray>>): Bitmap {
        val pixels = IntArray(inputSize * inputSize)
        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val scores = output[y][x]
                val classIdx = scores.indices.maxByOrNull { scores[it] } ?: 0
                pixels[y * inputSize + x] = classColors[classIdx]
            }
        }
        return Bitmap.createBitmap(pixels, inputSize, inputSize, Bitmap.Config.ARGB_8888)
    }

    fun close() = interpreter.close()
}
