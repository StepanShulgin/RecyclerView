package otus.gpb.recyclerview

import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView

class ChatDividerDecoration(
    @ColorInt color: Int,
    private val thickness: Float,
    private val textInset: Int,
) : RecyclerView.ItemDecoration() {
    private val paint = Paint().apply { this.color = color }

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            val position = parent.getChildAdapterPosition(child)
            if (position == RecyclerView.NO_POSITION || position == state.itemCount - 1) continue
            val left =
                if (parent.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                    parent.paddingLeft.toFloat()
                } else {
                    (parent.paddingLeft + textInset).toFloat()
                }
            val right =
                if (parent.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                    (parent.width - parent.paddingRight - textInset).toFloat()
                } else {
                    (parent.width - parent.paddingRight).toFloat()
                }
            val bottom = child.bottom + child.translationY
            canvas.drawRect(
                left + child.translationX,
                bottom - thickness,
                right + child.translationX,
                bottom,
                paint,
            )
        }
    }
}
