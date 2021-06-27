package com.example.musicplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicplayer.data.musicDatabaseDao;

import java.util.ArrayList;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.musicViewHolder> {

	public final Context parentContext;
	public final ArrayList<musicTrack> musicFiles;
	private ItemClickListener clickListener;
	private final musicDatabaseDao songDao;

	public byte[] getAlbumArt(String uri) {
		MediaMetadataRetriever retriever = new MediaMetadataRetriever();
		retriever.setDataSource(parentContext, Uri.parse(uri));
		byte[] art = retriever.getEmbeddedPicture();
		retriever.release();
		return art;
	}

	MusicAdapter(Context parentContext, ArrayList<musicTrack> musicFiles, musicDatabaseDao songDao) {
		this.musicFiles = musicFiles;
		this.parentContext = parentContext;
		this.songDao = songDao;
	}

	@NonNull
	@Override
	public musicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parentContext).inflate(R.layout.music_item, parent, false);
		return new musicViewHolder(view);
	}

	@SuppressLint("SetTextI18n")
	@Override
	public void onBindViewHolder(@NonNull musicViewHolder holder, int position) {

		final musicTrack track = musicFiles.get(position);

		holder.songTitle.setText(track.getTitle());
		// put the Dot in the middle of the texts and add the values
		String artist = track.getArtist();
		String album = track.getAlbum();
		final String infoText = ((artist == null) ? parentContext.getString(R.string.unknown_artist) : artist) + " \u2022 " + ((album == null) ? parentContext.getString(R.string.unknown_album) : album);
		holder.subtextInfo.setText(infoText);
		animateTextView(holder.subtextInfo);



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
		return musicFiles.size();
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
	}

	private void animateTextView(TextView mTextView) {
		int textWidth = getTextViewWidth(mTextView);
		int displayWidth = getDisplayWidth(parentContext);

		/* Start animation only when text is longer than display width. */
		if(displayWidth<=textWidth) {
			Animation mAnimation = new TranslateAnimation(
					0, -textWidth,
					0, 0);
			mAnimation.setDuration(10000);    // Set custom duration.
			mAnimation.setStartOffset(1000);    // Set custom offset.
			mAnimation.setRepeatMode(Animation.RESTART);    // This will animate text back after it reaches end.
			mAnimation.setRepeatCount(Animation.INFINITE);    // Infinite animation.

			mTextView.startAnimation(mAnimation);
		}
	}

	private int getDisplayWidth(Context context) {
		int displayWidth;

		WindowManager windowManager = (WindowManager)context.getSystemService(
				Context.WINDOW_SERVICE);
		Display display = windowManager.getDefaultDisplay();
		Point screenSize = new Point();

		display.getSize(screenSize);
		displayWidth = screenSize.x;

		return displayWidth;
	}

	private int getTextViewWidth(TextView textView) {
		textView.measure(0, 0);    // Need to set measure to (0, 0).
		return textView.getMeasuredWidth();
	}
}
