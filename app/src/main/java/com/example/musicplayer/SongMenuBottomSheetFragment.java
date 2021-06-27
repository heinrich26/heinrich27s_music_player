package com.example.musicplayer;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class SongMenuBottomSheetFragment extends BottomSheetDialogFragment {
	private final byte[] albumArt;
	private final String title;
	private final String artist;
	private final int position;
	private final SongMenuActions songMenuActions;

	SongMenuBottomSheetFragment(byte[] albumArt, String title, String artist, int position, SongMenuActions songMenuActions) {
		this.albumArt = albumArt;
		this.title = title;
		this.artist = artist;
		this.position = position;
		this.songMenuActions = songMenuActions;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.TransparentBottomSheetDialogTheme);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		final View layout = inflater.inflate(R.layout.song_menu, container, false);

		final TextView songTitle = layout.findViewById(R.id.songTitle);
		final TextView songInfo = layout.findViewById(R.id.songInfo);
		final ImageView albumView = layout.findViewById(R.id.albumArt);

		final LinearLayout remFromLib = layout.findViewById(R.id.actionRemoveFromLibrary);
		final LinearLayout addToPL = layout.findViewById(R.id.actionAddToPlaylist);
		final LinearLayout addToQueue = layout.findViewById(R.id.actionAddToQueue);

		remFromLib.setOnClickListener(v -> {
			songMenuActions.actionRemoveFromLibrary(position);
			dismiss();
		});

		addToPL.setOnClickListener(v -> {
			songMenuActions.actionAddToPlaylist(position);
			dismiss();
		});

		addToQueue.setOnClickListener(v -> {
			songMenuActions.actionAddToQueue(position);
			dismiss();
		});

		songTitle.setText(title);

		if (artist != null) songInfo.setText(artist);
		else songInfo.setVisibility(View.GONE);

		if (albumArt != null) {
			Glide.with(this)
					.asBitmap()
					.load(albumArt)
					.placeholder(R.drawable.ic_note_twocolor)
					.into(albumView);
		} else albumView.setBackgroundResource(R.drawable.ic_note_twocolor);

		return layout;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		return super.onCreateDialog(savedInstanceState);
	}

	public interface SongMenuActions {
		void actionRemoveFromLibrary(int position);
		void actionAddToQueue(int position);
		void actionAddToPlaylist(int position);
	}
}
