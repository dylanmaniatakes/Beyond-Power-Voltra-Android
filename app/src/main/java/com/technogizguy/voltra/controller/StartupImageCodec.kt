package com.technogizguy.voltra.controller

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import kotlin.math.max

data class PreparedStartupImage(
    val jpegBytes: ByteArray,
    val width: Int,
    val height: Int,
)

data class StartupImageCropTransform(
    val zoom: Float = 1f,
    val offsetXFraction: Float = 0f,
    val offsetYFraction: Float = 0f,
)

fun loadStartupImageBitmap(
    context: Context,
    uri: Uri,
    targetLargestDimension: Int = 1440,
): Bitmap {
    return decodeStartupImageBitmap(context.contentResolver, uri, targetLargestDimension)
        ?: error("Could not decode the selected image.")
}

fun prepareStartupImage(
    context: Context,
    uri: Uri,
    targetSizePx: Int = 720,
): PreparedStartupImage {
    val sampled = loadStartupImageBitmap(context, uri, max(targetSizePx * 2, 1440))
    return prepareStartupImage(sampled, StartupImageCropTransform(), targetSizePx).also {
        sampled.recycle()
    }
}

fun prepareStartupImage(
    bitmap: Bitmap,
    cropTransform: StartupImageCropTransform,
    targetSizePx: Int = 720,
): PreparedStartupImage {
    val cropped = bitmap.cropToSquare(targetSizePx, cropTransform)
    val flattened = cropped.flattenForJpeg()
    if (flattened !== cropped) {
        cropped.recycle()
    }

    var quality = STARTUP_IMAGE_INITIAL_JPEG_QUALITY
    var jpegBytes = addIpadLikeStartupPhotoMetadata(flattened.toJpeg(quality), targetSizePx, targetSizePx)
    while (jpegBytes.size > STARTUP_IMAGE_TARGET_MAX_BYTES && quality > STARTUP_IMAGE_MIN_JPEG_QUALITY) {
        quality -= STARTUP_IMAGE_JPEG_QUALITY_STEP
        jpegBytes = addIpadLikeStartupPhotoMetadata(flattened.toJpeg(quality), targetSizePx, targetSizePx)
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
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
    }
}

private fun decodeStartupImageBitmap(
    contentResolver: ContentResolver,
    uri: Uri,
    targetLargestDimension: Int,
): Bitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        decodeWithImageDecoder(contentResolver, uri, targetLargestDimension)?.let { return it }
    }
    return decodeSampledBitmap(contentResolver, uri, targetLargestDimension)
}

private fun decodeWithImageDecoder(
    contentResolver: ContentResolver,
    uri: Uri,
    targetLargestDimension: Int,
): Bitmap? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
    return runCatching {
        val source = ImageDecoder.createSource(contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetSampleSize(
                calculateInSampleSize(
                    width = info.size.width,
                    height = info.size.height,
                    targetLargestDimension = targetLargestDimension,
                ),
            )
            decoder.setOnPartialImageListener { true }
        }
    }.getOrNull()
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

