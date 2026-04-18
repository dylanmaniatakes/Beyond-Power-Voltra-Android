package com.technogizguy.voltra.controller

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlin.math.max

data class PreparedStartupImage(
    val jpegBytes: ByteArray,
    val width: Int,
    val height: Int,
)

fun prepareStartupImage(
    context: Context,
    uri: Uri,
    targetSizePx: Int = 720,
): PreparedStartupImage {
    val sampled = decodeSampledBitmap(context.contentResolver, uri, max(targetSizePx * 2, 1440))
        ?: error("Could not decode the selected image.")
    val cropped = sampled.centerCropSquare()
    if (cropped !== sampled) {
        sampled.recycle()
    }
    val scaled = Bitmap.createScaledBitmap(cropped, targetSizePx, targetSizePx, true)
    if (scaled !== cropped) {
        cropped.recycle()
    }
    val flattened = scaled.flattenForJpeg()
    if (flattened !== scaled) {
        scaled.recycle()
    }

    var quality = STARTUP_IMAGE_INITIAL_JPEG_QUALITY
    var jpegBytes = flattened.toJpeg(quality)
    while (jpegBytes.size > STARTUP_IMAGE_TARGET_MAX_BYTES && quality > STARTUP_IMAGE_MIN_JPEG_QUALITY) {
        quality -= STARTUP_IMAGE_JPEG_QUALITY_STEP
        jpegBytes = flattened.toJpeg(quality)
    }

    flattened.recycle()
    return PreparedStartupImage(
        jpegBytes = jpegBytes,
        width = targetSizePx,
        height = targetSizePx,
    )
}

private fun decodeSampledBitmap(
    contentResolver: ContentResolver,
    uri: Uri,
    targetLargestDimension: Int,
): Bitmap? {
    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val options = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(
            width = bounds.outWidth,
            height = bounds.outHeight,
            targetLargestDimension = targetLargestDimension,
        )
    }
    return contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
    }
}

private fun calculateInSampleSize(
    width: Int,
    height: Int,
    targetLargestDimension: Int,
): Int {
    var sampleSize = 1
    var largest = max(width, height)
    while (largest / 2 >= targetLargestDimension) {
        sampleSize *= 2
        largest /= 2
    }
    return sampleSize.coerceAtLeast(1)
}

private fun Bitmap.centerCropSquare(): Bitmap {
    val edge = minOf(width, height)
    val x = (width - edge) / 2
    val y = (height - edge) / 2
    return Bitmap.createBitmap(this, x, y, edge, edge)
}

private fun Bitmap.flattenForJpeg(): Bitmap {
    val flattened = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    flattened.setHasAlpha(false)
    val canvas = Canvas(flattened)
    canvas.drawBitmap(this, 0f, 0f, null)
    return flattened
}

private fun Bitmap.toJpeg(quality: Int): ByteArray {
    val stream = ByteArrayOutputStream()
    check(compress(Bitmap.CompressFormat.JPEG, quality, stream)) {
        "Could not encode the selected image as JPEG."
    }
    return stream.toByteArray()
}

private const val STARTUP_IMAGE_INITIAL_JPEG_QUALITY = 92
private const val STARTUP_IMAGE_TARGET_MAX_BYTES = 39_000
private const val STARTUP_IMAGE_MIN_JPEG_QUALITY = 18
private const val STARTUP_IMAGE_JPEG_QUALITY_STEP = 4
