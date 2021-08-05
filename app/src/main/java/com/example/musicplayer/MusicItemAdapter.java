package com.example.musicplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicplayer.data.musicDatabaseDao;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.example.musicplayer.MainActivity.formatMillis;

public class MusicItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
	private static final int TYPE_HEADER = 0;
	private static final int TYPE_ITEM = 1;
	private static final int HEADER_POSITION = 0;


	public final Context parentContext;
	private final MusicPlayerViewModel viewModel;
	private final List<musicTrack> musicFiles;
	private final musicDatabaseDao songDao;
	private BottomSheetMenu.bottomSheetMenuAction[] songMenuActions;
	private String playlistTitle, playlistDesc;

	public void setPlaylistTitle(String title) {
		playlistTitle = title;
	}

	public void setPlaylistDesc(String description) {
		playlistDesc = description;
	}

	private boolean selectionMode;
	private boolean editMode;
	private final boolean isLibrary;
	private PlaylistControls playlistControls;

	public void setPlaylistControls(PlaylistControls playlistControls) {
		this.playlistControls = playlistControls;
	}

	public void setTouchHelper(ItemTouchHelper touchHelper, SimpleSwipeController simpleTouchHelper) {
		this.touchHelper = touchHelper;
		this.simpleTouchHelper = simpleTouchHelper;

		this.simpleTouchHelper.setSwipeEnabled(!editMode || !selectionMode);
	}

	private ItemTouchHelper touchHelper;
	private SimpleSwipeController simpleTouchHelper;

	private final float dragElevation, dragRadius;

	MusicItemAdapter(Context parentContext, List<musicTrack> musicFiles, musicDatabaseDao songDao, boolean isLibrary) {
		this.musicFiles = musicFiles;
		this.parentContext = parentContext;
		this.songDao = songDao;
		this.isLibrary = isLibrary;
		setHasStableIds(true);

		viewModel = new ViewModelProvider((parentContext instanceof MainActivity) ? (MainActivity) parentContext : (AddTitlesActivity) parentContext).get(MusicPlayerViewModel.class);

		Resources res = parentContext.getResources();
		dragElevation = res.getDimensionPixelSize(R.dimen.music_item_drag_elevation);
		dragRadius = res.getDimensionPixelSize(R.dimen.music_item_drag_radius);
	}

	public void removeItem(int position) {
		musicFiles.remove(position - 1);
		notifyItemRemoved(position);
		if (isLibrary && musicFiles.size() > position) notifyItemChanged(position);
	}

	public void restoreItem(musicTrack item, int position) {
		musicFiles.add(position - 1, item);
		notifyItemInserted(position);
		if (isLibrary && musicFiles.size() > position + 1) notifyItemChanged(position + 1);
	}

	public void moveItem(int from, int to) {
		if (from < to) {
			for (int i = from; i < to; i++) {
				Collections.swap(musicFiles, i - 1, i);
			}
		} else {
			for (int i = from; i > to; i--) {
				Collections.swap(musicFiles, i - 1, i - 2);
			}
		}

		notifyItemMoved(from, to);
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
			if (isLibrary) {
				view = LayoutInflater.from(parentContext).inflate(R.layout.library_header, parent, false);
				return new textViewHolder(view);
			} else {
				view = LayoutInflater.from(parentContext).inflate(R.layout.playlist_header, parent, false);
				return new playlistHeader(view);
			}
		} else {
			view = LayoutInflater.from(parentContext).inflate(R.layout.music_item_fixed_size, parent, false);

			return new musicViewHolder(view);
		}
	}

	@Override
	public void setHasStableIds(boolean hasStableIds) {
		super.setHasStableIds(true);
	}

	// selection based stuff
	private boolean itemIsSelected(int position) { return viewModel.getAddSongsSelection().getValue() != null && viewModel.getAddSongsSelection().getValue().contains(getItemId(position)); }

	private void toggleSelectedState(int position) {
		if (itemIsSelected(position)) {
			viewModel.removeSongFromAddSelection(getItemId(position));
		} else {
			viewModel.addSongToAddSelection(getItemId(position));
		}
		notifyItemChanged(position);
	}

	public void clearSelection() {
		viewModel.getAddSongsSelection().setValue(new ArrayList<>());
		notifyDataSetChanged();
	}

	public void toggleSelectMode() {
		selectionMode = !selectionMode;
		if (simpleTouchHelper != null) simpleTouchHelper.setSwipeEnabled(!selectionMode);
		if (!selectionMode) {
			clearSelection();
		}
		notifyDataSetChanged();
	}

	public void leaveSelectMode() {
		selectionMode = false;
	}

	public boolean getSelectionMode() {return selectionMode;}


	@SuppressLint("ClickableViewAccessibility")
	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		if (holder instanceof musicViewHolder) {
			final musicViewHolder musicHolder = (musicViewHolder) holder;
			final boolean selected = itemIsSelected(position);

			musicHolder.itemView.setSelected(selected);
			musicHolder.checkbox.setVisibility((selectionMode) ? View.VISIBLE : View.GONE);
			musicHolder.checkbox.setChecked(selected);

			musicHolder.dragHandle.setVisibility((editMode) ? View.VISIBLE : View.GONE);
			musicHolder.dragHandle.setOnTouchListener((v, event) -> {
//				v.performClick();
				if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
					touchHelper.startDrag(musicHolder);
					musicHolder.trackHolder.setPressed(true);
					musicHolder.trackHolder.setRadius(dragRadius);
					musicHolder.trackHolder.setElevation(dragElevation);

					return true;
				} else return false;
			});

			final musicTrack track = musicFiles.get(position - 1);

			// Reset the Translation
			musicHolder.trackHolder.setTranslationX(0f);

			final String title = track.getTitle();
			musicHolder.songTitle.setText(title);
			// put the Dot in the middle of the texts and add the values
			final String artist = track.getArtist();
			final String infoText = ((artist == null) ? "" : artist + " \u2022 ") + formatMillis(track.getDuration());
			musicHolder.subtextInfo.setText(infoText);

			final byte[][] albumArt = {null};
			if (track.hasCover || !track.testedForCover()) {
				// TODO should load it on another Thread
				Handler coverHandler = new Handler(Looper.getMainLooper());
				coverHandler.post(() -> {
					albumArt[0] = MusicItemAdapter.this.getAlbumArt(track.getPath());
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
			musicHolder.trackHolder.setOnLongClickListener(view -> {
				showSongContextMenu(albumArt[0], track.getTitle(), track.getArtist(), getItemId(position), position);
				return true;
			});

			if (isLibrary) {
				final char indexChar = Character.toUpperCase(title.charAt(0));
				if (position == HEADER_POSITION + 1 || (int) indexChar != (int) Character.toUpperCase(musicFiles.get(position - 2).getTitle().charAt(0))) {
					musicHolder.letterTextView.setText(Character.toString(indexChar));
					musicHolder.letterTextView.setVisibility(View.VISIBLE);
				} else musicHolder.letterTextView.setVisibility(View.GONE);
			}

			// handle playing behavior
			final boolean isPlayed = isSongPlaying(position);
			musicHolder.playingIndicator.setVisibility((isPlayed) ? View.VISIBLE : View.GONE);
			musicHolder.itemView.setEnabled(isPlayed);
		} else if (holder instanceof playlistHeader) {
			((playlistHeader) holder).updateHeader(editMode, playlistTitle, playlistDesc);
		}
		// TODO deal with the textViewHolder's
	}


	public void showSongContextMenu(byte[] albumArt, String title, String artist, long id, int position) {
		BottomSheetMenu songMenuFragment = new BottomSheetMenu(albumArt, title, artist, id, position, songMenuActions);

		try {
			final FragmentManager fragmentManager = ((FragmentActivity) parentContext).getSupportFragmentManager();
			songMenuFragment.show(fragmentManager, songMenuFragment.getTag());
		} catch (ClassCastException ignored) {}
	}

	@Override
	public int getItemCount() {
		return musicFiles.size() + 1;
	}

	@Override
	public long getItemId(int position) {
		if (position == 0) return 0;
		else {
			final musicTrack track = musicFiles.get(position - 1);
			if (track != null) {
				return track.id;
			} else return position;
		}
	}

	public void setSongMenuActions(BottomSheetMenu.bottomSheetMenuAction[] songMenuActions) {
		this.songMenuActions = songMenuActions;
	}

	public void toggleEditMode() {
		editMode = !editMode;
		if (simpleTouchHelper != null) {
			simpleTouchHelper.setSwipeEnabled(!editMode);
		}

		notifyDataSetChanged();
	}

	private boolean isSongPlaying(int position) {
		return !selectionMode && getItemId(position) == viewModel.queue.get(viewModel.currentTrackByPos);
	}

	public class musicViewHolder extends RecyclerView.ViewHolder {
		final TextView songTitle, subtextInfo, letterTextView;
		final ImageView albumCover, dragHandle, playingIndicator;
		final CheckBox checkbox;
		final MaterialCardView trackHolder;
		byte[] albumArt;

		public musicViewHolder(@NonNull View itemView) {
			super(itemView);
			songTitle = itemView.findViewById(R.id.songTitle);
			subtextInfo = itemView.findViewById(R.id.subtextInfo);
			albumCover = itemView.findViewById(R.id.albumCover);
			albumCover.setClipToOutline(true);
			checkbox = itemView.findViewById(R.id.checkBox);
			checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
				if (isChecked != itemView.isSelected()) toggleSelectedState(getAdapterPosition());
			});

			dragHandle = itemView.findViewById(R.id.dragHandle);

			letterTextView = itemView.findViewById(R.id.letter_index);

			playingIndicator = itemView.findViewById(R.id.playingIndicator);

			trackHolder = itemView.findViewById(R.id.PlaylistItem);

			trackHolder.setOnClickListener(view -> {
				if (!getSelectionMode()) {
					playlistControls.playSong(this, getItemId());
					view.setPressed(true);
				} else {
					toggleSelectedState(getAdapterPosition());
				}
			});

			itemView.findViewById(R.id.PlaylistItem).setDuplicateParentStateEnabled(true);
		}
	}

	public String getPlaylistTitle() { return playlistTitle; }

	public String getPlaylistDesc() { return playlistDesc; }

	public class playlistHeader extends RecyclerView.ViewHolder {
		final EditText TitleEditText, DescriptionEditText;
		final ImageView playlistCover;
		final LinearLayout addSongsHolder, playlistControlsHolder, modeHolder, playlistDescHolder;
		final ImageButton buttonPlay, buttonShuffle;
		byte[] playlistArt;

		public playlistHeader(@NonNull View itemView) {
			super(itemView);

			TitleEditText = itemView.findViewById(R.id.playlistNameEditText);
			TitleEditText.setEnabled(false);
			TitleEditText.setSaveEnabled(true);
			TitleEditText.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) { playlistTitle = s.toString(); }

				@Override
				public void afterTextChanged(Editable s) {}
			});

			DescriptionEditText = itemView.findViewById(R.id.playlistDescriptionEditText);
			DescriptionEditText.setEnabled(false);
			DescriptionEditText.setSaveEnabled(true);
			DescriptionEditText.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) { playlistDesc = s.toString(); }

				@Override
				public void afterTextChanged(Editable s) {}
			});

			playlistDescHolder = itemView.findViewById(R.id.descriptionHolder);
			addSongsHolder = itemView.findViewById(R.id.addSongsHolder);
			addSongsHolder.setOnClickListener(v -> playlistControls.addSongs());
			modeHolder = itemView.findViewById(R.id.modeHolder);
			playlistControlsHolder = itemView.findViewById(R.id.playlistControls);

			buttonPlay = itemView.findViewById(R.id.buttonPlayPlaylist);
			buttonShuffle = itemView.findViewById(R.id.buttonShufflePlaylist);
			buttonPlay.setOnClickListener(v -> {
				playlistControls.play();
				v.setActivated(true);
				buttonShuffle.setActivated(false);
			});
			buttonShuffle.setOnClickListener(v -> {
				playlistControls.shuffle();
				v.setActivated(true);
				buttonPlay.setActivated(false);
			});


			playlistCover = itemView.findViewById(R.id.playlistCover);
			playlistCover.setClipToOutline(true);

			itemView.setFocusable(View.NOT_FOCUSABLE);
			itemView.setClickable(false);
		}

		public void updateHeader(boolean inEditMode, String title, String description) {
			TitleEditText.setEnabled(inEditMode);
			TitleEditText.setText(title);
			DescriptionEditText.setEnabled(inEditMode);
			DescriptionEditText.setText(description);
			if (description.length() > 0 || inEditMode) {
				playlistDescHolder.setVisibility(View.VISIBLE);
			} else {
				playlistDescHolder.setVisibility(View.GONE);
			}

			if (inEditMode) {
				DescriptionEditText.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
				TitleEditText.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);

				addSongsHolder.setVisibility(View.VISIBLE);
				playlistControlsHolder.setVisibility(View.GONE);
				itemView.setFocusable(View.FOCUSABLE_AUTO);
			} else {
				DescriptionEditText.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
				TitleEditText.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

				addSongsHolder.setVisibility(View.GONE);
				playlistControlsHolder.setVisibility(View.VISIBLE);
				itemView.setFocusable(View.NOT_FOCUSABLE);
			}
		}
	}

	private static class textViewHolder extends RecyclerView.ViewHolder {
		private final TextView textView;

		public textViewHolder(@NonNull View itemView) {
			super(itemView);
			textView = itemView.findViewById(R.id.textHolder);
		}
	}

	public interface PlaylistControls {
		void play();

		void shuffle();

		void addSongs();

		void playSong(musicViewHolder viewHolder, Long id);
	}
}

