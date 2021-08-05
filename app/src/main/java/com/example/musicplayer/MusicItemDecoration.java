package com.example.musicplayer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

public class MusicItemDecoration extends DividerItemDecoration {
	private Drawable mDivider;

	private final Rect mBounds = new Rect();

	public MusicItemDecoration(Context context, Drawable mDivider) {
		super(context, RecyclerView.VERTICAL);
		super.setDrawable(mDivider);
		this.mDivider = mDivider;
	}

	@Override
	public void setDrawable(@NonNull Drawable drawable) {
		super.setDrawable(drawable);
		this.mDivider = drawable;
	}

	@Nullable
	@Override
	public Drawable getDrawable() {
		return mDivider;
	}

	@Override
	public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
		canvas.save();
		final int left;
		final int right;
		//noinspection AndroidLintNewApi - NewApi lint fails to handle overrides.
		if (parent.getClipToPadding()) {
			left = parent.getPaddingLeft();
			right = parent.getWidth() - parent.getPaddingRight();
			canvas.clipRect(left, parent.getPaddingTop(), right,
					parent.getHeight() - parent.getPaddingBottom());
		} else {
			left = 0;
			right = parent.getWidth();
		}

		final int childCount = parent.getChildCount();
		for (int i = 0; i < childCount - 1; i++) {
			final View child = parent.getChildAt(i);
			// attempt to hide lines when Dragging
//			if (child.isActivated() || (i < childCount - 1 && parent.getChildAt(i + 1).isActivated())) continue;

			parent.getDecoratedBoundsWithMargins(child, mBounds);
			final int bottom = mBounds.bottom + Math.round(child.getTranslationY());
			final int top = bottom - mDivider.getIntrinsicHeight();
			mDivider.setBounds(left, top, right, bottom);
			mDivider.draw(canvas);
		}
		canvas.restore();
	}

	@Override
	public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
		super.getItemOffsets(outRect, view, parent, state);
	}
}
