package com.example.musicplayer;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicplayer.data.musicDatabaseDao;

import java.util.ArrayList;

public class AlbumArtAdapter extends RecyclerView.Adapter<AlbumArtAdapter.musicViewHolder> {

	public final Context parentContext;
	public final ArrayList<Long> musicIds;
	private final musicDatabaseDao songDao;
	private final MusicPlayerViewModel viewModel;


	// old method to extract art from MP3
	public byte[] getAlbumArt(String uri) {
		MediaMetadataRetriever retriever = new MediaMetadataRetriever();
		retriever.setDataSource(parentContext, Uri.parse(uri));
		byte[] art = retriever.getEmbeddedPicture();
		retriever.release();
		return art;
	}

	AlbumArtAdapter(Context parentContext, ArrayList<Long> musicIds, musicDatabaseDao songDao) {
		this.musicIds = musicIds;
		this.parentContext = parentContext;
		this.songDao = songDao;

		viewModel = new ViewModelProvider((ViewModelStoreOwner) parentContext).get(MusicPlayerViewModel.class);
	}

	@NonNull
	@Override
	public musicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parentContext).inflate(R.layout.album_art_big, parent, false);
		view.setClipToOutline(false);

		return new musicViewHolder(view);
	}

	@Override
	public long getItemId(int position) {
		return musicIds.get(position);
	}

	@Override
	public void onBindViewHolder(@NonNull musicViewHolder holder, int position) {

		((ViewGroup) holder.itemView).setClipChildren(false);

		final musicTrack track = viewModel.musicDict.get(getItemId(position));

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

	public static class musicViewHolder extends RecyclerView.ViewHolder  {
		ImageView albumCover;

		public musicViewHolder(@NonNull View itemView) {
			super(itemView);
			albumCover = itemView.findViewById(R.id.albumCoverView);
			albumCover.setClipToOutline(true);
		}
	}
}
