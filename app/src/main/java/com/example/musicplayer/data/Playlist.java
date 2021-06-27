package com.example.musicplayer.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.io.Serializable;
import java.util.ArrayList;

@Entity(tableName = "Playlists")
public class Playlist implements Serializable {
	@PrimaryKey(autoGenerate = true)
	@ColumnInfo(name = "id")
	public int id;

	@ColumnInfo
	public String name;

	@ColumnInfo
	public String description;

	@TypeConverters({Converters.class})
	@ColumnInfo
	public ArrayList<Integer> songs;

	public Playlist(int id, String name, String description) {
		this.id = id;
		this.name = name;
		this.description = description;
	}
}

