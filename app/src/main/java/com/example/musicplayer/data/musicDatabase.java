package com.example.musicplayer.data;


import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.musicplayer.musicTrack;

@Database(entities = {folderRef.class, ignores.class, musicTrack.class, Playlist.class}, exportSchema = false, version = 11)
public abstract class musicDatabase extends RoomDatabase {
	private static final String DB_NAME = "music_db";
	private static volatile musicDatabase instance;

	public static synchronized musicDatabase getInstance(Context context) {
		if (instance == null) {
			instance = Room.databaseBuilder(context.getApplicationContext(),
					musicDatabase.class,
					DB_NAME)
					.allowMainThreadQueries()
					.fallbackToDestructiveMigration()
					.build();
		}
		return instance;
	}

	public abstract musicDatabaseDao musicDatabaseDao();
}
