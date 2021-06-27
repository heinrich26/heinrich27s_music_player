package com.example.musicplayer;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.example.musicplayer.data.folderRef;
import com.example.musicplayer.data.musicDatabase;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.String.format;

@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity{

	public MainActivity() {}

	private MusicPlayerViewModel viewModel;

	private AudioManager audioManager;
	private int maxVolume;
	private int currentVolume;

	private NavController navController;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main_layout);
		viewModel = new ViewModelProvider(this).get(MusicPlayerViewModel.class);
		viewModel.mediaPlayer = mp;

		viewModel.songMenuActions = new SongMenuBottomSheetFragment.SongMenuActions() {
			@Override
			public void actionRemoveFromLibrary(int position) {}

			@Override
			public void actionAddToQueue(int position) { addToQueue(position); }

			@Override
			public void actionAddToPlaylist(int position) {}
		};
		
		playPauseButton = findViewById(R.id.playPauseButton);
		currentTrackPager = findViewById(R.id.currentTrackPager);

		final Toolbar mainAppbar = findViewById(R.id.main_appbar);
//		setSupportActionBar(mainAppbar);
//		getSupportActionBar().setDisplayShowTitleEnabled(false);
//		getSupportActionBar().setDisplayShowHomeEnabled(true);

		NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
		navController = navHostFragment.getNavController();
		NavigationUI.setupWithNavController((Toolbar) mainAppbar, navController);

		audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

		getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, new VolumeContentObserver((Build.VERSION.SDK_INT >= 28) ? new Handler(Looper.myLooper()) : new Handler()));
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		int id = item.getItemId();

		if (id == R.id.action_add_song) {
			openDirectory(null);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	// Permission Stuff

	private static final String[] PERMISSIONS = {
			Manifest.permission.READ_EXTERNAL_STORAGE
	};

	private static final int REQUEST_PERMISSIONS = 12345;

	private static final int PERMISSION_COUNT = 1;



	private boolean arePermissionsDenied() {
		for (int i = 0; i < PERMISSION_COUNT; i++) {
			if (checkSelfPermission(PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED) return true;
		}
		return false;
	}

	@RequiresApi(api = Build.VERSION_CODES.Q)
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (arePermissionsDenied()) {
			((ActivityManager) (this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
			recreate();
		} else onResume();
	}


	// Methods for selecting new Music Directory's
	public void openDirectory(View view) {
		// Choose a directory using the system's file picker.
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

		startActivityForResult(intent, 2);
	}

	@RequiresApi(api = Build.VERSION_CODES.Q)
	@SuppressLint("MissingSuperCall")
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
		if (requestCode == 2
				&& resultCode == Activity.RESULT_OK) {
			// The result data contains a URI for the document or directory that the user selected.
			if (resultData != null) {
				Uri uri = resultData.getData();
				final int takeFlags = resultData.getFlags()
						& (Intent.FLAG_GRANT_READ_URI_PERMISSION
						| Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
				getContentResolver().takePersistableUriPermission(uri, takeFlags);
				// Perform operations on the document using its URI.
				DocumentFile document = DocumentFile.fromTreeUri(this, DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri)));
				if (document != null) addFolderToLibrary(document, true, false);
			}
		}
	}

	@RequiresApi(api = Build.VERSION_CODES.Q)
	public void addFolderToLibrary(DocumentFile treeDocument, boolean rootCall, boolean load) {
		if (viewModel.songDatabaseDao.exists(treeDocument.getUri().toString()) && !load) return;
		// don't take the Root dir (trying its best)
		else if ("content://com.android.externalstorage.documents/tree/primary%3A/document/primary%3A".equals(treeDocument.getUri().toString())) {
			Toast.makeText(this, "You can't select the Filesystem Root, please select a subfolder!", Toast.LENGTH_SHORT).show();
			return;
		}

		final DocumentFile[] childDocs = treeDocument.listFiles();

		if (childDocs.length == 0) {
			// empty folder
			return;
		} else  {
			final String[] projection = {
					MediaStore.Audio.Media.TITLE, // 0
					MediaStore.Audio.Media.ARTIST, // 1
					MediaStore.Audio.Media.ALBUM, // 2
					MediaStore.Audio.Media.DURATION, // 3
					MediaStore.Audio.Media.RELATIVE_PATH, // 4
					MediaStore.Audio.Media.DATA // 5
			};

			if (!load && rootCall) viewModel.songDatabaseDao.insertFolder(new folderRef(treeDocument.getUri().toString()));
			for (DocumentFile child : childDocs) {
				if (child.isDirectory()) {
					addFolderToLibrary(child, false, load);
				} else if (musicMimes.contains(child.getType())) {
					final Uri mediaUri;
					try {
						mediaUri = MediaStore.getMediaUri(this, child.getUri());
					} catch (IllegalArgumentException exception) {
						continue;
					}

					// if ignored, we don't want this!
					if (viewModel.songDatabaseDao.getIgnoredUris().contains(mediaUri.toString())) continue;


					final Cursor cursor;
					cursor = this.getContentResolver().query(mediaUri, projection, null, null, null);

					if (cursor != null && cursor.moveToNext()) {
						// make unknown Artist null
						String artist = cursor.getString(1);
						if (artist.equals(MediaStore.UNKNOWN_STRING)) artist = null;

						// determine if the album is the folder name, so we can doublecheck
						String album = cursor.getString(2);
						if (cursor.getString(4).contains(album)) {
							MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
							try {
								mediaMetadataRetriever.setDataSource(this, mediaUri);
							} catch (Exception e) {
								// the file is corrupt, can't be played etc.
								continue;
							}
							album = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
							mediaMetadataRetriever.release();
						}

						final musicTrack track = new musicTrack(mediaUri.toString(),
								cursor.getString(0), artist,	album,
								Integer.parseInt(cursor.getString(3)), cursor.getString(5), false, false);
						viewModel.musicFilesList.add(track);
						viewModel.songDatabaseDao.addTrack(track);

						cursor.close();
					}
				}
			}
		}
// notify the fragment
//		if (rootCall) {
//			if (viewModel.musicFilesList.size() != 0 && musicAdapter != null) {
//				// update Adapter from data
//				musicAdapter.notifyDataSetChanged();
//			}
//		}
	}


	private boolean isMusicPlayerInit;
	

	private final ArrayList<String> musicMimes = new ArrayList<>(Arrays.asList("audio/mpeg", "audio/x-wav", "audio/ogg"));


	private final MediaPlayer mp = new MediaPlayer();
	private boolean musicPlayerPrepared = false;


	private FloatingActionButton playPauseButton;
	private AppCompatImageButton playerShuffleButton, playerRepeatButton;

	private SeekBar seekBar;
	private Slider playerSeekBar, playerVolumeBar;
	private boolean holdSeekbarUpdate = false;
	private boolean holdPlayerSeekbarUpdate = false;
	private boolean holdVolumeBarUpdate = false;

	public ImageButton playerPlayPauseButton, playerNextButton, playerPrevButton;

	private TextView playerSongTitle, playerSongArtistAlbumInfo, playerSongCurrentPosition, playerSongTimeRemaining;

	private BottomSheetBehavior<ConstraintLayout> bottomSheetPlayer;

	private MusicAdapter musicAdapterCurrentTrack;
	private AlbumArtAdapter playerAlbumArtAdapter;
	private ViewPager2 currentTrackPager, playerCarousel;
	private boolean scrollTrackOnly = false;



	private int playMusicFile(String path) {
		if (musicPlayerPrepared) mp.reset();
		try {
			mp.setDataSource(path);
			try {
				mp.prepare();
			} catch (Exception ignored) {
				try {
					mp.prepare();
				} catch (Exception ignored2) {}
			}
			musicPlayerPrepared = true;
			mp.start();
			songRepeated = false;
		} catch (IOException ignored) {}

		return mp.getDuration();
	}

	private volatile int songPosition;

	private volatile boolean isPlaying;
	public boolean isPlaying() { return isPlaying; }

	public volatile boolean shuffle;
	public static int REPEAT_NEVER = 0;
	public static int REPEAT = 1;
	public static int REPEAT_ONCE = 2;
	private boolean songRepeated;

	private int repeatMode = REPEAT_NEVER;

	private static volatile int songDuration = 0;

	private Thread currentUpdaterThread;

	@SuppressLint("DefaultLocale")
	public static String formatMillis(int millis) {
		return format("%01d:%02d", (millis / 60000) % 60, (millis / 1000) % 60);
	}

	@SuppressLint("DefaultLocale")
	private static String formatTimeRemaining(int millis) {
		if (millis >= songDuration) return "- 0:00";
		else millis = songDuration - millis;
		return format("- %01d:%02d", (millis / 60000) % 60, (millis / 1000) % 60);
	}


	@RequiresApi(api = Build.VERSION_CODES.Q)
	@Override
	protected void onResume() {
		super.onResume();
		if (arePermissionsDenied()) {
			requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
			return;
		}

		if (!(isMusicPlayerInit)) {
			mp.setOnErrorListener((mp, what, extra) -> true);
			mp.setOnCompletionListener(mediaPlayer -> {
				if (repeatMode == REPEAT_ONCE) {
					if (songRepeated) nextSong(null);
					else {
						prevSong(null);
						songRepeated = true;
					}
				} else if (repeatMode == REPEAT) {
					prevSong(null);
					songRepeated = true;
				}
				else nextSong(null);

				// implement autoplay
			});



			seekBar = findViewById(R.id.seekBar);
			playerSeekBar = findViewById(R.id.playerSeekBar);
			playerSeekBar.setValueTo((float) seekBar.getMax());
			playerSeekBar.setValue((float) Math.min(seekBar.getProgress(), songDuration));
			playerVolumeBar = findViewById(R.id.playerVolumeBar);
			playerVolumeBar.setValueTo((float) maxVolume);
			playerVolumeBar.setValue((float) currentVolume);

			
			playerPlayPauseButton = findViewById(R.id.playerPlayPauseButton);
			playerPlayPauseButton.setSelected(isPlaying);

			playerShuffleButton = findViewById(R.id.playerShuffleButton);
			playerShuffleButton.setOnClickListener(v -> {
				v.setSelected(!v.isSelected());
				shuffle = v.isSelected();
			});

			playerRepeatButton = findViewById(R.id.playerRepeatButton);
			playerRepeatButton.setOnClickListener(new View.OnClickListener() {
				private int state = 0;
				@Override
				public void onClick(View v) {
					state = (state == 2) ? 0 : state+1;
					if (state != 2) {
						playerRepeatButton.setImageResource(R.drawable.ic_repeat);
					} else playerRepeatButton.setImageResource(R.drawable.ic_repeat_one);
					v.setSelected(state != 0);
					repeatMode = state;
				}
			});

			playerNextButton = findViewById(R.id.playerNextButton);
			playerPrevButton = findViewById(R.id.playerPrevButton);

			// prepare the scroller anim
			playerSongTitle = findViewById(R.id.playerSongTitle);

			playerSongArtistAlbumInfo = findViewById(R.id.playerArtistAlbumTextView);


			playerSongCurrentPosition = findViewById(R.id.playerCurrentPosition);
			playerSongTimeRemaining = findViewById(R.id.playerSongDuration);


			playerCarousel = findViewById(R.id.playerAlbumPager);


			// init the database
			viewModel.songDatabase = musicDatabase.getInstance(this);
			viewModel.songDatabaseDao = viewModel.songDatabase.musicDatabaseDao();

			// ask for a folder if the Database is empty
			viewModel.musicFilesList.addAll(viewModel.songDatabaseDao.getMusicTracks());
			Handler albumArtHandler = new Handler(Looper.getMainLooper());
			albumArtHandler.post(() -> {
				// bekannte Cover Cachen
			});

			if (viewModel.musicFilesList.size() < viewModel.songDatabaseDao.getFolderUris().size()) {
				// load Library from database
				for (String folder : viewModel.songDatabaseDao.getFolderUris()) {
					DocumentFile folderDocument = DocumentFile.fromTreeUri(this, DocumentsContract.buildChildDocumentsUriUsingTree(Uri.parse(folder), DocumentsContract.getTreeDocumentId(Uri.parse(folder))));
					if (folderDocument != null) addFolderToLibrary(folderDocument, true, true);
				}
			} else if (viewModel.songDatabaseDao.getFolderUris().size() == 0) {
				openDirectory(null);
			}

			// move to fragment
//			musicAdapter = new MusicAdapterFixedHeight(this, viewModel.musicFilesList, viewModel.songDatabaseDao);
//			musicAdapter.setSongMenuActions(new SongMenuBottomSheetFragment.SongMenuActions() {
//				@Override
//				public void actionRemoveFromLibrary(int position) {
//					removeFromLibrary(position);
//				}
//
//				@Override
//				public void actionAddToQueue(int position) {
//					viewModel.queue.add(viewModel.currentTrackByPos + 1, musicAdapter.musicFiles.get(position));
//					musicAdapterCurrentTrack.notifyItemInserted(viewModel.currentTrackByPos + 1);
//					playerAlbumArtAdapter.notifyItemInserted(viewModel.currentTrackByPos + 1);
//				}
//
//				@Override
//				public void actionAddToPlaylist(int position) {
//
//				}
//			});
//
//			playlistView.setAdapter(musicAdapter);
//			playlistView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
//
//			int[] ATTRS = new int[]{android.R.attr.listDivider};
//
//			TypedArray val = obtainStyledAttributes(ATTRS);
//			Drawable divider = val.getDrawable(0);
//			val.recycle();
//			int inset = getResources().getDimensionPixelSize(R.dimen.dividerInsetValue);
//			InsetDrawable insetDivider = new InsetDrawable(divider, inset, 0, 0, 0);
//
//			DividerItemDecoration decoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
//			decoration.setDrawable(insetDivider);
//			playlistView.addItemDecoration(decoration);
//
//			SimpleSwipeController playlistSwipeController = new SimpleSwipeController(this) {
//				@Override
//				public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
//					final int position = viewHolder.getAdapterPosition();
//					removeFromLibrary(position);
//				}
//			};
//
//			ItemTouchHelper itemTouchHelper = new ItemTouchHelper(playlistSwipeController);
//			itemTouchHelper.attachToRecyclerView(playlistView);


			// init the listeners for the Seekbars
			seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) { holdSeekbarUpdate = true; }

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					if (musicPlayerPrepared) {
						songPosition = seekBar.getProgress();
						mp.seekTo(songPosition);
					}
					holdSeekbarUpdate = false;
				}
			});
			playerSeekBar.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
				@Override
				public void onStartTrackingTouch(@NonNull Slider slider) {
					holdPlayerSeekbarUpdate = true;
					playerSongTimeRemaining.setSelected(true);
					playerSongCurrentPosition.setSelected(true);
				}

				@Override
				public void onStopTrackingTouch(@NonNull Slider slider) {
					holdPlayerSeekbarUpdate = false;
					playerSongTimeRemaining.setSelected(false);
					playerSongCurrentPosition.setSelected(false);
					seekTo((int) playerSeekBar.getValue(), MainActivity.PLAYER_SEEKBAR);
				}
			});

			playerVolumeBar.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
				@Override
				public void onStartTrackingTouch(@NonNull Slider slider) {
					holdVolumeBarUpdate = true;
				}

				@Override
				public void onStopTrackingTouch(@NonNull Slider slider) {
					currentVolume = (int) slider.getValue();
					audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
					holdVolumeBarUpdate = false;
				}
			});
			playerVolumeBar.addOnChangeListener((slider, value, fromUser) -> {
				final int newVal = (int) value;
				if (newVal != currentVolume && fromUser) audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVal, 0);
			});


			currentUpdaterThread = new Thread() {
				public void run() {
					while (this.isAlive()) {
						try {
							sleep(500);
						} catch (InterruptedException exception) {
							exception.printStackTrace();
						}
						if (isPlaying) {
							try {
								songPosition = mp.getCurrentPosition();
							} catch (Exception e) {
								// sometimes it's doing weired things, so we need to prepare
							}
							final String timeRemaining = formatTimeRemaining(songPosition);
							final String timeElapsed = formatMillis(songPosition);
							runOnUiThread(() -> updateSeekBar(timeElapsed, timeRemaining));
						}
					}
				}
			};


			viewModel.queue.addAll(viewModel.musicFilesList);
			Collections.shuffle(viewModel.queue);


			musicAdapterCurrentTrack = new MusicAdapter(this, viewModel.queue, viewModel.songDatabaseDao);
			musicAdapterCurrentTrack.setClickListener((view, position) -> openPlayer(view));
			currentTrackPager.setAdapter(musicAdapterCurrentTrack);

			viewModel.currentTrackByPos = currentTrackPager.getCurrentItem();
			currentTrackPager.setSelected(isPlaying);
			currentTrackPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
				@Override
				public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

				@Override
				public void onPageSelected(int position) {
					if (playerCarousel.getCurrentItem() != position) playerCarousel.setCurrentItem(position, isPlayerVisible());
					System.out.println(scrollTrackOnly);
					if (position != viewModel.currentTrackByPos || scrollTrackOnly) {
						viewModel.currentTrackByPos = position;
						if (scrollTrackOnly) scrollTrackOnly = false;
						else playSong(position, true);
					}
				}

				@Override
				public void onPageScrollStateChanged(int state) {}
			});


			final ImageButton closeButton = findViewById(R.id.playerButtonClose);

			bottomSheetPlayer = BottomSheetBehavior.from(findViewById(R.id.bottomSheetPlayer));
			bottomSheetPlayer.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
				private boolean dragging;

				@Override
				public void onStateChanged(@NonNull View bottomSheet, int newState) {
					if (newState == BottomSheetBehavior.STATE_DRAGGING) {
						dragging = true;
						closeButton.setSelected(true);
					} else if (dragging) {
						dragging = false;
						closeButton.setSelected(false);
					}
				}

				@Override
				public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
			});

			playerAlbumArtAdapter = new AlbumArtAdapter(this, viewModel.queue, viewModel.songDatabaseDao);
			playerCarousel.setAdapter(playerAlbumArtAdapter);
			playerCarousel.setClipToPadding(false);
			playerCarousel.setClipToOutline(false);
			playerCarousel.setClipChildren(false);
			playerCarousel.setOffscreenPageLimit(2);
			final View carouselFrame = playerCarousel.getChildAt(0);
			carouselFrame.setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
			carouselFrame.setDuplicateParentStateEnabled(true);
			playerCarousel.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
				@Override
				public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

				@Override
				public void onPageSelected(int position) {
					if (currentTrackPager.getCurrentItem() != position) currentTrackPager.setCurrentItem(position, true);
				}

				@Override
				public void onPageScrollStateChanged(int state) {}
			});

			CompositePageTransformer pageTransformer = new CompositePageTransformer();
			pageTransformer.addTransformer(new MarginPageTransformer((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32f, getResources().getDisplayMetrics())));
			pageTransformer.addTransformer((page, position) -> {
				float r = 1 - Math.abs(position);
				page.setScaleY(0.85f + r * 0.15f);
			});

			playerCarousel.setPageTransformer(pageTransformer);

			((ViewGroup) playerCarousel.getChildAt(0)).setClipChildren(false);

			// start thread after player is there
			currentUpdaterThread.start();

			// update the Player once because its needed
			final Handler trackUpdater = new Handler(Looper.getMainLooper());
			trackUpdater.post(() -> {
				try {
					final musicTrack track = viewModel.queue.get(viewModel.currentTrackByPos);
					updateSongInfo(track.getTitle(), track.getArtist(), track.getAlbum(), true);
				} catch (Exception ignored) {}
			});


			isMusicPlayerInit = true;
		}
	}

	// playback related Methods
	public void playSong(int position, boolean intentional) {
		// Clear all scroll animations
		if (playerTitleAnim != null) {
			playerTitleAnim.removeAllListeners();
			playerTitleAnim.cancel();
			playerInfoAnim = null;
			// reset the transformation
			playerSongTitle.setScrollX(0);
		}
		if (playerInfoAnim != null) {
			playerInfoAnim.removeAllListeners();
			playerInfoAnim.cancel();
			playerInfoAnim = null;
			// reset the transformation
			playerSongArtistAlbumInfo.setScrollX(0);
		}

		musicTrack item;
		if (!intentional) {
			item = viewModel.musicFilesList.get(position);
			viewModel.currentTrackByPos = (viewModel.queue.size() == 0)? 0 : viewModel.currentTrackByPos + 1;
			viewModel.queue.add(viewModel.currentTrackByPos, item);
			musicAdapterCurrentTrack.notifyItemInserted(viewModel.currentTrackByPos);
			playerAlbumArtAdapter.notifyItemInserted(viewModel.currentTrackByPos);
			if (viewModel.queue.size() != 1) {
				scrollTrackOnly = true;
				currentTrackPager.setCurrentItem(viewModel.currentTrackByPos, true);
			}
		} else {
			item = viewModel.queue.get(position);
		}

		songDuration = playMusicFile(item.getAbsPath());

		// update the Seekbars
		seekBar.setProgress(0);
		playerSeekBar.setValue(0f);
		seekBar.setMax(songDuration);
		playerSeekBar.setValueTo((float) songDuration + 1);
		// add one, because player stops at songDuration + 1
		updateSongInfo(item.getTitle(), item.getArtist(), item.getAlbum(), false);

		setPlaying(true);


		int itemsToAdd = Math.max(0, 4 - viewModel.queue.size() + position);
		if (itemsToAdd != 0) {
			final Handler handler = new Handler(Looper.getMainLooper());
			handler.post(() -> {
				for (int i = 0; i < itemsToAdd; i++) {
					viewModel.queue.add(viewModel.musicFilesList.get(ThreadLocalRandom.current().nextInt(0, viewModel.musicFilesList.size())));
				}
				musicAdapterCurrentTrack.notifyItemRangeInserted(position+1, itemsToAdd);
				playerAlbumArtAdapter.notifyItemRangeInserted(position+1, itemsToAdd);
			});
		}
	}

	public void playPauseToggle(View view) {
		if (isPlaying) {
			mp.pause();
			setPlaying(false);
		} else if (!musicPlayerPrepared) {
			if (viewModel.queue.size() != 0) playSong(viewModel.currentTrackByPos, true);
			else playSong(ThreadLocalRandom.current().nextInt(0, viewModel.musicFilesList.size()), false);
		} else {
			mp.start();
			setPlaying(true);
		}
	}

	public void playFromView(View view) {
		int position = ((RecyclerView) view.getParent()).getChildLayoutPosition(view) - 1;
		playSong(position, false);
	}

	public void openPlayer(View view) {
		if (bottomSheetPlayer.getState() == BottomSheetBehavior.STATE_COLLAPSED) bottomSheetPlayer.setState(BottomSheetBehavior.STATE_EXPANDED);
	}

	public void nextSong(View view) {
		currentTrackPager.setCurrentItem(currentTrackPager.getCurrentItem() + 1, true);
	}

	public void prevSong(View view) {
		if (isPlaying && songPosition >= 5000) playSong(viewModel.currentTrackByPos, true);
		else currentTrackPager.setCurrentItem(Math.max(0, viewModel.currentTrackByPos - 1), true);
	}

	public void hidePlayer(View view) { bottomSheetPlayer.setState(BottomSheetBehavior.STATE_COLLAPSED); }

	public void addToQueue(int position) {
		viewModel.queue.add(viewModel.currentTrackByPos + 1, viewModel.musicFilesList.get(position));
		musicAdapterCurrentTrack.notifyItemInserted(viewModel.currentTrackByPos + 1);
		playerAlbumArtAdapter.notifyItemInserted(viewModel.currentTrackByPos + 1);
	}


	// known IDs for all Seekbars
	public static final int MAIN_SEEKBAR = 0;
	public static final int PLAYER_SEEKBAR = 1;

	public void seekTo(int position, int seekbar) {
		if (musicPlayerPrepared) {
			songPosition = position;
			mp.seekTo(position);
		}

		if (seekbar == MAIN_SEEKBAR) {
			// set else bars to position
			updateSeekBar(formatMillis(position), formatTimeRemaining(position));
		} else if (seekbar == PLAYER_SEEKBAR) {
			seekBar.setProgress(position, true);
			playerSongTimeRemaining.setText(formatMillis(position));
			playerSongCurrentPosition.setText(formatTimeRemaining(position));
		}
	}

	private void updateSeekBar(String formattedTime, String formattedRemainingTime) {
		if (!holdSeekbarUpdate) seekBar.setProgress(songPosition);
		if (!holdPlayerSeekbarUpdate) playerSeekBar.setValue((float) Math.min(seekBar.getProgress(), songDuration));
		playerSongTimeRemaining.setText(formattedTime);
		playerSongCurrentPosition.setText(formattedRemainingTime);
	}

	private void updateSongInfo(String newTitle, String artist, String album, boolean passive) {
		playerSongTitle.setText(newTitle);
		String newInfo;
		if (artist == null) {
			if (album == null) newInfo = "";
			else newInfo = album;
		} else if (album == null) newInfo = artist;
		else newInfo = artist + " \u2012 " + album;
		playerSongArtistAlbumInfo.setText(newInfo);


		final int titleTextWidth = (int) playerSongArtistAlbumInfo.getPaint().measureText(newTitle);
		final int maxWidth = (int) findViewById(R.id.playerAlbumPlaceholder).getMeasuredWidth();
		if (titleTextWidth > maxWidth) {
			playerTitleAnim = new AnimatorSet();
			ObjectAnimator anim1 = ObjectAnimator.ofInt(playerSongTitle, "scrollX", 0, titleTextWidth);
			ObjectAnimator anim2 = ObjectAnimator.ofInt(playerSongTitle, "scrollX", -titleTextWidth, 0);
			anim1.setInterpolator(new LinearInterpolator());
			anim2.setInterpolator(new LinearInterpolator());
			playerTitleAnim.play(anim1).before(anim2);
			playerTitleAnim.setDuration( titleTextWidth * 10 );
			playerTitleAnim.addListener(new TextScrollAdapter());
			if (!passive) playerTitleAnim.start();
		} else {
			playerTitleAnim = null;
		}

		final int infoTextWidth = (int) playerSongArtistAlbumInfo.getPaint().measureText(newInfo);
		if (infoTextWidth > maxWidth) {
			playerInfoAnim = new AnimatorSet();
			ObjectAnimator anim1 = ObjectAnimator.ofInt(playerSongArtistAlbumInfo, "scrollX", 0, infoTextWidth);
			ObjectAnimator anim2 = ObjectAnimator.ofInt(playerSongArtistAlbumInfo, "scrollX", -infoTextWidth, 0);
			anim1.setInterpolator(new LinearInterpolator());
			anim2.setInterpolator(new LinearInterpolator());
			playerInfoAnim.play(anim1).before(anim2);
			playerInfoAnim.setDuration( infoTextWidth * 10 );
			playerInfoAnim.addListener(new TextScrollAdapter());
			if (!passive) playerInfoAnim.start();
		} else {
			playerInfoAnim = null;
		}
	}

	private boolean isPlayerVisible() { return !(bottomSheetPlayer.getState() == BottomSheetBehavior.STATE_COLLAPSED); }

	private AnimatorSet playerTitleAnim, playerInfoAnim;

	private void setPlaying(boolean playing) {
		isPlaying = playing;

		// update the button Icon
		playPauseButton.setSelected(playing);
		playerPlayPauseButton.setSelected(playing);
		playerCarousel.setSelected(playing);

		// start Scrolling animation on the text if necessary
		if (playing) {
			if (playerTitleAnim != null) playerTitleAnim.start();
			if (playerInfoAnim != null) playerInfoAnim.start();
		} else {
			// reset the transformation
			if (playerTitleAnim != null) {
				playerTitleAnim.cancel();
				playerSongTitle.setScrollX(0);
			}
			if (playerInfoAnim != null) {
				playerInfoAnim.cancel();
				playerSongArtistAlbumInfo.setScrollX(0);
			}
		}
	}

	@Override
	protected void onDestroy() {
		viewModel.songDatabaseDao.updateTracks(viewModel.musicFilesList);
		super.onDestroy();
	}

	private static class TextScrollAdapter extends AnimatorListenerAdapter {
		private boolean animCanceled;

		@Override
		public void onAnimationCancel(Animator animation) {
			animCanceled = true;
			animation.end();
		}

		@Override
		public void onAnimationEnd(Animator animation) {
			if (!animCanceled) {
				animation.start();
			}
		}

		@Override
		public void onAnimationStart(Animator animation) {
			animCanceled = false;
		}
	}

	public class VolumeContentObserver extends ContentObserver {

		public VolumeContentObserver(Handler handler) {
			super(handler);
		}

		@Override
		public boolean deliverSelfNotifications() {
			return super.deliverSelfNotifications();
		}

		@Override
		public void onChange(boolean selfChange) {
			if (!holdVolumeBarUpdate) {
				currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
				if (isMusicPlayerInit) playerVolumeBar.setValue(currentVolume);
			}
		}
	}
}