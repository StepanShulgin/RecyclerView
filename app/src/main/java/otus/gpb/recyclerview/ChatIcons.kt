package otus.gpb.recyclerview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.RawRes
import com.caverock.androidsvg.SVG
import kotlin.math.ceil

internal class ChatIcons(private val context: Context) {
    private val bitmaps = mutableMapOf<Int, Bitmap>()

    fun drawable(@RawRes resource: Int): BitmapDrawable {
        val bitmap =
            bitmaps.getOrPut(resource) {
                val svg = SVG.getFromResource(context, resource)
                val density = context.resources.displayMetrics.density
                Bitmap.createBitmap(
                        ceil(svg.documentWidth * density).toInt(),
                        ceil(svg.documentHeight * density).toInt(),
                        Bitmap.Config.ARGB_8888,
                    )
                    .also { image ->
                        image.density = context.resources.displayMetrics.densityDpi
                        svg.renderToCanvas(Canvas(image).apply { scale(density, density) })
                    }
            }
        return BitmapDrawable(context.resources, bitmap)
    }
}