private fun Bitmap.cropToSquare(
    targetSizePx: Int,
    cropTransform: StartupImageCropTransform,
): Bitmap {
    val output = Bitmap.createBitmap(targetSizePx, targetSizePx, Bitmap.Config.ARGB_8888)
    output.setHasAlpha(false)
    val canvas = Canvas(output)
    canvas.drawARGB(0xFF, 0x00, 0x00, 0x00)

    val baseScale = max(
        targetSizePx.toFloat() / width.toFloat(),
        targetSizePx.toFloat() / height.toFloat(),
    )
    val appliedZoom = cropTransform.zoom.coerceIn(1f, 4f)
    val scaledWidth = width.toFloat() * baseScale * appliedZoom
    val scaledHeight = height.toFloat() * baseScale * appliedZoom
    val left = ((targetSizePx - scaledWidth) / 2f) + (cropTransform.offsetXFraction * targetSizePx)
    val top = ((targetSizePx - scaledHeight) / 2f) + (cropTransform.offsetYFraction * targetSizePx)
    val dest = RectF(left, top, left + scaledWidth, top + scaledHeight)
    canvas.drawBitmap(this, null, dest, null)
    return output
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

internal fun addIpadLikeStartupPhotoMetadata(
    jpegBytes: ByteArray,
    width: Int,
    height: Int,
): ByteArray {
    if (jpegBytes.size < 4 || jpegBytes[0] != 0xFF.toByte() || jpegBytes[1] != 0xD8.toByte()) {
        return jpegBytes
    }

    val insertOffset = findPostJfifOffset(jpegBytes)
    val app1 = buildIpadLikeExifSegment(width, height)
    val app13 = IPAD_LIKE_APP13_SEGMENT
    return jpegBytes.copyOfRange(0, insertOffset) + app1 + app13 + jpegBytes.copyOfRange(insertOffset, jpegBytes.size)
}

private fun findPostJfifOffset(jpegBytes: ByteArray): Int {
    if (jpegBytes.size < 6) return 2
    val marker = jpegBytes[3].toInt() and 0xFF
    if (jpegBytes[2] != 0xFF.toByte() || marker != 0xE0) {
        return 2
    }
    val length = ((jpegBytes[4].toInt() and 0xFF) shl 8) or (jpegBytes[5].toInt() and 0xFF)
    val nextOffset = 2 + 2 + length
    return nextOffset.coerceIn(2, jpegBytes.size)
}

private fun buildIpadLikeExifSegment(width: Int, height: Int): ByteArray {
    val payload = ByteArrayOutputStream().apply {
        write(byteArrayOf(0x45, 0x78, 0x69, 0x66, 0x00, 0x00))
        write(byteArrayOf(0x4D, 0x4D, 0x00, 0x2A))
        write(byteArrayOf(0x00, 0x00, 0x00, 0x08))
        write(byteArrayOf(0x00, 0x01))
        write(byteArrayOf(0x87.toByte(), 0x69))
        write(byteArrayOf(0x00, 0x04))
        write(byteArrayOf(0x00, 0x00, 0x00, 0x01))
        write(byteArrayOf(0x00, 0x00, 0x00, 0x1A))
        write(byteArrayOf(0x00, 0x00, 0x00, 0x00))
        write(byteArrayOf(0x00, 0x03))
        write(byteArrayOf(0xA0.toByte(), 0x01, 0x00, 0x03))
        write(byteArrayOf(0x00, 0x00, 0x00, 0x01))
        write(byteArrayOf(0x00, 0x01, 0x00, 0x00))
        write(byteArrayOf(0xA0.toByte(), 0x02, 0x00, 0x04))
        write(byteArrayOf(0x00, 0x00, 0x00, 0x01))
        write(intToBe32(width))
        write(byteArrayOf(0xA0.toByte(), 0x03, 0x00, 0x04))
        write(byteArrayOf(0x00, 0x00, 0x00, 0x01))
        write(intToBe32(height))
        write(byteArrayOf(0x00, 0x00, 0x00, 0x00))
    }.toByteArray()

    val length = payload.size + 2
    return byteArrayOf(
        0xFF.toByte(),
        0xE1.toByte(),
        ((length shr 8) and 0xFF).toByte(),
        (length and 0xFF).toByte(),
    ) + payload
}

private fun intToBe32(value: Int): ByteArray {
    return byteArrayOf(
        ((value shr 24) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        (value and 0xFF).toByte(),
    )
}

private const val STARTUP_IMAGE_INITIAL_JPEG_QUALITY = 92
// The clean iPad capture contains two official startup-photo uploads:
// one around 54 KB and a later explicitly cropped branch around 155 KB.
// We stay much closer to the smaller official path here so Android can test a
// more iPad-like export without jumping straight back to the huge transfers
// that previously stalled on-device.
private const val STARTUP_IMAGE_TARGET_MAX_BYTES = 56_000
private const val STARTUP_IMAGE_MIN_JPEG_QUALITY = 18
private const val STARTUP_IMAGE_JPEG_QUALITY_STEP = 4

private val IPAD_LIKE_APP13_SEGMENT = byteArrayOf(
    0xFF.toByte(), 0xED.toByte(), 0x00, 0x38,
    0x50, 0x68, 0x6F, 0x74, 0x6F, 0x73, 0x68, 0x6F, 0x70, 0x20, 0x33, 0x2E, 0x30, 0x00,
    0x38, 0x42, 0x49, 0x4D, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x38, 0x42, 0x49, 0x4D, 0x04, 0x25, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10,
    0xD4.toByte(), 0x1D, 0x8C.toByte(), 0xD9.toByte(), 0x8F.toByte(), 0x00, 0xB2.toByte(), 0x04,
    0xE9.toByte(), 0x80.toByte(), 0x09, 0x98.toByte(), 0xEC.toByte(), 0xF8.toByte(), 0x42, 0x7E,
)
