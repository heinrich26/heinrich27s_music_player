package com.example.musicplayer;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicplayer.data.musicDatabase;
import com.example.musicplayer.data.musicDatabaseDao;

import java.util.ArrayList;
import java.util.HashMap;

public class AddSongsViewModel extends ViewModel {

	public musicDatabase songDatabase;
	public musicDatabaseDao songDatabaseDao;

	public HashMap<Long, musicTrack> musicDict = new HashMap<>();

	private final MutableLiveData<ArrayList<Long>> addSongsSelection = new MutableLiveData<>();

	public MutableLiveData<ArrayList<Long>> getAddSongsSelection() {
		return addSongsSelection;
	}

	public void addSongToAddSelection(long id) {
		addSongsSelection.getValue().add(id);
		addSongsSelection.setValue(addSongsSelection.getValue());
	}

	public void removeSongFromAddSelection(long id) {
		addSongsSelection.getValue().remove(id);
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
