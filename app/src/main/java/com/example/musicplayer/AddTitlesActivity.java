package com.example.musicplayer;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.musicplayer.data.Playlist;
import com.example.musicplayer.data.musicDatabase;

import org.apache.commons.lang3.ArrayUtils;

import java.io.Serializable;
import java.util.ArrayList;

public class AddTitlesActivity extends AppCompatActivity {

	private NavController navController;

	private Toolbar toolbar;

	private MusicPlayerViewModel viewModel;

	private Playlist playlistToAdd;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_titles);

		final Serializable playlistExtra = getIntent().getSerializableExtra("playlistToAdd");
		if (playlistExtra instanceof Playlist) {
			playlistToAdd = (Playlist) playlistExtra;
		}

		viewModel = new ViewModelProvider(this).get(MusicPlayerViewModel.class);

		viewModel.songDatabase = musicDatabase.getInstance(getApplicationContext());
		viewModel.songDatabaseDao = viewModel.songDatabase.musicDatabaseDao();

		viewModel.musicDict.putAll(viewModel.songDatabaseDao.getMusicDict());

		toolbar = findViewById(R.id.main_appbar);
		setSupportActionBar(toolbar);

		NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.add_songs_host_fragment);
		navController = navHostFragment.getNavController();
		NavigationUI.setupWithNavController(toolbar, navController);

		final Observer<ArrayList<Long>> addSelectionObserver = new Observer<ArrayList<Long>>() {
			private final Resources res = getResources();
			private final TextView addSelectionInfo = findViewById(R.id.selection_info);
			private final String baseText = getString(R.string.addTitlesToPlaylist, (playlistToAdd.name.length() > 0) ? playlistToAdd.name : getString(R.string.unnamed_playlist));

			@Override
			public void onChanged(ArrayList<Long> longs) {
				addSelectionInfo.setText((longs.size() == 0) ? baseText : res.getQuantityString(R.plurals.titlesAddedToPlaylist, longs.size(), longs.size(), playlistToAdd.name));
			}
		};
		viewModel.getAddSongsSelection().observe(this, addSelectionObserver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_done, menu);
		return super.onCreateOptionsMenu(menu);
	}

	public static String RESULT_KEY = "com.example.musicplayer.newSongs";

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == R.id.action_done) {
			final ArrayList<Long> longs = viewModel.getAddSongsSelection().getValue();
			final Intent resultIntent = getIntent();

			if (longs != null && longs.size() != 0) {
				final long[] newSongs = ArrayUtils.toPrimitive(longs.toArray(new Long[0]));
				resultIntent.putExtra(RESULT_KEY, newSongs);
			}
			setResult(RESULT_OK, resultIntent);
			finish();
		}
		return super.onOptionsItemSelected(item);
	}
}