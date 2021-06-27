package com.example.musicplayer;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicplayer.data.musicDatabaseDao;

import java.util.ArrayList;
import java.util.List;

import static com.example.musicplayer.MainActivity.formatMillis;

public class MusicAdapterFixedHeight extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
	private static final int TYPE_HEADER = 0;
	private static final int TYPE_ITEM = 1;
	private static final int HEADER_POSITION = 0;


	public final Context parentContext;
	public final ArrayList<musicTrack> musicFiles;
	private final musicDatabaseDao songDao;
	private SongMenuBottomSheetFragment.SongMenuActions songMenuActions;
	private String playlistTitle, playlistDesc;
	private View.OnClickListener itemClickListener;
	private List<Integer> selection = new ArrayList<>();
	private boolean selectionMode = false;

	MusicAdapterFixedHeight(Context parentContext, ArrayList<musicTrack> musicFiles, musicDatabaseDao songDao, String playlistTitle, String playlistDesc) {
		this.musicFiles = musicFiles;
		this.parentContext = parentContext;
		this.songDao = songDao;
		this.playlistTitle = playlistTitle;
		this.playlistDesc = playlistDesc;
		setHasStableIds(true);
	}

	public void removeItem(int position) {
		musicFiles.remove(position);
		notifyItemRemoved(position + 1);
	}

	public void restoreItem(musicTrack item, int position) {
		musicFiles.add(position, item);
		notifyItemInserted(position + 1);
	}

	public byte[] getAlbumArt(String uri) {
		MediaMetadataRetriever retriever = new MediaMetadataRetriever();
		retriever.setDataSource(parentContext, Uri.parse(uri));
		byte[] art = retriever.getEmbeddedPicture();
		retriever.release();
		return art;
	}


	@Override
	public int getItemViewType(int position) {
		if (position == HEADER_POSITION) return TYPE_HEADER;

		return TYPE_ITEM;
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view;
		if (viewType == TYPE_HEADER) {
			view = LayoutInflater.from(parentContext).inflate(R.layout.playlist_header, parent, false);
			return new playlistHeader(view);
		} else {
			view = LayoutInflater.from(parentContext).inflate(R.layout.music_item_fixed_size, parent, false);
			return new musicViewHolder(view);
		}
	}


	// selection based stuff
	private boolean itemIsSelected(int position) { return selection.contains(position); }

	private void toggleSelectedState(int position) {
		if (itemIsSelected(position)) {
			selection.remove(position);
		} else {
			selection.add(position);
		}
		notifyItemChanged(position);
	}

	public void clearSelection() {
		for (Integer i : selection) {
			selection.remove(i);
			notifyItemChanged(i);
		}
	}

	public int getSelectedItemCount() { return selection.size(); }

	public List<Integer> getSelectedItems() { return selection; }

	public void enterSelectMode() {
		selectionMode = true;
	}

	public void leaveSelectMode() {
		selectionMode = false;
	}



	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		if (holder instanceof musicViewHolder) {
			musicViewHolder musicHolder = (musicViewHolder) holder;
			musicHolder.itemView.setSelected(itemIsSelected(position));

			final musicTrack track = musicFiles.get(position - 1);

			musicHolder.songTitle.setText(track.getTitle());
			// put the Dot in the middle of the texts and add the values
			String artist = track.getArtist();
			String album = track.getAlbum();
			final String infoText = ((artist == null) ? "" : artist + " \u2022 ") + formatMillis(track.getDuration());
			musicHolder.subtextInfo.setText(infoText);


			final byte[][] albumArt = {null};
			if (track.hasCover || !track.testedForCover()) {
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
								.into(musicHolder.albumCover);
					} else {
						track.setHasCover(false);
						musicHolder.albumCover.setBackgroundResource(R.drawable.ic_note_twocolor);
					}
				});
				songDao.updateTrack(track);
			} else {
				Glide.with(parentContext).clear(musicHolder.albumCover);
				musicHolder.albumCover.setBackgroundResource(R.drawable.ic_note_twocolor);
			}
			holder.itemView.setOnLongClickListener(view -> {
				showSongContextMenu(albumArt[0], track.getTitle(), track.getArtist(), position - 1);
				return true;
			});
		} else {
			playlistHeader headerHolder = (playlistHeader) holder;

			headerHolder.playlistTitle.setText(playlistTitle);
			headerHolder.playlistDescription.setText(playlistDesc);
		}
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
		return musicFiles.size() + 1;
	}

	public void setSongMenuActions(SongMenuBottomSheetFragment.SongMenuActions songMenuActions) {
		this.songMenuActions = songMenuActions;
	}

	public void setItemClickListener(View.OnClickListener listener) {
		this.itemClickListener = listener;
	}

	public class musicViewHolder extends RecyclerView.ViewHolder {
		TextView songTitle, subtextInfo;
		ImageView albumCover;
		byte[] albumArt;

		public musicViewHolder(@NonNull View itemView) {
			super(itemView);
			songTitle = itemView.findViewById(R.id.songTitle);
			subtextInfo = itemView.findViewById(R.id.subtextInfo);
			albumCover = itemView.findViewById(R.id.albumCover);
			albumCover.setClipToOutline(true);
			itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (!selectionMode) {
						itemClickListener.onClick(v);
					} else {
						toggleSelectedState(getAdapterPosition());
					}
				}
			});
		}
	}

	public static class playlistHeader extends RecyclerView.ViewHolder {
		EditText playlistTitle, playlistDescription;
		ImageView playlistCover;
		TextView addSongsTV;
		LinearLayout addSongsHolder, playlistControls;
		byte[] playlistArt;

		public playlistHeader(@NonNull View itemView) {
			super(itemView);
			playlistTitle = itemView.findViewById(R.id.playlistNameEditText);
			playlistDescription = itemView.findViewById(R.id.playlistDescriptionEditText);
			playlistCover = itemView.findViewById(R.id.playlistCover);
			addSongsTV = itemView.findViewById(R.id.addSongsTV);
			addSongsHolder = itemView.findViewById(R.id.addSongsHolder);
			playlistControls = itemView.findViewById(R.id.playlistControls);
			playlistTitle.setEnabled(false);
			playlistTitle.setSaveEnabled(true);
			playlistDescription.setEnabled(false);
			playlistDescription.setSaveEnabled(false);
			playlistCover.setClipToOutline(true);
			itemView.setFocusable(View.NOT_FOCUSABLE);
		}
	}
}
