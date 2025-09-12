package com.example.worldradio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class ItemMoveCallback extends ItemTouchHelper.Callback {

    private final ItemTouchHelperContract adapter;
    private final Paint paint;

    ItemMoveCallback(ItemTouchHelperContract _adapter) {
        adapter = _adapter;
        paint = new Paint();
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return adapter.isDragEnabled();
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return adapter.isSwipeEnabled();
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        adapter.onSwipe(viewHolder, i);
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {
        adapter.onRowMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG)
            adapter.onRowSelected(viewHolder);
        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        adapter.onRowClear(viewHolder);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX != 0) {
            Drawable d = ContextCompat.getDrawable(adapter.getContext(),
                    dX > 0 ? R.drawable.baseline_delete_forever_red_24
                            : adapter.isPlaylistOpen() ? R.drawable.baseline_playlist_add_purple_24
                            : R.drawable.baseline_share_24);
            if (d != null) {
                View itemView = viewHolder.itemView;
                int iconWidth = d.getIntrinsicWidth();
                int iconHeight = d.getIntrinsicHeight();
                int cellHeight = itemView.getBottom() - itemView.getTop();
                int iconTop = itemView.getTop() + (cellHeight - iconHeight) / 2;
                int iconBottom = iconTop + iconHeight;
                int margin = (int) ((Math.abs(dX) - iconWidth) / 2);
                int iconLeft = dX > 0 ? itemView.getLeft() + margin : itemView.getRight() - margin - iconWidth;
                int iconRight = dX > 0 ? itemView.getLeft() + margin + iconWidth : itemView.getRight() - margin;
                int alpha = (int)((Math.abs(dX) / (itemView.getRight() - itemView.getLeft())) * 140);
                int cX = (iconLeft + iconRight) / 2;
                int cY = (iconTop + iconBottom) / 2;
                d.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                paint.setARGB(alpha, dX > 0 ? 255 : 0, 0, dX > 0 ? 0 : 255);
                c.drawRect(dX > 0 ? itemView.getLeft() : cX, cY - iconHeight, dX > 0 ? cX : itemView.getRight(), cY + iconHeight, paint);
                c.drawArc(cX - iconHeight, cY - iconHeight, cX + iconHeight, cY + iconHeight, dX > 0 ? 270 : 90, 180, true, paint);
                d.draw(c);
            }
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    interface ItemTouchHelperContract {
        boolean isPlaylistOpen();
        boolean isDragEnabled();
        boolean isSwipeEnabled();
        void onRowMoved(int fromPosition, int toPosition);
        void onRowSelected(RecyclerView.ViewHolder myViewHolder);
        void onRowClear(RecyclerView.ViewHolder myViewHolder);
        void onSwipe(RecyclerView.ViewHolder myViewHolder, int i);
        Context getContext();
    }
}