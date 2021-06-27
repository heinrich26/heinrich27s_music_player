package com.example.musicplayer;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.data.ignores;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

public class FragmentPlaylist extends Fragment {
	public FragmentPlaylist() {
		super(R.layout.playlist_layout);
	}

	private RecyclerView playlistView;
	private MusicAdapterFixedHeight playlistAdapter;
	private ArrayList<musicTrack> playlistTracks;

	private MusicPlayerViewModel viewModel;

	private MainActivity app;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		viewModel = new ViewModelProvider(requireActivity()).get(MusicPlayerViewModel.class);
		app = (MainActivity) getActivity();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		playlistView = view.findViewById(R.id.fragmentPlaylistView);
		playlistTracks = viewModel.musicFilesList;
		playlistAdapter = new MusicAdapterFixedHeight(requireContext(), playlistTracks, viewModel.songDatabaseDao);
		SongMenuBottomSheetFragment.SongMenuActions songMenuActions = new SongMenuBottomSheetFragment.SongMenuActions() {
			@Override
			public void actionRemoveFromLibrary(int position) {
				removeFromLibrary(position);
			}

			@Override
			public void actionAddToQueue(int position) {
				viewModel.songMenuActions.actionAddToQueue(position);
			}

			@Override
			public void actionAddToPlaylist(int position) {
				viewModel.songMenuActions.actionAddToPlaylist(position);
			}
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

		SimpleSwipeController playlistSwipeController = new SimpleSwipeController(requireContext()) {
			@Override
			public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
				final int position = viewHolder.getAdapterPosition();
				removeFromLibrary(position);
			}
		};

		ItemTouchHelper itemTouchHelper = new ItemTouchHelper(playlistSwipeController);
		itemTouchHelper.attachToRecyclerView(playlistView);

		super.onViewCreated(view, savedInstanceState);
	}

	public void removeFromLibrary(int position) {
		final musicTrack item = playlistAdapter.musicFiles.get(position);

		// machen das nur ignored wird, wenn es die Library ist!
		viewModel.songDatabaseDao.addIgnore(new ignores(item.getPath()));
		playlistAdapter.removeItem(position);


		Snackbar snackbar = Snackbar
				.make(playlistView, "Song was removed from the Playlist.", Snackbar.LENGTH_LONG);
		snackbar.setAction("UNDO", view -> {

			playlistAdapter.restoreItem(item, position);
			playlistView.scrollToPosition( (position > 0) ? position-1 : 0 );
			viewModel.songDatabaseDao.deleteIgnore(new ignores(item.getPath()));
		});

		snackbar.setActionTextColor(Color.BLUE);
		snackbar.show();
	}

	@Override
	public void onDestroy() {
		// save the playlist to the Dao
		super.onDestroy();
	}
}