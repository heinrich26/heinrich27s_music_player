package com.example.musicplayer.data;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class Converters {
	@TypeConverter
	public static ArrayList<Integer> fromString(String value) {
		Type list = new TypeToken<ArrayList<Integer>>() {}.getType();
		return new Gson().fromJson(value, list);
	}

	@TypeConverter
	public static String fromArrayList(ArrayList<Integer> list) {
		String json = new Gson().toJson(list);
		return json;
	}
}
