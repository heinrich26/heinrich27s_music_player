package com.example.musicplayer;

import android.app.Activity;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicplayer.data.musicDatabaseDao;

import java.util.ArrayList;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.musicViewHolder> {

	public final Context parentContext;
	public final ArrayList<Long> musicIds;
	private ItemClickListener clickListener;
	private final musicDatabaseDao songDao;
	private final MusicPlayerViewModel viewModel;
	private final int maxTextWidth;

	public byte[] getAlbumArt(String uri) {
		MediaMetadataRetriever retriever = new MediaMetadataRetriever();
		retriever.setDataSource(parentContext, Uri.parse(uri));
		byte[] art = retriever.getEmbeddedPicture();
		retriever.release();
		return art;
	}

	QueueAdapter(Context parentContext, ArrayList<Long> musicIds, musicDatabaseDao songDao) {
		this.musicIds = musicIds;
		this.parentContext = parentContext;
		this.songDao = songDao;

		viewModel = new ViewModelProvider((ViewModelStoreOwner) parentContext).get(MusicPlayerViewModel.class);

		maxTextWidth = ((Activity) parentContext).getWindowManager().getCurrentWindowMetrics().getBounds().height() - parentContext.getResources().getDimensionPixelSize(R.dimen.musicItemHeight);
	}

	@Override
	public long getItemId(int position) {
		return musicIds.get(position);
	}

	@NonNull
	@Override
	public musicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parentContext).inflate(R.layout.music_item, parent, false);
		return new musicViewHolder(view);
	}


	@Override
	public void onBindViewHolder(@NonNull musicViewHolder holder, int position) {
		final musicTrack track = viewModel.musicDict.get(getItemId(position));

		if (track == null) return;

		// put the Dot in the middle of the texts and add the values
		final String artist = track.getArtist();
		final String album = track.getAlbum();
		final String infoText = ((artist == null) ? parentContext.getString(R.string.unknown_artist) : artist) + " \u2022 " + ((album == null) ? parentContext.getString(R.string.unknown_album) : album);

		holder.updateText(track.getTitle(), infoText);


		// setup the Album Cover
		if (track.hasCover || ! track.testedForCover()) {
			// should load it on another Thread
			Handler coverHandler = new Handler(Looper.getMainLooper());
			coverHandler.post(() -> {
				byte[] albumArt = getAlbumArt(track.getPath());
				if (albumArt != null) {
					track.setHasCover(true);
					Glide.with(parentContext)
							.asBitmap()
							.load(albumArt)
							.placeholder(R.drawable.ic_note_twocolor)
							.into(holder.albumCover);
				} else {
					track.setHasCover(false);
					holder.albumCover.setBackgroundResource(R.drawable.ic_note_twocolor);
				}
			});
			songDao.updateTrack(track);
		} else {
			Glide.with(parentContext).clear(holder.albumCover);
			holder.albumCover.setBackgroundResource(R.drawable.ic_note_twocolor);
		}
	}

	@Override
	public int getItemCount() {
		return musicIds.size();
	}

	void setClickListener(ItemClickListener itemClickListener) {
		this.clickListener = itemClickListener;
	}

	public interface ItemClickListener {
		void onItemClick(View view, int position);
	}

	public class musicViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
		TextView songTitle, subtextInfo;
		ImageView albumCover;

		public musicViewHolder(@NonNull View itemView) {
			super(itemView);
			songTitle = itemView.findViewById(R.id.songTitle);
			subtextInfo = itemView.findViewById(R.id.subtextInfo);
			albumCover = itemView.findViewById(R.id.albumCover);
			albumCover.setClipToOutline(true);
			itemView.setOnClickListener(this);
		}

		@Override
		public void onClick(View view) {
			if (clickListener != null) clickListener.onItemClick(view, getAdapterPosition());
		}

		public void updateText(String title, String info) {
			songTitle.setText(title);
			subtextInfo.setText(info);


			final int titleWidth = getViewWidth(songTitle);
			final int infoWidth = getViewWidth(subtextInfo);

			if (maxTextWidth < titleWidth) {
				Animation mAnimation = new TranslateAnimation(
						0, -titleWidth,
						0, 0);
				mAnimation.setDuration(10000);    // Set custom duration.
				mAnimation.setStartOffset(1000);    // Set custom offset.
				mAnimation.setRepeatMode(Animation.RESTART);    // This will animate text back after it reaches end.
				mAnimation.setRepeatCount(Animation.INFINITE);    // Infinite animation.

				songTitle.startAnimation(mAnimation);
			}
			if (maxTextWidth < infoWidth) {
				Animation mAnimation = new TranslateAnimation(
						0, -infoWidth,
						0, 0);
				mAnimation.setDuration(10000);    // Set custom duration.
				mAnimation.setStartOffset(1000);    // Set custom offset.
				mAnimation.setRepeatMode(Animation.RESTART);    // This will animate text back after it reaches end.
				mAnimation.setRepeatCount(Animation.INFINITE);    // Infinite animation.

				subtextInfo.startAnimation(mAnimation);
			}
		}
	}

	private int getViewWidth(View view) {
		view.measure(0, 0);    // Need to set measure to (0, 0).
		return view.getMeasuredWidth();
	}
}
