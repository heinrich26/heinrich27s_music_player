package com.example.musicplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.data.Playlist;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class FragmentPlaylistOverview extends Fragment {

	public FragmentPlaylistOverview() {
		super(R.layout.playlist_overview_layout);
	}

	private RecyclerView playlistListView;
	private PlaylistOverviewListViewAdapter playlistOverviewAdapter;

	private MusicPlayerViewModel viewModel;

	private NavController navController;

	private MainActivity app;

	private final BottomSheetMenu.bottomSheetMenuAction[] songMenuActions = new BottomSheetMenu.bottomSheetMenuAction[] {
		new BottomSheetMenu.bottomSheetMenuAction() {
			@Override
			public void action(int position, long id) { playlistOverviewAdapter.removeItem(position); }

			@Override
			public int getName() { return R.string.delete; }

			@Override
			public int getIcon() { return R.drawable.ic_delete; }
		}, new BottomSheetMenu.bottomSheetMenuAction() {
			@Override
			public void action(int position, long id) {
				Playlist playlist = playlistOverviewAdapter.data.get(position - 1);
				if (playlist.songs != null && playlist.songs.size() != 0) {
					app.addToQueue(playlist.songs);
				}
			}

			@Override
			public int getName() { return R.string.next_title; }

			@Override
			public int getIcon() { return R.drawable.ic_queue_add; }
		}, new BottomSheetMenu.bottomSheetMenuAction() {
			@Override
			public void action(int position, long id) {
				// TODO allow adding to other playlists!
			}

			@Override
			public int getName() { return R.string.add_to_playlist; }

			@Override
			public int getIcon() { return R.drawable.ic_playlist_add; }
		}
	};

	private boolean inAddMode;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		viewModel = new ViewModelProvider(requireActivity()).get(MusicPlayerViewModel.class);
		inAddMode = getActivity() instanceof AddTitlesActivity;

		app = (inAddMode) ? (MainActivity) getActivity().getParent() : (MainActivity) getActivity();
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
		navController = Navigation.findNavController(view);

		playlistListView = view.findViewById(R.id.playlistListView);
		List<Playlist> playlists = viewModel.songDatabaseDao.getPlaylists();

		if (playlists.size() == 0) {
			Playlist testPl = new Playlist(0, "myplaylist", "desc");
			testPl.songs = new ArrayList<>();
			testPl.songs.addAll(new ArrayList<>(viewModel.musicDict.keySet()).subList(0, Math.min(viewModel.musicDict.size(), 7)));
			playlists.add(testPl);
		}

		playlistOverviewAdapter = new PlaylistOverviewListViewAdapter(playlists);
		playlistListView.setAdapter(playlistOverviewAdapter);

		super.onViewCreated(view, savedInstanceState);
	}

	public class PlaylistOverviewListViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		private static final int TYPE_HEADER = 0;
		private static final int TYPE_ITEM = 1;
		private static final int HEADER_POSITION = 0;

		private final List<Playlist> data;

		PlaylistOverviewListViewAdapter(List<Playlist> data) {
			this.data = data;
		}

		@NonNull
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View view;
			if (viewType == TYPE_HEADER) {
				view = getLayoutInflater().inflate(R.layout.playlist_overview_header, parent, false);
				if (inAddMode) view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
				return new headerViewHolder(view);
			} else {
				view = getLayoutInflater().inflate(R.layout.playlist_overview_item, parent, false);
				return new playlistViewHolder(view);
			}
		}

		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
			if (position != HEADER_POSITION) {
				final Playlist playlist = data.get(position-1);

				final String playlistName = (playlist.name.equals("")) ? getString(R.string.unnamed_playlist) : playlist.name;
				playlistViewHolder playlistItem = (playlistViewHolder) holder;
				playlistItem.playlistName.setText(playlistName);
				// set the cover

				// to navigate to the PlaylistFragment
				holder.itemView.setOnClickListener(v -> {
					FragmentPlaylistOverviewDirections.ActionPlaylistOverviewToPlaylist action = FragmentPlaylistOverviewDirections.actionPlaylistOverviewToPlaylist();
					action.setLibrary(false);
					action.setPlaylist(playlist);
					navController.navigate(action);
				});

				holder.itemView.setOnLongClickListener(v -> {
					showSongContextMenu(null, playlistName, (playlist.description != null) ? playlist.description : "", getItemId(position), position);
					return true;
				});
			} else {
				if (inAddMode) holder.itemView.setVisibility(View.GONE);
				else {
					holder.itemView.setOnClickListener(v -> {
						FragmentPlaylistOverviewDirections.ActionPlaylistOverviewToPlaylist action = FragmentPlaylistOverviewDirections.actionPlaylistOverviewToPlaylist();
						action.setLibrary(false);
						action.setCreateNew(true);
						navController.navigate(action);
					});
				}
			}
		}

		@Override
		public int getItemViewType(int position) {
			if (position == HEADER_POSITION) return TYPE_HEADER;

			return TYPE_ITEM;
		}

		@Override
		public int getItemCount() {
			return data.size() + 1;
		}

		public void removeItem(int position) {
			final Playlist playlist = data.remove(position - 1);
			notifyItemRemoved(position);

			Snackbar snackbar = Snackbar.make(playlistListView, "Playlist was removed!", Snackbar.LENGTH_LONG);
			snackbar.setAction("UNDO", view -> {
				data.add(position - 1, playlist);
				notifyItemInserted(position);
			});
			snackbar.addCallback(new Snackbar.Callback() {
				@Override
				public void onDismissed(Snackbar transientBottomBar, int event) {
					if (event != BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_ACTION) {
						viewModel.songDatabaseDao.deletePlaylist(playlist);
					}
					super.onDismissed(transientBottomBar, event);
				}
			});
			snackbar.show();
		}

		public void showSongContextMenu(byte[] playlistArt, String name, String description, long id, int position) {
			BottomSheetMenu songMenuFragment = new BottomSheetMenu(playlistArt, name, description, id, position, songMenuActions);

			try {
				final FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
				songMenuFragment.show(fragmentManager, songMenuFragment.getTag());
			} catch (ClassCastException ignored) {}
		}
	}

	public static class playlistViewHolder extends RecyclerView.ViewHolder {
		TextView playlistName;
		ImageView playlistCover;
		byte[] playlistArt;

		public playlistViewHolder(@NonNull View itemView) {
			super(itemView);
			playlistName = itemView.findViewById(R.id.playlistNameTV);
			playlistCover = itemView.findViewById(R.id.playlistCover);
			playlistCover.setClipToOutline(true);
		}
	}

	public static class headerViewHolder extends RecyclerView.ViewHolder {

		public headerViewHolder(@NonNull View itemView) {
			super(itemView);
			itemView.setOnClickListener(v -> {
				// create new playlist
			});
		}
	}
}
