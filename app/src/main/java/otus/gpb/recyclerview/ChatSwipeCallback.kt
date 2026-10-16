package otus.gpb.recyclerview

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class ChatSwipeCallback(recyclerView: RecyclerView, private val onDismiss: (Long) -> Unit) :
    ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
    private val context = recyclerView.context
    private val backgroundPaint =
        Paint().apply { color = ContextCompat.getColor(context, R.color.swipe_background) }
    private val labelPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.swipe_foreground)
            textSize = context.resources.getDimension(R.dimen.swipe_label_size)
            textAlign = Paint.Align.CENTER
            typeface =
                android.graphics.Typeface.create(
                    "sans-serif-medium",
                    android.graphics.Typeface.NORMAL,
                )
        }
    private val icon = ChatIcons(context).drawable(R.raw.chat_archive)
    private val cornerRadius = context.resources.getDimension(R.dimen.swipe_corner_radius)
    private val iconSize = context.resources.getDimensionPixelSize(R.dimen.swipe_icon_size)
    private val actionWidth = context.resources.getDimensionPixelSize(R.dimen.swipe_action_width)
    private val labelGap = context.resources.getDimension(R.dimen.swipe_label_gap)
    private val label = context.getString(R.string.delete_chat)

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder,
    ): Boolean = false

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = 0.7f

    override fun getSwipeEscapeVelocity(defaultValue: Float): Float = Float.MAX_VALUE

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        (viewHolder as ChatAdapter.ChatViewHolder).chatId?.let(onDismiss)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        (viewHolder.itemView.background as GradientDrawable).cornerRadius = 0f
        viewHolder.itemView.clipToOutline = false
    }

    override fun onChildDraw(
        canvas: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean,
    ) {
        if (dX < 0 && actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            val item = viewHolder.itemView
            (item.background.mutate() as GradientDrawable).cornerRadius = cornerRadius
            item.clipToOutline = true
            val checkpoint = canvas.save()
            canvas.clipRect(
                item.left.toFloat(),
                item.top.toFloat(),
                item.right.toFloat(),
                item.bottom.toFloat(),
            )
            canvas.drawRect(
                item.left.toFloat(),
                item.top.toFloat(),
                item.right.toFloat(),
                item.bottom.toFloat(),
                backgroundPaint,
            )
            val centerX = item.right - actionWidth / 2f
            val labelHeight = labelPaint.fontMetrics.run { descent - ascent }
            val top = (item.top + (item.height - iconSize - labelGap - labelHeight) / 2).toInt()
            val left = (centerX - iconSize / 2).toInt()
            icon.setBounds(left, top, left + iconSize, top + iconSize)
            icon.draw(canvas)
            val baseline = top + iconSize + labelGap - labelPaint.fontMetrics.ascent
            canvas.drawText(label, centerX, baseline, labelPaint)
            canvas.restoreToCount(checkpoint)
        }
        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
