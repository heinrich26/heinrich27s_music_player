package com.example.musicplayer;

import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.data.Playlist;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class EditPlaylistFragment extends Fragment {

	public EditPlaylistFragment() {
		super(R.layout.playlist_layout);
	}

	private RecyclerView playlistView;
	private MusicAdapterFixedHeight playlistAdapter;
	private ArrayList<musicTrack> playlistTracks;

	private MusicPlayerViewModel viewModel;

	private MainActivity app;

	private String playlistTitle, playlistDesc;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		viewModel = new ViewModelProvider(requireActivity()).get(MusicPlayerViewModel.class);
		app = (MainActivity) getActivity();

		setHasOptionsMenu(true);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		Playlist playlist = FragmentPlaylistArgs.fromBundle(getArguments()).getPlaylist();
		playlistView = view.findViewById(R.id.fragmentPlaylistView);

		playlistTracks = playlist.songs.stream().map(viewModel.musicFilesList::get).collect(Collectors.toCollection(ArrayList::new));
		playlistTitle = playlist.name;
		playlistDesc = playlist.description;

		playlistAdapter = new MusicAdapterFixedHeight(requireContext(), playlistTracks, viewModel.songDatabaseDao, playlistTitle, playlistDesc);
		SongMenuBottomSheetFragment.SongMenuActions songMenuActions = new SongMenuBottomSheetFragment.SongMenuActions() {
			@Override
			public void actionRemoveFromLibrary(int position) {}

			@Override
			public void actionAddToQueue(int position) {}

			@Override
			public void actionAddToPlaylist(int position) {}
		};

		playlistAdapter.setSongMenuActions(songMenuActions);
		playlistView.setAdapter(playlistAdapter);
		playlistView.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));

		int[] ATTRS = new int[]{android.R.attr.listDivider};

		TypedArray val = requireActivity().obtainStyledAttributes(ATTRS);
		Drawable divider = val.getDrawable(0);
		val.recycle();
		int inset = getResources().getDimensionPixelSize(R.dimen.dividerInsetValue);
		InsetDrawable insetDivider = new InsetDrawable(divider, inset, 0, 0, 0);

		DividerItemDecoration decoration = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
		decoration.setDrawable(insetDivider);
		playlistView.addItemDecoration(decoration);

		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onDestroy() {
		// save the playlist to the Dao
		super.onDestroy();
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		inflater.inflate(R.menu.menu_new_pl, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}



	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_done:
				// create the playlist!
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}
}