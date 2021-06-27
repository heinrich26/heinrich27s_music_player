package com.example.musicplayer;

import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.musicplayer.data.Playlist;

import java.util.List;

public class FragmentPlaylistOverview extends Fragment {

	public FragmentPlaylistOverview() {
		super(R.layout.playlist_overview_layout);
	}

	private ListView playlistListView;

	private MusicPlayerViewModel viewModel;

	private MainActivity app;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		viewModel = new ViewModelProvider(requireActivity()).get(MusicPlayerViewModel.class);
		app = (MainActivity) getActivity();
	}

	@Override
	public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
		playlistListView = view.findViewById(R.id.playlistListView);
		playlistListView.setAdapter(new PlaylistOverviewListViewAdapter(viewModel.songDatabaseDao.getPlaylists()));
		super.onViewCreated(view, savedInstanceState);
	}

	private class PlaylistOverviewListViewAdapter implements ListAdapter {
		private final List<Playlist> data;

		PlaylistOverviewListViewAdapter(List<Playlist> data) {
			this.data = data;
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			return false;
		}

		@Override
		public void registerDataSetObserver(DataSetObserver observer) {

		}

		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {

		}

		@Override
		public int getCount() {
			return data.size();
		}

		@Override
		public Object getItem(int position) {
			return data.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(R.layout.playlist_overview_layout, parent, false);
			}
			((TextView) convertView.findViewById(R.id.playlistNameTV)).setText(data.get(position).name);

			return convertView;
		}

		@Override
		public int getItemViewType(int position) {
			return 0;
		}

		@Override
		public int getViewTypeCount() {
			return 0;
		}

		@Override
		public boolean isEmpty() {
			return data == null;
		}
	}
}
