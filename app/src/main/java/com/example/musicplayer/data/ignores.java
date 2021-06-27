package com.example.musicplayer.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "ignores")
public class ignores implements Serializable {
	@NonNull
	@PrimaryKey
	@ColumnInfo(name = "ignoreUri")
	public String ignoreUri;

	public ignores(@NonNull String ignoreUri) {
		this.ignoreUri = ignoreUri;
	}
}
