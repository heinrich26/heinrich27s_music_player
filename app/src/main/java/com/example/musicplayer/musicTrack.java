package com.example.musicplayer;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "trackList", indices = {@Index(value = {"path"}, unique = true)})
public class musicTrack implements Serializable {
	@NonNull
	@PrimaryKey(autoGenerate = true)
	@ColumnInfo(name = "SongId")
	public long id;

	@ColumnInfo(name = "path")
	public String path;

	@ColumnInfo
	public String title;

	@ColumnInfo
	public String artist;

	@ColumnInfo
	public String album;

	@ColumnInfo
	public int duration;

	@ColumnInfo
	public String absPath;

	@ColumnInfo
	public boolean hasCover;

	@ColumnInfo
	public boolean testedForCover;

	public musicTrack(@NonNull long id, @NonNull String path, String title, String artist, String album, int duration, String absPath, boolean hasCover, boolean testedForCover) {
		this.id = id;
		this.path = path;
		this.title = title;
		this.artist = artist;
		this.album = album;
		this.duration = duration;
		this.absPath = absPath;
		this.hasCover = hasCover;
		this.testedForCover = testedForCover;
	}

	@NonNull
	public String getPath() {
		return path;
	}

	public void setPath(@NonNull String path) {
		this.path = path;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getAlbum() {
		return album;
	}

	public void setAlbum(String album) {
		this.album = album;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public String getAbsPath() {
		return absPath;
	}

	public boolean hasCover() {
		return hasCover;
	}

	public void setCover(boolean hasCover) {
		this.hasCover = hasCover;
	}

	public void setHasCover(boolean hasCover) {
		this.hasCover = hasCover;
	}

	public boolean testedForCover() {
		if (!this.testedForCover) {
			this.testedForCover = true;
			return false;
		} else return true;
	}
}
