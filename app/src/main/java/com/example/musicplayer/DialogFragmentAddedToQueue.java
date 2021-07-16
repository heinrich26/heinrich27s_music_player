package com.example.musicplayer;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DialogFragmentAddedToQueue extends DialogFragment {

	private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NORMAL, R.style.AlphaInfoDialog);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.next_title_info_dialog, container, false);

		Runnable hideDialog = this::dismiss;
		executor.schedule(hideDialog, 3, TimeUnit.SECONDS);

		view.setOnTouchListener((v, event) -> {
			dismiss();
			return false;
		});

		return view;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		return super.onCreateDialog(savedInstanceState);
	}

	@Override
	public void onStart() {
		super.onStart();
		final Dialog dialog = getDialog();
		if (dialog == null) return;

		dialog.getWindow().setWindowAnimations(R.style.SlidingDialogAnimation);
	}
}
