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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.data.Playlist;

import java.util.ArrayList;
import java.util.List;

public class FragmentPlaylistOverview extends Fragment {

	public FragmentPlaylistOverview() {
		super(R.layout.playlist_overview_layout);
	}

	private RecyclerView playlistListView;

	private MusicPlayerViewModel viewModel;

	private NavController navController;

	private MainActivity app;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		viewModel = new ViewModelProvider(requireActivity()).get(MusicPlayerViewModel.class);
		app = (MainActivity) getActivity();
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

//		Playlist testPl = new Playlist(0, "myplaylist", "desc");
//		testPl.songs = new ArrayList<>();
//		testPl.songs.add(0);
//		testPl.songs.add(3);
//		playlists.add(testPl);
		playlistListView.setAdapter(new PlaylistOverviewListViewAdapter(playlists));

		super.onViewCreated(view, savedInstanceState);
	}

	private class PlaylistOverviewListViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
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
				return new headerViewHolder(view);
			} else {
				view = getLayoutInflater().inflate(R.layout.playlist_overview_item, parent, false);
				return new playlistViewHolder(view);
			}
		}

		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
			if (position != HEADER_POSITION) {
				final Playlist playlistItem = data.get(position-1);
				playlistViewHolder playlist = (playlistViewHolder) holder;
				playlist.playlistName.setText(playlistItem.name);
				// set the cover

				// to navigate to the PlaylistFragment
				holder.itemView.setOnClickListener(v -> {
					FragmentPlaylistOverviewDirections.ActionPlaylistOverviewToPlaylist action = FragmentPlaylistOverviewDirections.actionPlaylistOverviewToPlaylist();
					action.setLibrary(false);
					action.setPlaylist(playlistItem);
					navController.navigate(action);
				});
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

		private class playlistViewHolder extends RecyclerView.ViewHolder {
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

		private class headerViewHolder extends RecyclerView.ViewHolder {

			public headerViewHolder(@NonNull View itemView) {
				super(itemView);
				itemView.setOnClickListener(v -> {
					// create new playlist
				});
			}
		}
	}
}
