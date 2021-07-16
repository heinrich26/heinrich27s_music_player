package com.example.musicplayer;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

public class FragmentHome extends Fragment {
	public FragmentHome() {
		super(R.layout.player_start_page);
	}

	private TextView toArtists, toAlbums;
	private MusicPlayerViewModel viewModel;
	private MainActivity app;

	private boolean inAddMode;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		viewModel = new ViewModelProvider(requireActivity()).get(MusicPlayerViewModel.class);

		inAddMode = (getActivity() instanceof AddTitlesActivity);

		app = (inAddMode) ? (MainActivity) getActivity().getParent() : (MainActivity) getActivity();
	}

	@Override
	public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		final TextView toLibrary = view.findViewById(R.id.action_to_library);
		toLibrary.setOnClickListener(v -> {
			FragmentHomeDirections.ActionHomeToPlaylist action = FragmentHomeDirections.actionHomeToPlaylist();
			action.setLibrary(true);
			Navigation.findNavController(view).navigate(action);
			v.setClickable(false);
		});
		final TextView toPlaylists = view.findViewById(R.id.action_to_playlists);
		toPlaylists.setOnClickListener(v -> {
			Navigation.findNavController(view).navigate(FragmentHomeDirections.actionHomeToPlaylistOverview());
			v.setClickable(false);
		});
	}
}
