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

public class BottomSheetMenu extends BottomSheetDialogFragment {
	private final byte[] albumArt;
	private final String title, artist;
	private final long id;
	private final int position;
	private final bottomSheetMenuAction[] menuActions;

	protected static final int TYPE_SONG = 0;
	protected static final int TYPE_PLAYLIST = 1;
	// more types, like album etc.

	BottomSheetMenu(byte[] albumArt, String title, String artist, long id, int position, bottomSheetMenuAction[] menuActions) {
		this.albumArt = albumArt;
		this.title = title;
		this.artist = artist;
		this.id = id;
		this.position = position;
		this.menuActions = menuActions;
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

		final LinearLayout actionHolder = layout.findViewById(R.id.action_holder);

		int layoutPos = 1;
		for (bottomSheetMenuAction action: menuActions) {
			View item = inflater.inflate(R.layout.song_menu_item, actionHolder, false);
			item.setOnClickListener(v -> {
				action.action(position, id);
				dismiss();
			});

			((TextView) item.findViewById(R.id.menu_item_title)).setText(action.getName());
			((ImageView) item.findViewById(R.id.menu_item_icon)).setImageResource(action.getIcon());

			actionHolder.addView(item, layoutPos);
			layoutPos ++;
		}

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

	public interface bottomSheetMenuAction {
		void action(int position, long id);

		int getName();
		int getIcon();
	}
}
