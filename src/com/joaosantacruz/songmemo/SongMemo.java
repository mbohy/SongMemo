/*
* SongMemo Project
* @author joaosantacruz.com
* Created on 10/Feb/2011, 23:20:14
*/

package com.joaosantacruz.songmemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Locale;

public class SongMemo extends Activity {

	private static final int NUMBER_OF_TRACKS = 4;
	private static final int RENAME_TRACK = 1;
	private static final int CLEAR_TRACK = 3;

	private Song song;

	private LinearLayout mainBox;
	private Theme theme;

	private EditText lyricsTextBox;
	private String oldLyricsText;

	private TrackWidget[] trackWidgets;

	private boolean isPressingSeekBar = false;
	private SeekBar currentPositionBar;

	private Button playButton;
	private Button lyricsButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		makeWindowFullScreen();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Log.v("JOAO", "APP START - Display UI");

		mainBox = (LinearLayout) findViewById(R.id.MainBox);

		LinearLayout mainLayout = (LinearLayout) findViewById(R.id.MainLayout);
		theme = new Theme(mainLayout);

		song = new Song();

		lyricsTextBox = (EditText) findViewById(R.id.LyricsTextBox);
		registerTextChangedListenerOnLyricsBox(lyricsTextBox);

		trackWidgets = buildTrackWidgets(mainBox);
		registerOnClickTrackWidgetButtons(trackWidgets);
		registerOnCreateContextMenuForTrackWidgets(trackWidgets);

		currentPositionBar = (SeekBar) findViewById(R.id.CurrentPositionBar);
		registerOnSeekBarChange(currentPositionBar);

		Button recButton = (Button) findViewById(R.id.RecBtn);
		registerOnClickRecButton(recButton);

		Button stopButton = (Button) findViewById(R.id.StopBtn);
		registerOnClickStopButton(stopButton, recButton);

		playButton = (Button) findViewById(R.id.PlayBtn);
		registerOnClickPlayButton(playButton);

		lyricsButton = (Button) findViewById(R.id.LyricsBtn);
		registerOnClickLyricsButton(lyricsButton);

		registerContextMenuForSongTitle();

