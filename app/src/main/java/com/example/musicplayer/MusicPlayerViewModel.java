package com.example.musicplayer;

import android.media.MediaPlayer;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicplayer.data.musicDatabase;
import com.example.musicplayer.data.musicDatabaseDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MusicPlayerViewModel extends ViewModel {

	private MutableLiveData<String> playerTimeRemaining, playerTimeElapsed;

	public MutableLiveData<String> getPlayerTimeRemaining() {
		if (playerTimeRemaining == null) {
			playerTimeRemaining = new MutableLiveData<>();
		}
		return playerTimeRemaining;
	}

	public MutableLiveData<String> getPlayerTimeElapsed() {
		if (playerTimeElapsed == null) {
			playerTimeElapsed = new MutableLiveData<>();
		}
		return playerTimeElapsed;
	}

	public MediaPlayer mediaPlayer;

	public musicDatabase songDatabase;
	public musicDatabaseDao songDatabaseDao;

	public Map<Long, musicTrack> musicDict = Collections.synchronizedMap(new HashMap<>());

	public ArrayList<Long> queue = new ArrayList<>();

	public int currentTrackByPos;

	private MutableLiveData<ArrayList<Long>> addSongsSelection;

	public MutableLiveData<ArrayList<Long>> getAddSongsSelection() {
		if (addSongsSelection == null) {
			addSongsSelection = new MutableLiveData<>();
			addSongsSelection.setValue(new ArrayList<>());
		}
		return addSongsSelection;
	}

	public void addSongToAddSelection(long id) {
		getAddSongsSelection().getValue().add(id);
		addSongsSelection.setValue(addSongsSelection.getValue());
	}

	public void removeSongFromAddSelection(long id) {
		getAddSongsSelection().getValue().remove(id);
		addSongsSelection.setValue(addSongsSelection.getValue());
	}

	private MutableLiveData<Boolean> inAddMode;

	public MutableLiveData<Boolean> getInAddMode() {
		if (inAddMode == null) {
			inAddMode = new MutableLiveData<>();
		}

		return inAddMode;
	}
}
