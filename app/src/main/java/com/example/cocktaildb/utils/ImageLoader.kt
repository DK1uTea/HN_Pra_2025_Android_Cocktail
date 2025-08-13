package com.example.cocktaildb.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.widget.ImageView
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Simple utility class for loading images from URLs
 * without using external libraries
 */
object ImageLoader {

    /**
     * Loads an image from a URL into an ImageView
     * @param url The URL of the image to load
     * @param imageView The ImageView to load the image into
     * @param placeholderResId The resource ID of a placeholder image to show while loading
     */
    fun loadImage(url: String?, imageView: ImageView, placeholderResId: Int) {
        if (url.isNullOrEmpty()) {
            imageView.setImageResource(placeholderResId)
            return
        }

        // Set placeholder while loading
        imageView.setImageResource(placeholderResId)

        // Use AsyncTask to load the image in the background
        ImageDownloadTask(imageView, placeholderResId).execute(url)
    }

    /**
     * AsyncTask for downloading images
     */
    private class ImageDownloadTask(
        private val imageView: ImageView,
        private val errorResId: Int
    ) : AsyncTask<String, Void, Bitmap?>() {

        override fun doInBackground(vararg params: String): Bitmap? {
            val imageUrl = params[0]
            var bitmap: Bitmap? = null
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input: InputStream = connection.inputStream
                bitmap = BitmapFactory.decodeStream(input)
                input.close()
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return bitmap
        }

        override fun onPostExecute(result: Bitmap?) {
            if (result != null) {
                imageView.setImageBitmap(result)
            } else {
                imageView.setImageResource(errorResId)
            }
        }
    }
}
