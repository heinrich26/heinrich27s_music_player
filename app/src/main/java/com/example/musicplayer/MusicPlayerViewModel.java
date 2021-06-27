package com.example.musicplayer;

import android.media.MediaPlayer;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicplayer.data.musicDatabase;
import com.example.musicplayer.data.musicDatabaseDao;

import java.util.ArrayList;

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

	public ArrayList<musicTrack> musicFilesList = new ArrayList<>();

	public ArrayList<musicTrack> queue = new ArrayList<>();

	public SongMenuBottomSheetFragment.SongMenuActions songMenuActions;


	public int currentTrackByPos;
}
