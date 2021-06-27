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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicplayer.data.musicDatabaseDao;

import java.util.ArrayList;

import static com.example.musicplayer.MainActivity.formatMillis;

public class MusicAdapterFixedHeight extends RecyclerView.Adapter<MusicAdapterFixedHeight.musicViewHolder> {

	public final Context parentContext;
	public final ArrayList<musicTrack> musicFiles;
	private final musicDatabaseDao songDao;
	private SongMenuBottomSheetFragment.SongMenuActions songMenuActions;

	MusicAdapterFixedHeight(Context parentContext, ArrayList<musicTrack> musicFiles, musicDatabaseDao songDao) {
		this.musicFiles = musicFiles;
		this.parentContext = parentContext;
		this.songDao = songDao;
	}

	public void removeItem(int position) {
		musicFiles.remove(position);
		notifyItemRemoved(position);
	}

	public void restoreItem(musicTrack item, int position) {
		musicFiles.add(position, item);
		notifyItemInserted(position);
		songDao.addTrack(item);
	}

	public byte[] getAlbumArt(String uri) {
		MediaMetadataRetriever retriever = new MediaMetadataRetriever();
		retriever.setDataSource(parentContext, Uri.parse(uri));
		byte[] art = retriever.getEmbeddedPicture();
		retriever.release();
		return art;
	}

	@NonNull
	@Override
	public musicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view;
		view = LayoutInflater.from(parentContext).inflate(R.layout.music_item_fixed_size, parent, false);
		return new musicViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull musicViewHolder holder, int position) {

		final musicTrack track = musicFiles.get(position);

		holder.songTitle.setText(track.getTitle());
		// put the Dot in the middle of the texts and add the values
		String artist = track.getArtist();
		String album = track.getAlbum();
		final String infoText = ((artist == null) ? "" : artist + " \u2022 ") + formatMillis(track.getDuration());
		holder.subtextInfo.setText(infoText);


		final byte[][] albumArt = {null};
		if (track.hasCover || ! track.testedForCover()) {
			// should load it on another Thread
			Handler coverHandler = new Handler(Looper.getMainLooper());
			coverHandler.post(() -> {
				albumArt[0] = MusicAdapterFixedHeight.this.getAlbumArt(track.getPath());
				if (albumArt[0] != null) {
					track.setHasCover(true);
					Glide.with(parentContext)
							.asBitmap()
							.load(albumArt[0])
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
		holder.itemView.setOnLongClickListener(view -> {
			showSongContextMenu(albumArt[0], track.getTitle(), track.getArtist(), position);
			return true;
		});
	}


	public void showSongContextMenu(byte[] albumArt, String title, String artist, int position) {
		SongMenuBottomSheetFragment songMenuFragment = new SongMenuBottomSheetFragment(albumArt, title, artist, position, songMenuActions);

		try {
			final FragmentManager fragmentManager = ((FragmentActivity) parentContext).getSupportFragmentManager();
			songMenuFragment.show(fragmentManager, songMenuFragment.getTag());
		} catch (ClassCastException ignored) {}
	}

	@Override
	public int getItemCount() {
		return musicFiles.size();
	}

	public void setSongMenuActions(SongMenuBottomSheetFragment.SongMenuActions songMenuActions) {
		this.songMenuActions = songMenuActions;
	}

	public static class musicViewHolder extends RecyclerView.ViewHolder {
		TextView songTitle, subtextInfo;
		ImageView albumCover;
		byte[] albumArt;

		public musicViewHolder(@NonNull View itemView) {
			super(itemView);
			songTitle = itemView.findViewById(R.id.songTitle);
			subtextInfo = itemView.findViewById(R.id.subtextInfo);
			albumCover = itemView.findViewById(R.id.albumCover);
			albumCover.setClipToOutline(true);
		}
	}
}
