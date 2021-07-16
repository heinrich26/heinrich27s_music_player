package com.example.musicplayer.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.musicplayer.musicTrack;

import java.util.HashMap;
import java.util.List;

import static androidx.room.OnConflictStrategy.IGNORE;
import static androidx.room.OnConflictStrategy.REPLACE;

@Dao
public interface musicDatabaseDao {
	@Query("Select * from folderRef")
	List<folderRef> getMusicDatabaseList();

	@Query("Select folderUri from folderRef")
	List<String> getFolderUris();

	@Query("Select ignoreUri from ignores")
	List<String> getIgnoredUris();

	// returns the musicTrack instances
	@Query("Select * from trackList order by title ASC")
	List<musicTrack> getMusicTracks();

	default HashMap<Long, musicTrack> getMusicDict() {
		HashMap<Long, musicTrack> map = new HashMap<>();
		for (musicTrack track : getMusicTracks()) {
			if (map.put(track.id, track) != null) {
				throw new IllegalStateException("Duplicate key");
			}
		}
		return map;
	}

	@Query("Select * from trackList WHERE hasCover")
	List<musicTrack> getTracksWithCover();

	@Query("Select * from trackList WHERE SongId=:id")
	musicTrack getTrack(long id);


	@Query("Select * from playlists order by name")
	List<Playlist> getPlaylists();


	// Tracks
	@Insert(onConflict = IGNORE, entity = musicTrack.class)
	void addTrack(musicTrack track);

	@Delete
	void deleteTrack(musicTrack track);

	@Update(entity = musicTrack.class)
	void updateTrack(musicTrack track);

	@Update()
	void updateTracks(List<musicTrack> tracks);

	// Ignores
	@Insert(onConflict = IGNORE)
	void addIgnore(ignores ignores);

	@Delete
	void deleteIgnore(ignores ignores);

	// Folders
	@Insert(onConflict = REPLACE)
	void insertFolder(folderRef folderRef);

	@Update
	void updateFolder(folderRef folderRef);

	@Delete
	void deleteFolder(folderRef folderRef);

	default boolean exists(String folder) {
		return getFolderUris().contains(folder);
	}

	// Playlists
	@Insert(onConflict = REPLACE)
	void addPlaylist(Playlist playlist);

	@Update
	void updatePlaylist(Playlist playlist);

	@Delete
	void deletePlaylist(Playlist playlist);
}
