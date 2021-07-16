package com.example.musicplayer;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ContentResolver;
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
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;
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

import com.example.musicplayer.data.Playlist;
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
import java.util.stream.Collectors;

import static java.lang.String.format;


public class MainActivity extends AppCompatActivity{

	public MainActivity() {}

	private MusicPlayerViewModel viewModel;

	private AudioManager audioManager;
	private int maxVolume;
	private int currentVolume;

	private NavController navController;
	Toolbar mainAppbar;

	private final ActivityResultLauncher<Uri> askForFolderLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(),
			new ActivityResultCallback<Uri>() {
				@RequiresApi(api = Build.VERSION_CODES.Q)
				@Override
				public void onActivityResult(Uri result) {
					if (result != null) {
							getContentResolver().takePersistableUriPermission(result, Intent.FLAG_GRANT_READ_URI_PERMISSION
									| Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

							// Perform operations on the document using its URI.
							Uri treeUri = DocumentsContract.buildChildDocumentsUriUsingTree(result, DocumentsContract.getTreeDocumentId(result));
							if (treeUri != null) addFolderToLibrary(treeUri, true, false);
						}
					}
			});

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		viewModel = new ViewModelProvider(this).get(MusicPlayerViewModel.class);
		viewModel.mediaPlayer = mp;
		
		playPauseButton = findViewById(R.id.playPauseButton);
		currentTrackPager = findViewById(R.id.currentTrackPager);

		mainAppbar = findViewById(R.id.main_appbar);
		setSupportActionBar(mainAppbar);

		NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
		navController = navHostFragment.getNavController();
		NavigationUI.setupWithNavController(mainAppbar, navController);

		audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);


		Window window = getWindow();

		// clear FLAG_TRANSLUCENT_STATUS flag:
		window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

		// add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
		window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);


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
		askForFolderLauncher.launch(Uri.fromFile(Environment.getExternalStorageDirectory()));
	}

	@RequiresApi(api = Build.VERSION_CODES.Q)
	@WorkerThread
	public void addFolderToLibrary(Uri treeUri, boolean rootCall, boolean load) {

		if (viewModel.songDatabaseDao.exists(treeUri.toString()) && !load) return;
			// don't take the Root dir (trying its best)
		else if ("content://com.android.externalstorage.documents/tree/primary%3A/document/primary%3A".equals(treeUri.toString())) {
			Toast.makeText(this, "You can't select the Filesystem Root, please select a subfolder!", Toast.LENGTH_SHORT).show();
			return;
		}

		final DocumentFile treeDocument = DocumentFile.fromTreeUri(this, treeUri);
		final DocumentFile[] childDocs = treeDocument.listFiles();


		if (childDocs.length == 0) {
			// empty folder
			return;
		} else {
			final String[] projection = {
					MediaStore.Audio.Media.TITLE, // 0
					MediaStore.Audio.Media.ARTIST, // 1
					MediaStore.Audio.Media.ALBUM, // 2
					MediaStore.Audio.Media.DURATION, // 3
					MediaStore.Audio.Media.RELATIVE_PATH, // 4
					MediaStore.Audio.Media.DATA, // 5
					MediaStore.Audio.Media._ID
			};

			if (!load && rootCall) viewModel.songDatabaseDao.insertFolder(new folderRef(treeUri.toString()));

			final ArrayList<String> mediaIDs = new ArrayList<>();
			final ArrayList<Uri> mediaUris = new ArrayList<>();

			for (DocumentFile child : childDocs) {
				if (child.isDirectory()) {
					getIdsFromDocumentTree(mediaIDs, mediaUris, child);
				} else if (musicMimes.contains(child.getType())) {
					final Uri mediaUri;
					final String id;
					try {
						mediaUri = MediaStore.getMediaUri(this, child.getUri());
						mediaUris.add(mediaUri);
						final String[] splitId = mediaUri.getPath().split("/");
						id = splitId[splitId.length - 1];
					} catch (IllegalArgumentException exception) {
						continue;
					}


					// if ignored, we don't want this!
					if (viewModel.songDatabaseDao.getIgnoredUris().contains(mediaUri.toString())) continue;
					mediaIDs.add(id);
				}
			}

			final String selection = MediaStore.Audio.Media._ID + " IN (" + makePlaceholders(mediaIDs.size()) + ")";
//			AND " + MediaStore.Audio.Media.IS_MUSIC + "=?"
			final String[] selectionArgs = new String[mediaIDs.size()];
			for (int i = 0; i < mediaIDs.size(); i++)
				selectionArgs[i] = mediaIDs.get(i);
//			selectionArgs[mediaIDs.size()] = String.valueOf(true);


			final ContentResolver contentResolver = getContentResolver();
			final Cursor cursor;


//			cursor = new MergeCursor(new Cursor[] {
//					contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null, null),
//					contentResolver.query(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, projection, selection, selectionArgs, null, null)
//			});

			// TODO Handle Internal and External Storage

			cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null, null);


			if (cursor != null && cursor.moveToFirst()) {
				while (cursor.moveToNext()) {
					// make unknown Artist null
					final String id = cursor.getString(6);
					System.out.println(id);
					final Uri mediaUri = mediaUris.get(mediaIDs.indexOf(id));

					String artist = cursor.getString(1);
					if (artist.equals(MediaStore.UNKNOWN_STRING)) artist = null;

					// determine if the album is the folder name, so we can doublecheck
					String album = cursor.getString(2);
					final String path = cursor.getString(5);
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

					final musicTrack track = new musicTrack(0, mediaUri.toString(),
							cursor.getString(0), artist, album,
							cursor.getInt(3), path,
							false, false);
					viewModel.songDatabaseDao.addTrack(track);
				}
				cursor.close();
			}
		}
		viewModel.musicDict.putAll(viewModel.songDatabaseDao.getMusicDict());
		final FragmentPlaylist playlist = (FragmentPlaylist) getSupportFragmentManager().findFragmentById(R.id.fragmentPlaylist);
		if (playlist != null) playlist.updateLibraryData();
		refreshQueue(null);
	}

	@WorkerThread
	@RequiresApi(api = Build.VERSION_CODES.Q)
	private void getIdsFromDocumentTree(ArrayList<String> mediaIDs, ArrayList<Uri> mediaUris, DocumentFile treeDocument) {
		final DocumentFile[] childDocs = treeDocument.listFiles();

		if (childDocs.length == 0)
			// empty folder
			return;

		for (DocumentFile child : childDocs) {
			if (child.isDirectory()) {
				getIdsFromDocumentTree(mediaIDs, mediaUris, child);
			} else if (musicMimes.contains(child.getType())) {
				final Uri mediaUri;
				final String id;
				try {
					mediaUri = MediaStore.getMediaUri(this, child.getUri());
					mediaUris.add(mediaUri);
					final String[] splitId = mediaUri.getPath().split("/");
					id = splitId[splitId.length - 1];
				} catch (IllegalArgumentException exception) {
					continue;
				}

				// if ignored, we don't want this!
				if (viewModel.songDatabaseDao.getIgnoredUris().contains(mediaUri.toString())) continue;
				mediaIDs.add(id);
			}
		}
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
			viewModel.songDatabase = musicDatabase.getInstance(getApplicationContext());
			viewModel.songDatabaseDao = viewModel.songDatabase.musicDatabaseDao();

			// ask for a folder if the Database is empty
			viewModel.musicDict.putAll(viewModel.songDatabaseDao.getMusicDict());
			Handler albumArtHandler = new Handler(Looper.getMainLooper());
			albumArtHandler.post(() -> {
				// bekannte Cover Cachen
			});

			if (viewModel.musicDict.size() < viewModel.songDatabaseDao.getFolderUris().size()) {
				// load Library from database
				for (String folder : viewModel.songDatabaseDao.getFolderUris()) {
					Uri treeUri = DocumentsContract.buildChildDocumentsUriUsingTree(Uri.parse(folder), DocumentsContract.getTreeDocumentId(Uri.parse(folder)));
					if (treeUri != null) addFolderToLibrary(treeUri, true, true);
				}
			} else if (viewModel.songDatabaseDao.getFolderUris().size() == 0) {
				openDirectory(null);
			}


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


			refreshQueue(null);


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
					if (position != viewModel.currentTrackByPos || scrollTrackOnly) {
						viewModel.currentTrackByPos = position;
						if (scrollTrackOnly) scrollTrackOnly = false;
						else playSong(position); // intentional call
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

	private void refreshQueue(Playlist playlist) {
		if (playlist == null) {
			viewModel.queue.addAll(viewModel.musicDict.values());
			if (shuffle) Collections.shuffle(viewModel.queue);
		}
		if (musicAdapterCurrentTrack != null) musicAdapterCurrentTrack.notifyDataSetChanged();
		if (playerAlbumArtAdapter != null) playerAlbumArtAdapter.notifyDataSetChanged();
	}

	// playback related Methods
	public void playSong(int position) {
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


		musicTrack item = viewModel.queue.get(position);


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
					viewModel.queue.add(viewModel.musicDict.get(new ArrayList<>(viewModel.musicDict.keySet()).get(ThreadLocalRandom.current().nextInt(viewModel.musicDict.size()))));
				}
				musicAdapterCurrentTrack.notifyItemRangeInserted(position+1, itemsToAdd);
				playerAlbumArtAdapter.notifyItemRangeInserted(position+1, itemsToAdd);
			});
		}
	}

	public void playSong(long id) {
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

		musicTrack item = viewModel.musicDict.get(id);
		viewModel.currentTrackByPos = (viewModel.queue.size() == 0)? 0 : viewModel.currentTrackByPos + 1;
		viewModel.queue.add(viewModel.currentTrackByPos, item);
		musicAdapterCurrentTrack.notifyItemInserted(viewModel.currentTrackByPos);
		playerAlbumArtAdapter.notifyItemInserted(viewModel.currentTrackByPos);
		if (viewModel.queue.size() != 1) {
			scrollTrackOnly = true;
			currentTrackPager.setCurrentItem(viewModel.currentTrackByPos, true);
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


		int itemsToAdd = Math.max(0, 4 - viewModel.queue.size() + viewModel.currentTrackByPos);
		if (itemsToAdd != 0) {
			final Handler handler = new Handler(Looper.getMainLooper());
			handler.post(() -> {
				for (int i = 0; i < itemsToAdd; i++) {
					viewModel.queue.add(viewModel.musicDict.get(new ArrayList<>(viewModel.musicDict.keySet()).get(ThreadLocalRandom.current().nextInt(viewModel.musicDict.size()))));
				}
				musicAdapterCurrentTrack.notifyItemRangeInserted(viewModel.currentTrackByPos + 1, itemsToAdd);
				playerAlbumArtAdapter.notifyItemRangeInserted(viewModel.currentTrackByPos + 1, itemsToAdd);
			});
		}
	}

	public void playPauseToggle(View view) {
		if (isPlaying) {
			mp.pause();
			setPlaying(false);
		} else if (!musicPlayerPrepared) {
			if (viewModel.queue.size() != 0) playSong(viewModel.currentTrackByPos); // intentional call
			else if (viewModel.musicDict.size() != 0) playSong(ThreadLocalRandom.current().nextInt(0, viewModel.musicDict.size())); // unintentional call
		} else {
			mp.start();
			setPlaying(true);
		}
	}

	// outdated
	public void playFromView(View view) {
		int position = ((RecyclerView) view.getParent()).getChildLayoutPosition(view) - 1;
		playSong(position); // unintentional call
	}

	public void openPlayer(View view) {
		if (bottomSheetPlayer.getState() == BottomSheetBehavior.STATE_COLLAPSED) bottomSheetPlayer.setState(BottomSheetBehavior.STATE_EXPANDED);
	}

	public void nextSong(View view) {
		currentTrackPager.setCurrentItem(currentTrackPager.getCurrentItem() + 1, true);
	}

	public void prevSong(View view) {
		if (isPlaying && songPosition >= 5000) playSong(viewModel.currentTrackByPos); // intentional call
		else currentTrackPager.setCurrentItem(Math.max(0, viewModel.currentTrackByPos - 1), true);
	}

	public void hidePlayer(View view) { bottomSheetPlayer.setState(BottomSheetBehavior.STATE_COLLAPSED); }

	public void addToQueue(long id) {
		DialogFragmentAddedToQueue infoDialog = new DialogFragmentAddedToQueue();
		infoDialog.show(getSupportFragmentManager(), infoDialog.getTag());

		viewModel.queue.add(viewModel.currentTrackByPos + 1, viewModel.musicDict.get(id));
		musicAdapterCurrentTrack.notifyItemInserted(viewModel.currentTrackByPos + 1);
		playerAlbumArtAdapter.notifyItemInserted(viewModel.currentTrackByPos + 1);
	}

	public void addToQueue(ArrayList<Long> ids) {
		viewModel.queue.addAll(viewModel.currentTrackByPos + 1, ids.stream().map(key -> viewModel.musicDict.get(key)).collect(Collectors.toList()));
		musicAdapterCurrentTrack.notifyItemRangeInserted(viewModel.currentTrackByPos + 1, ids.size());
		playerAlbumArtAdapter.notifyItemRangeInserted(viewModel.currentTrackByPos + 1, ids.size());

		DialogFragmentAddedToQueue infoDialog = new DialogFragmentAddedToQueue();
		infoDialog.show(getSupportFragmentManager(), infoDialog.getTag());
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
		final int maxWidth = findViewById(R.id.playerAlbumPlaceholder).getMeasuredWidth();
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
		viewModel.songDatabaseDao.updateTracks(new ArrayList<>(viewModel.musicDict.values()));
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

	// method to create SQL Argument Placeholders
	private String makePlaceholders(int len) {
		StringBuilder sb = new StringBuilder(len * 2 - 1);
		sb.append("?");
		for (int i = 1; i < len; i++)
			sb.append(",?");
		return sb.toString();
	}

	@Override
	public boolean onSupportNavigateUp() {
		if (navController.getCurrentDestination().getId() == R.id.fragmentPlaylist) {
			final FragmentPlaylist fragment = (FragmentPlaylist) getSupportFragmentManager().findFragmentById(R.id.fragmentPlaylist);

			if (fragment != null) {
				fragment.savePlaylistData();
			}
		}
		return super.onSupportNavigateUp();
	}
}