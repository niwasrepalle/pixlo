package com.pixlo.app

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.RectF
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object SubjectDetection {
    fun boxes(context: Context, file: File): List<RectF> {
        val prefs = context.getSharedPreferences("subjects", Context.MODE_PRIVATE)
        val key = file.name
        val cached = prefs.getString(key, null)
        if (cached != null) {
            val a = JSONArray(cached)
            return (0 until a.length()).map { i -> val b = a.getJSONObject(i); RectF(b.getDouble("l").toFloat(), b.getDouble("t").toFloat(), b.getDouble("r").toFloat(), b.getDouble("b").toFloat()) }
        }
        val image = BitmapFactory.decodeFile(file.absolutePath) ?: return emptyList()
        val detector = ObjectDetection.getClient(ObjectDetectorOptions.Builder().setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE).enableMultipleObjects().build())
        val task = try { detector.process(InputImage.fromBitmap(image, 0)) }
            catch (_: Exception) { detector.close(); image.recycle(); return emptyList() }
        return try {
            val objects = Tasks.await(task, 15, TimeUnit.SECONDS)
            val boxes = objects.map { o -> val b = o.boundingBox; RectF(b.left.toFloat() / image.width, b.top.toFloat() / image.height, b.right.toFloat() / image.width, b.bottom.toFloat() / image.height) }
            val a = JSONArray()
            boxes.forEach { b -> a.put(JSONObject().put("l", b.left).put("t", b.top).put("r", b.right).put("b", b.bottom)) }
            prefs.edit().putString(key, a.toString()).apply()
            boxes
        } catch (_: Exception) { emptyList() } finally {
            if (task.isComplete) { detector.close(); image.recycle() }
            else task.addOnCompleteListener { detector.close(); image.recycle() }
        }
    }
}
