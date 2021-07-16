package com.example.musicplayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.data.Playlist;
import com.example.musicplayer.data.ignores;
import com.google.android.material.snackbar.Snackbar;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.musicplayer.AddTitlesActivity.RESULT_KEY;

public class FragmentPlaylist extends Fragment {
	public FragmentPlaylist() {
		super(R.layout.playlist_layout);
		playlistTracks = new ArrayList<>();
	}

	private RecyclerView playlistView;
	private MusicAdapterFixedHeight playlistAdapter;
	private final List<musicTrack> playlistTracks;

	private MusicPlayerViewModel viewModel;

	private MainActivity app;
	private NavController navController;

	private String playlistTitle, playlistDesc;
	private Playlist mPlaylist;
	private boolean inAddMode, createNew, isLibrary;

	private androidx.appcompat.view.ActionMode actionMode;

	final ActivityResultLauncher<Intent> startAddMode = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
			result -> {
				if (result.getResultCode() == Activity.RESULT_OK) {
					final Intent data = result.getData();
					final long[] resultData = data.getLongArrayExtra(RESULT_KEY);
					if (resultData != null) {
						FragmentPlaylist.this.addToAdapterList(resultData);
					}
				}
			}
	);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		inAddMode = getActivity() instanceof AddTitlesActivity;

		viewModel = new ViewModelProvider(requireActivity()).get(MusicPlayerViewModel.class);

		final FragmentPlaylistArgs args = FragmentPlaylistArgs.fromBundle(getArguments());
		isLibrary = args.getLibrary();
		createNew = args.getCreateNew();
		mPlaylist = (createNew) ? new Playlist(0, "", "") : args.getPlaylist();
		if (!isLibrary && mPlaylist.songs == null) mPlaylist.songs = new ArrayList<>();

		app = (inAddMode) ? (MainActivity) requireActivity().getParent() : (MainActivity) requireActivity();
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (!inAddMode) {
			setHasOptionsMenu(true);
		}
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		if (!inAddMode) inflater.inflate(R.menu.playlist_menu, menu);

		if (isLibrary) menu.findItem(R.id.action_start_edit_mode).setVisible(false);
		super.onCreateOptionsMenu(menu, inflater);
	}


	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.action_start_edit_mode) {
			actionMode = app.startSupportActionMode(actionModeCallback);
		} else if (itemId == R.id.action_add_to_playlist) {
			if (isLibrary) app.openDirectory(null);
			else addSongsAction();
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
		navController = Navigation.findNavController(view);

		playlistView = view.findViewById(R.id.fragmentPlaylistView);

		if (isLibrary) {
			playlistTracks.addAll(viewModel.songDatabaseDao.getMusicTracks());
			playlistTitle = getString(R.string.library);
			playlistDesc = "";
		} else {
			if (mPlaylist.songs != null && mPlaylist.songs.size() != 0) {
				playlistTracks.addAll(mPlaylist.songs.stream().map(viewModel.musicDict::get).collect(Collectors.toList()));
			}
			playlistTitle = mPlaylist.name;
			playlistDesc = mPlaylist.description;
		}


		playlistAdapter = new MusicAdapterFixedHeight(requireContext(), playlistTracks, viewModel.songDatabaseDao, isLibrary);
		playlistAdapter.setPlaylistTitle(playlistTitle);
		playlistAdapter.setPlaylistDesc(playlistDesc);
		BottomSheetMenu.bottomSheetMenuAction[] menuActions = new BottomSheetMenu.bottomSheetMenuAction[] {
			new BottomSheetMenu.bottomSheetMenuAction() {
				@Override
				public void action(int position, long id) { removeFromLibrary(position); }

				@Override
				public int getName() { return (isLibrary) ? R.string.remove_from_library : R.string.remove_from_playlist; }

				@Override
				public int getIcon() { return R.drawable.ic_delete; }
			}, new BottomSheetMenu.bottomSheetMenuAction() {
				@Override
				public void action(int position, long id) { app.addToQueue(id); }

				@Override
				public int getName() { return R.string.next_title; }

				@Override
				public int getIcon() { return R.drawable.ic_queue_add; }
			}, new BottomSheetMenu.bottomSheetMenuAction() {
				@Override
				public void action(int position, long id) {
					// TODO add to playlist directly
				}

				@Override
				public int getName() { return R.string.add_to_playlist; }

				@Override
				public int getIcon() { return R.drawable.ic_playlist_add; }
			}
		};

		playlistAdapter.setSongMenuActions(menuActions);
		playlistAdapter.setItemClickListener(new MusicAdapterFixedHeight.MusicItemClickListener() {
			@Override
			public void onClick(MusicAdapterFixedHeight.musicViewHolder viewHolder, Long id) {
				if (!inAddMode) {
					app.playSong(id);

					viewHolder.itemView.setActivated(true);
				}
			}
		});

		playlistAdapter.setAddSongsAction(v -> addSongsAction());


		if (inAddMode) playlistAdapter.toggleSelectMode();
		else if (createNew) actionMode = app.startSupportActionMode(actionModeCallback);

		playlistView.setAdapter(playlistAdapter);

		playlistView.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));

		int[] ATTRS = new int[] {android.R.attr.listDivider};

		TypedArray val = requireActivity().obtainStyledAttributes(ATTRS);
		Drawable divider = val.getDrawable(0);
		val.recycle();
		int inset = getResources().getDimensionPixelSize(R.dimen.dividerInsetValue);
		InsetDrawable insetDivider = new InsetDrawable(divider, inset, 0, 0, 0);

		DividerItemDecoration decoration = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
		decoration.setDrawable(insetDivider);
		playlistView.addItemDecoration(decoration);


		if (!inAddMode) {
			final SimpleSwipeController playlistSwipeController = new SimpleSwipeController(requireContext()) {
				@Override
				public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
					final int from = viewHolder.getAdapterPosition();
					final int to = target.getAdapterPosition();

					if (to == 0) return false;

					playlistAdapter.moveItem(from, to);

					return true;
				}

				@Override
				public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
					final int position = viewHolder.getAdapterPosition();
					if (direction == ItemTouchHelper.LEFT) {
						removeFromLibrary(position);
					} else {
						playlistAdapter.notifyItemChanged(position);
						app.addToQueue(playlistAdapter.getItemId(position));
					}
				}


			};

			final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(playlistSwipeController);
			itemTouchHelper.attachToRecyclerView(playlistView);
			playlistAdapter.setTouchHelper(itemTouchHelper, playlistSwipeController);
		}

		super.onViewCreated(view, savedInstanceState);
	}

	private void addSongsAction() {
		final Intent addSongsIntent = new Intent(requireActivity(), AddTitlesActivity.class);
		addSongsIntent.putExtra("playlistToAdd", mPlaylist);
		startAddMode.launch(addSongsIntent);
	}

	public void removeFromLibrary(int position) {
		final musicTrack item =	playlistTracks.get(position - 1);
		if (item == null) return;


		if (isLibrary) {
			viewModel.songDatabaseDao.addIgnore(new ignores(item.getPath()));
			viewModel.songDatabaseDao.deleteTrack(item);
		}
		playlistAdapter.removeItem(position);

		Snackbar snackbar = Snackbar
				.make(playlistView, "Song was removed from the Playlist.", Snackbar.LENGTH_LONG);
		snackbar.setAction("UNDO", view -> {

			playlistAdapter.restoreItem(item, position);
			playlistView.scrollToPosition(Math.max(position + 1, 1));
			if (isLibrary) {
				viewModel.songDatabaseDao.addTrack(item);
				viewModel.songDatabaseDao.deleteIgnore(new ignores(item.getPath()));
			}
		});

		snackbar.setActionTextColor(Color.BLUE);
		snackbar.show();
	}

	@Override
	public void onDestroy() {
		savePlaylistData();
		super.onDestroy();
	}


	public void addToAdapterList(long[] ids) {
		// TODO try to allow multilples
		final int startIndex = playlistTracks.size();
		final List<Long> newSongs = Arrays.asList(ArrayUtils.toObject(ids));
		if (ids != null) {
			playlistTracks.addAll(newSongs.stream().map(id -> viewModel.musicDict.get(id)).collect(Collectors.toList()));
			playlistAdapter.notifyItemRangeInserted(startIndex, ids.length);
			if (mPlaylist.songs == null) mPlaylist.songs = new ArrayList<>();
			mPlaylist.songs.addAll(newSongs);
		}
	}

	private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {

		private boolean pressedDone;

		private String oldTitle, oldDescription;

		// Called when the action mode is created; startActionMode() was called
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// Inflate a menu resource providing context menu items
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.menu_done, menu);

			mode.setTitle(R.string.edit_playlist);

			playlistAdapter.toggleEditMode();

			pressedDone = false;

			return true;
		}

		// Called each time the action mode is shown. Always called after onCreateActionMode, but
		// may be called multiple times if the mode is invalidated.
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false; // Return false if nothing is done
		}

		// Called when the user selects a contextual menu item
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			if (item.getItemId() == R.id.action_done) {
				playlistTitle = ((MusicAdapterFixedHeight.playlistHeader) playlistView.getChildViewHolder(playlistView.getChildAt(0))).playlistTitle.getText().toString();
				playlistDesc = ((MusicAdapterFixedHeight.playlistHeader) playlistView.getChildViewHolder(playlistView.getChildAt(0))).playlistDescription.getText().toString();

				mPlaylist.name = playlistTitle;
				mPlaylist.description = playlistDesc;

				savePlaylistData();
				pressedDone = true;
				mode.finish(); // Action picked, so close the CAB
				return true;
			}
			return false;
		}

		// Called when the user exits the action mode
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			if (!pressedDone) {
				playlistTracks.clear();
				if (mPlaylist.songs != null && mPlaylist.songs.size() != 0) {
					playlistTracks.addAll(mPlaylist.songs.stream().map(viewModel.musicDict::get).collect(Collectors.toList()));
				}
				playlistAdapter.setPlaylistTitle(playlistDesc);
				playlistAdapter.setPlaylistDesc(playlistDesc);
				playlistAdapter.notifyDataSetChanged();
			}
			playlistAdapter.toggleEditMode();

			InputMethodManager inputManager = (InputMethodManager) app.getSystemService(Context.INPUT_METHOD_SERVICE);
			if (app.getCurrentFocus() != null) {
				inputManager.hideSoftInputFromWindow(app.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
			}

			actionMode = null;
		}
	};

	public void savePlaylistData() {
		if (!isLibrary && !inAddMode && !(createNew && mPlaylist.songs.size() == 0)) {
			mPlaylist.songs.clear();
			for (int i = 1; i < playlistAdapter.getItemCount(); i++) mPlaylist.songs.add(playlistAdapter.getItemId(i));
			if (!createNew) { viewModel.songDatabaseDao.updatePlaylist(mPlaylist); }
			else { viewModel.songDatabaseDao.addPlaylist(mPlaylist); }
		}
	}

	public void updateLibraryData() {
		if (isLibrary) {
			playlistTracks.clear();
			playlistTracks.addAll(viewModel.songDatabaseDao.getMusicTracks());
			playlistAdapter.notifyDataSetChanged();
		}
	}
}
