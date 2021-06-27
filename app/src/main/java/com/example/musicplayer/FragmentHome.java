package com.example.musicplayer;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

public class FragmentHome extends Fragment {
	public FragmentHome() {
		super(R.layout.player_start_page);
	}

	private TextView toLibrary, toPlaylists, toArtists, toAlbums;
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
		super.onViewCreated(view, savedInstanceState);
		toLibrary = view.findViewById(R.id.action_to_library);
		toLibrary.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentHomeDirections.ActionHomeToPlaylist action = FragmentHomeDirections.actionHomeToPlaylist();
				action.setLibrary(true);
				Navigation.findNavController(view).navigate(action);
			}
		});
	}
}
