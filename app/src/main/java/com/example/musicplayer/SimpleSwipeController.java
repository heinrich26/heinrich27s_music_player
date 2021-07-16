package com.example.musicplayer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public abstract class SimpleSwipeController extends ItemTouchHelper.Callback {

	Context mContext;
	private final Paint mClearPaint = new Paint();
	private final ColorDrawable deleteBackground = new ColorDrawable(), addQueueBackground = new ColorDrawable();
	private final Drawable deleteDrawable, addQueueDrawable;
	private final int iconMargin, decreasedMargin, itemHeight;
	private final float dragIncrement;

	public void setSwipeEnabled(boolean enabled) {
		this.swipeEnabled = enabled;
	}

	private boolean swipeEnabled;

	SimpleSwipeController(Context context) {
		mContext = context;
		deleteBackground.setColor(mContext.getColor(R.color.deleteRed));
		deleteDrawable = ContextCompat.getDrawable(mContext, R.drawable.ic_delete);
		assert deleteDrawable != null;
		deleteDrawable.setTint(Color.WHITE);

		addQueueBackground.setColor(mContext.getColor(R.color.addToQueueBlue));
		addQueueDrawable = ContextCompat.getDrawable(mContext, R.drawable.ic_queue_add);
		assert addQueueDrawable != null;
		addQueueDrawable.setTint(Color.WHITE);

		mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		iconMargin = context.getResources().getDimensionPixelSize(R.dimen.musicItemDrawableMargin);
		decreasedMargin = 2 * iconMargin / 3;

		DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();

		dragIncrement = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, metrics);

		itemHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72f, metrics);
	}


	@Override
	public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
		if (!(viewHolder instanceof MusicAdapterFixedHeight.musicViewHolder)) return makeMovementFlags(0,0);
		return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
	}

	@Override
	public float getMoveThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
		return super.getMoveThreshold(viewHolder);
	}

	@Override
	public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
		super.onSelectedChanged(viewHolder, actionState);

		if (viewHolder != null && actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
			viewHolder.itemView.setElevation(dragIncrement);
			final CardView card = ((MusicAdapterFixedHeight.musicViewHolder) viewHolder).trackHolder;
			card.setPressed(true);
			card.setCardElevation(dragIncrement);
			card.setRadius(dragIncrement);
		}
	}

	@Override
	public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
		super.clearView(recyclerView, viewHolder);

		viewHolder.itemView.setElevation(0f);
		final CardView card = ((MusicAdapterFixedHeight.musicViewHolder) viewHolder).trackHolder;
		card.setPressed(false);
		card.setCardElevation(0f);
		card.setRadius(0f);
	}

	@Override
	public boolean isLongPressDragEnabled() {
		return false;
	}

	@Override
	public boolean isItemViewSwipeEnabled() {
		return swipeEnabled;
	}



	@Override
	public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
							@NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
							int actionState, boolean isCurrentlyActive) {
		if (!(viewHolder instanceof MusicAdapterFixedHeight.musicViewHolder)) return;

		View itemView = viewHolder.itemView;
		final int itemTop = itemView.getBottom() - itemHeight;


		if (dX < 0) {
			deleteBackground.setBounds(itemView.getRight() + (int) dX, itemTop, itemView.getRight(), itemView.getBottom());
			deleteBackground.draw(c);

			final int iconHeight = itemHeight - 2 * iconMargin;
			final int iconTop = itemTop + iconMargin;
			final int iconRight = itemView.getRight() - (int) (0.75 * iconMargin);
			final int iconLeft = iconRight - iconHeight;
			final int iconBottom = iconTop + iconHeight;


			deleteDrawable.setBounds(iconLeft, iconTop, iconRight, iconBottom);
			deleteDrawable.draw(c);
		} else {
			addQueueBackground.setBounds(itemView.getLeft(), itemTop, itemView.getLeft() + (int) dX, itemView.getBottom());
			addQueueBackground.draw(c);

			final int iconLeft = itemView.getLeft() + (int) (0.75f * decreasedMargin);
			if (dX > iconLeft) {
				final int iconHeight = itemHeight - 2 * decreasedMargin;
				final int iconTop = itemTop + decreasedMargin;
				final int iconRight = iconLeft + iconHeight;
				final int iconBottom = iconTop + iconHeight;


				addQueueDrawable.setBounds(iconLeft, iconTop, iconRight, iconBottom);
				addQueueDrawable.draw(c);
			}
		}

		super.onChildDraw(c, recyclerView, viewHolder, 0, dY, actionState, isCurrentlyActive);

		((MusicAdapterFixedHeight.musicViewHolder) viewHolder).trackHolder.setTranslationX(dX);
	}

	@Override
	public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
		return 0.7f;
	}

	@Override
	public float getSwipeEscapeVelocity(float defaultValue) {
		return 1.5f * defaultValue;
	}

	@Override
	public float getSwipeVelocityThreshold(float defaultValue) {
		return 0.75f * defaultValue;
	}

	public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
}
