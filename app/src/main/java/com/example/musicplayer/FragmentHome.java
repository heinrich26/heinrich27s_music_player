package com.example.musicplayer;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.musicplayer.R;

public class FragmentHome extends Fragment {
	public FragmentHome() {
		super(R.layout.player_start_page);
	}

	private TextView toLibrary, toPlaylists, toArtists, toAlbums;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		toLibrary = view.findViewById(R.id.action_to_library);
		toLibrary.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Navigation.findNavController(view).navigate(R.id.action_fragmentHome_to_fragmentPlaylist);
			}
		});
	}
}