		updateGUIState(trackWidgets, song, lyricsTextBox);
	}

	private void registerContextMenuForSongTitle() {
		TextView songTitleLabel = (TextView) findViewById(R.id.SongTitleLabel);
		registerForContextMenu(songTitleLabel);
	}

	private void registerOnCreateContextMenuForTrackWidgets(TrackWidget[] trackWidgets) {
		for (int i = 0; i < trackWidgets.length; i++)
			trackWidgets[i].label().setOnCreateContextMenuListener(new TrackLabelOnCreateContextMenuListener(i));
	}

	private void registerTextChangedListenerOnLyricsBox(final EditText lyricsTextBox) {
		lyricsTextBox.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence text, int start, int before, int after) {
				song.setLyricsText(lyricsTextBox.getText().toString());
				song.saveSettings();
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});
	}

	private void registerOnClickTrackWidgetButtons(TrackWidget[] trackWidgets) {
		for (int i = 0; i < trackWidgets.length; i++) {
			TrackWidget widget = trackWidgets[i];

			widget.muteButtonSelect().setOnClickListener(new SentinelaOnClick(i, "muteBtnSelect"));
			widget.recordButtonSelect().setOnClickListener(new SentinelaOnClick(i, "recBtnSelect"));

			widget.faderButtonUp().setOnClickListener(new SentinelaOnClick(i, "faderBtnUp"));
			widget.faderButtonDown().setOnClickListener(new SentinelaOnClick(i, "faderBtnDown"));

			widget.panBar().setOnSeekBarChangeListener(new SentinelaOnClick(i, "panBar"));
		}
	}

	private void registerOnClickLyricsButton(Button lyricsButton) {
		lyricsButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				lyricsTextBox.setText(song.getLyricsText());

				if (mainBox.getVisibility() == View.VISIBLE) {
					oldLyricsText = song.getLyricsText();
					mainBox.setVisibility(View.GONE);
					lyricsTextBox.setVisibility(View.VISIBLE);
				} else {
					Log.v("JOAO", "ESCONDE LYRICS    \n\r" + oldLyricsText + "\n\r" + song.getLyricsText());
					if (!oldLyricsText.equals(song.getLyricsText())) {
						Toast mToast = Toast.makeText(getApplicationContext(), "Saving lyrics...", Toast.LENGTH_SHORT);
				    	mToast.show();
						song.saveLyrics(song.getLyricsText());
					}
					mainBox.setVisibility(View.VISIBLE);
					lyricsTextBox.setVisibility(View.GONE);
				}
			}
		});
	}

	private void registerOnClickPlayButton(final Button playButton) {
		playButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				playButton.setSelected(song.songPlay(currentPositionBar.getProgress()));
				if (song.isPlayingSong() && !song.isRecordingSong()) {
					new Thread(new Runnable() {
						public void run() {
							dealWithPlay(isPressingSeekBar);
						}
					}).start();
				}

				if (!song.isPlayingSong() && !song.isRecordingSong()) {
					Toast mToast = Toast.makeText(getApplicationContext(), "Nothing to play. All tracks are empty!", Toast.LENGTH_SHORT);
			    	mToast.show();
				}
			}
		});
	}

	private void registerOnClickRecButton(final Button recButton) {
		recButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				try {
					recButton.setSelected(song.songRecord());
					if (!song.isRecordingSong() && !song.isPlayingSong()) {
						Toast mToast = Toast.makeText(getApplicationContext(), "No track selected to record!", Toast.LENGTH_SHORT);
				    	mToast.show();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private void registerOnClickStopButton(final Button stopButton, final Button recButton) {
		stopButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				try {
					stopButton.setSelected(song.songStop());
					recButton.setSelected(false);
					playButton.setSelected(false);
					lyricsButton.setSelected(false);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private void registerOnSeekBarChange(final SeekBar currentPositionBar) {
		currentPositionBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (isPressingSeekBar)
					currentPositionBar.setProgress(song.setPlayPosition(currentPositionBar.getProgress()));
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				isPressingSeekBar = true;
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				isPressingSeekBar = false;
			}
		});
	}

	private TrackWidget[] buildTrackWidgets(LinearLayout mainBox) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		TrackWidget[] trackWidgets = new TrackWidget[NUMBER_OF_TRACKS];

		for (int i = 0; i < trackWidgets.length; i++) {
			RelativeLayout track = (RelativeLayout) inflater.inflate(R.layout.track, mainBox, false);
			TrackWidget trackWidget = buildTrackWidget(track);
			trackWidgets[i] = trackWidget;
			mainBox.addView(track);
		}

		return trackWidgets;
	}

	private void makeWindowFullScreen() {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}

	private TrackWidget buildTrackWidget(RelativeLayout trackLayout) {
        TextView label = (TextView) trackLayout.getChildAt(0);
		Button muteButtonSelect = (Button) trackLayout.getChildAt(1);
		Button recordButtonSelect = (Button) trackLayout.getChildAt(2);
		SeekBar panBar = (SeekBar) trackLayout.getChildAt(3);

		RelativeLayout faderLayout = (RelativeLayout) trackLayout.getChildAt(4);

        Button faderButtonUp  = (Button) faderLayout.getChildAt(0);
        TextView faderButtonText = (TextView) faderLayout.getChildAt(1);
        Button faderButtonDown = (Button) faderLayout.getChildAt(2);

		return new TrackWidget(label, faderButtonUp, faderButtonDown, faderButtonText, panBar, muteButtonSelect, recordButtonSelect);
	}

	private void updateGUIState(TrackWidget[] trackWidgets, Song song, EditText lyricsTextBox) {
		for (int i = 0; i < trackWidgets.length; i++) {
			TrackWidget widget = trackWidgets[i];

			widget.label().setText(song.tracks.get(i).getTrackName());
			widget.muteButtonSelect().setSelected(song.tracks.get(i).isMuted());
			widget.recordButtonSelect().setSelected(song.tracks.get(i).isRecordable());

			widget.faderButtonText().setText(String.format(Locale.getDefault(), "%d", song.tracks.get(i).getLeftVolume()));
			widget.panBar().setProgress(song.tracks.get(i).getBalance());
		}

		song.saveSettings();

		lyricsTextBox.setText(song.getLyricsText());

		theme.restore();
	}

	private void dealWithPlay(boolean isPressingSeekBar) {

		int maxPosition = song.maxTrackDuration("track_duration");

		int curPosition = song.tracks.get(song.maxTrackDuration("track_number")).mediaPlayer.getCurrentPosition();
		SeekBar currentPositionBar = (SeekBar) findViewById(R.id.CurrentPositionBar);
		song.setPlayPosition(currentPositionBar.getProgress());

		while ((curPosition + 20) < maxPosition && maxPosition > 0) {
			if (!isPressingSeekBar) {
				curPosition = song.tracks.get(song.maxTrackDuration("track_number")).mediaPlayer.getCurrentPosition();
				currentPositionBar.setProgress(curPosition * 100 / maxPosition);

				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} 
			}

			if (!song.tracks.get(song.maxTrackDuration("track_number")).mediaPlayer.isPlaying())
				break;
		}

		song.songStop();
		currentPositionBar.setProgress(0);

		Message msg = handler.obtainMessage();
		handler.sendMessage(msg);
	}

	private final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			playButton.setSelected(false);
		}
	};

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

		super.onCreateContextMenu(menu, v, menuInfo);
		
		// Change App Background -- - - - -- - - - - - - - - -
		if (v.getId() == R.id.SongTitleLabel) {
			theme.moveToNext();

		}
		// - - - - - - - - - - - - - - - - - - - - - - - - - - -

	}

	@Override
	public boolean onContextItemSelected(final MenuItem item) {

		switch (item.getOrder()) {

		case RENAME_TRACK:
			Log.v("JOAO", "CONTEXT MENU - RENAME track + + + + + + ");

			final AlertDialog.Builder alert = new AlertDialog.Builder(this);
			final EditText input = new EditText(this);

			alert.setView(input);
			alert.setTitle("Insert a new track name:");
			input.requestFocus();

			alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int optButton) {
					String trackName = input.getText().toString().trim();
					trackWidgets[item.getGroupId()].label().setText(song.renameTrack(trackName, item.getGroupId()));
				}
			});

			alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int optButton) {
					dialog.cancel();
				}
			});

			alert.show();
			break;

		case CLEAR_TRACK:
			Log.v("JOAO", "CONTEXT MENU - CLEAR track + + + + + + ");
			song.clearTrack(item.getGroupId());

			break;
		}

		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		new MenuInflater(getApplication()).inflate(R.menu.menu, menu);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

			case R.id.newmenubtn:
				handleNewSong();
				break;

			case R.id.openmenubtn:
				handleOpenSong(item);
				break;

			case R.id.deletemenubtn:
				handleDeleteSong();
				break;

			case R.id.saveas:
				handleSaveSongAs();
				break;

			case R.id.quitmenubtn:
				handleQuit();
				break;

			case R.id.aboutmenubtn:
				handleAboutApp();
				break;
		}

		if (item.getItemId() == 4444) {
			TextView songTitleLabel  = (TextView) findViewById(R.id.SongTitleLabel);
			songTitleLabel.setText(song.openSong((String) item.getTitle()));
			updateGUIState(trackWidgets, song, lyricsTextBox);
		}

		return super.onOptionsItemSelected(item);
	}

	private void handleAboutApp() {
		final AlertDialog.Builder alertAbout = new AlertDialog.Builder(this);
		alertAbout.setTitle("SongMemo's message");
		alertAbout.setIcon(android.R.drawable.ic_menu_info_details);
		alertAbout.setMessage(
				"As a musician and software developer, I've committed myself to " +
				"build a multi-track audio recorder (4 track like) for the " +
				"Android platform.\n\n" +

				"Openness brings us fast access to knowledge and, as a consequence, " +
				"better solutions. That is why SongMemo is a Free and Living Open " +
				"Source project.\n\n" +
				"Contributors are welcome!\n\n" +
				"If you have ideas that you wish to see implemented or if you somehow " +
				"want to be a part of SongMemo's developer team, feel free to contact me.\n\n" +
				"Further info, please visit:\nwww.joasantacruz.com/songmemo"
		);
		alertAbout.setNegativeButton("Close", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int optButton) {
				dialog.cancel();
			}
		});

		alertAbout.show();
	}

	private void handleQuit() {
		Toast.makeText(getApplicationContext(), "Saving SongMemo's settings...\nSee you next time!", Toast.LENGTH_SHORT).show();
		terminate();
	}

	private void handleSaveSongAs() {
		final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
		final EditText input = new EditText(this);

		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		// only will trigger it if no physical keyboard is open
		inputMethodManager.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);

		alertBuilder.setView(input);
		alertBuilder.setTitle("Save song as:");

		alertBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int optButton) {
				String songName = input.getText().toString().trim();
				final String msg = song.renameSong(songName);
				TextView songTitleLabel = (TextView) findViewById(R.id.SongTitleLabel);
				songTitleLabel.setText(songName);
				Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
			}
		});

		alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int optButton) {
				dialog.cancel();
			}
		});

		alertBuilder.show();
	}

	private void handleDeleteSong() {
		final AlertDialog.Builder alertDelete = new AlertDialog.Builder(this);
		alertDelete.setIcon(android.R.drawable.ic_dialog_alert);

		alertDelete.setTitle("Delete Song");
		alertDelete.setMessage("Are you sure you want to delete this song and all of its tracks?");

		alertDelete.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int optButton) {
				if (song.deleteSong()) {
					TextView songTitleLabel = (TextView) findViewById(R.id.SongTitleLabel);
					songTitleLabel.setText(song.songName);
					Toast.makeText(getApplicationContext(), "Song deleted", Toast.LENGTH_SHORT).show();
				}
			}
		});

		alertDelete.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int optButton) {
				dialog.cancel();
			}
		});

		alertDelete.show();
	}

	private void handleOpenSong(MenuItem item) {
		File[] listOfSongs = song.getSongsList();
		item.getSubMenu().clear();
		for (File song : listOfSongs) {
			if (song.isDirectory()) {
				item.getSubMenu().add(99, 4444, 0, song.getName());
			}
		}
	}

	private void handleNewSong() {
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		final EditText input = new EditText(this);

		InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		// only will trigger it if no physical keyboard is open
		mgr.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);

		alert.setView(input);
		alert.setTitle("Please name your song:");

		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int optButton) {
				String songName = input.getText().toString().trim();
				final String msg = song.newSong(songName);
				TextView songTitleLabel = (TextView) findViewById(R.id.SongTitleLabel);
				songTitleLabel.setText(songName);
				Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int optButton) {
				dialog.cancel();
			}
		});

		alert.show();
	}

	public void terminate() {
		Log.i("myid","terminated!!");
		super.onDestroy();
		this.finish();
	}

	private class SentinelaOnClick implements View.OnClickListener, OnSeekBarChangeListener {
		private int i;
		private String element = "";
		private Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		SentinelaOnClick(int i, String element) {
			super();
			this.i = i;
			this.element = element;
		}

		@Override
		public void onClick(View v) {
			Log.v("JOAO", "CLICK - - - -  " + element + " - - track-" + i);
			
			if (element == "muteBtnSelect") {
				trackWidgets[i].muteButtonSelect().setSelected(song.toggleMute(i));
				vib.vibrate(25);
			}

			if (element == "recBtnSelect"){
				trackWidgets[i].recordButtonSelect().setSelected(song.toggleRecordable(i));
				vib.vibrate(25);
			}

			if (element == "faderBtnUp") {
				trackWidgets[i].faderButtonText().setText(String.format(Locale.getDefault(), "%d", song.volumeUp(i)));
			}

			if (element == "faderBtnDown"){
				trackWidgets[i].faderButtonText().setText(String.format(Locale.getDefault(), "%d", song.volumeDown(i)));
			}

			updateGUIState(trackWidgets, song, lyricsTextBox);
		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			Log.v("JOAO", "SET Balance " + i + " - " + trackWidgets[i].panBar().getProgress());
			song.setBalance(i, trackWidgets[i].panBar().getProgress());
			updateGUIState(trackWidgets, song, lyricsTextBox);
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
		}
	}

	private class TrackWidget {
		private final TextView label;
		private final Button faderButtonUp;
		private final Button faderButtonDown;
		private final TextView faderButtonText;
		private final SeekBar panBar;
		private final Button muteButtonSelect;
		private final Button recordButtonSelect;

		TrackWidget(TextView label, Button faderButtonUp, Button faderButtonDown, TextView faderButtonText,
					SeekBar panBar, Button muteButtonSelect, Button recordButtonSelect) {

			this.label = label;
			this.faderButtonUp = faderButtonUp;
			this.faderButtonDown = faderButtonDown;
			this.faderButtonText = faderButtonText;
			this.panBar = panBar;
			this.muteButtonSelect = muteButtonSelect;
			this.recordButtonSelect = recordButtonSelect;
		}

		TextView label() {
			return label;
		}

		Button faderButtonUp() {
			return faderButtonUp;
		}

		Button faderButtonDown() {
			return faderButtonDown;
		}

		TextView faderButtonText() {
			return faderButtonText;
		}

		SeekBar panBar() {
			return panBar;
		}

		Button muteButtonSelect() {
			return muteButtonSelect;
		}

		Button recordButtonSelect() {
			return recordButtonSelect;
		}
	}

	private class TrackLabelOnCreateContextMenuListener implements View.OnCreateContextMenuListener {
		private final int trackNumber;

		TrackLabelOnCreateContextMenuListener(int trackNumber) {
			this.trackNumber = trackNumber;
		}

		@Override
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
			Log.d(SongMemo.class.getSimpleName(), "Creating context menu for " + ((TextView) v).getText());

			menu.setHeaderTitle(String.format("Edit '%s'", trackWidgets[trackNumber].label.getText()));
			menu.add(trackNumber, v.getId(), RENAME_TRACK, "Rename track");
			menu.add(trackNumber, v.getId(), CLEAR_TRACK, "Clear track");
		}
	}

	private class Theme {

		private static final String PREFS_FILE_NAME = "myPrefsFile";
		private static final String SELECTED_BG_PREF = "selectedBg_pref";

		private final int[] drawableBg = {
				R.drawable.bg_01, R.drawable.bg_02, R.drawable.bg_03,
				R.drawable.bg_04, R.drawable.bg_07, R.drawable.bg_08,
				R.drawable.bg_06, R.drawable.bg_09, R.drawable.bg_10,
				R.drawable.bg_11, R.drawable.bg_12 };

		private final LinearLayout mainLayout;
		private int selectedBg = 0;

		Theme(LinearLayout mainLayout) {
            this.mainLayout = mainLayout;
		}

		private void restore() {
			SharedPreferences settings = getSharedPreferences(PREFS_FILE_NAME, 0);
			selectedBg = settings.getInt(SELECTED_BG_PREF, 0);

			update(selectedBg);
		}

		void moveToNext() {
			if ((selectedBg + 1) >= drawableBg.length)
				selectedBg = 0;
			else
				selectedBg++;

			update(selectedBg);
			save();
		}

		private void update(int selectedBg) {
			mainLayout.setBackgroundDrawable(getResources().getDrawable(drawableBg[selectedBg]));
		}

		private void save() {
			SharedPreferences settings = getSharedPreferences(PREFS_FILE_NAME, 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putInt(SELECTED_BG_PREF, selectedBg);
			editor.commit();
		}
	}
}
