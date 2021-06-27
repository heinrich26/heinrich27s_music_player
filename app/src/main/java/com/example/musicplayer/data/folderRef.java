package com.example.musicplayer.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;


@Entity(tableName = "folderRef")
public class folderRef implements Serializable {
	@NonNull
	@PrimaryKey
	@ColumnInfo(name = "folderUri")
	public String folderUri;

	public folderRef(@NonNull String folderUri) {
		this.folderUri = folderUri;
	}
}
