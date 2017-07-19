/*
* SoundMemo Project
* @author joaosantacruz.com
* Created on 10/Feb/2011, 23:20:14
*/

package com.joaosantacruz.songmemo;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Song {

	private static final String DEFAULT_SONG = "UntitledSong";
	private static final String APPDIR = Environment.getExternalStorageDirectory().getPath() + "/SongMemo/";
	private static final String LYRICS_FILE = "lyrics.html";
	private static final String SETTINGS_FILE = "settings.txt";

	private boolean isSongOpened = false;
	
	private boolean isRecordingSong = false;
	private boolean isPlayingSong = false;

	String songName = DEFAULT_SONG;
	private String lyricsText = "";
	
	ArrayList<Track> tracks = new ArrayList<>();

	public Song() {
		touchDirectory(APPDIR);
		openSong(songName);
	}

	/*
	* This method creates a new song and open it 
	* @author joaosantacruz.com
	*/
	String newSong(String songname) {
		
		String msg;
		
		if (touchDirectory(APPDIR + songname)) {
			msg = "The song '" + songname + "! Please insert another name.";
			openSong(songname);
		} else {
			msg = "The song '" + songname + "' was successfully created.";
			openSong(songname);
		}
		
		return msg;
	}

	/*
	* This method Opens song and Initialize it 
	* @author joaosantacruz.com
	*/
	String openSong(String songname) {

		if (isSongOpened)
			closeSong();

		this.songName = songname;
		touchDirectory(APPDIR + songName);
		touchFile(APPDIR + songName + "/" + LYRICS_FILE);
		touchFile(APPDIR + songName + "/" + SETTINGS_FILE);
		
		tracks.add(new Track(this.songName, "01"));
		tracks.add(new Track(this.songName, "02"));
		tracks.add(new Track(this.songName, "03"));
		tracks.add(new Track(this.songName, "04"));

		try {
			openSettings();
			setLyricsText(openLyrics());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		isSongOpened = true;

		return songName;
	}

	/*
	* This method renames song (file)
	* @author joaosantacruz.com
	*/
	String renameSong(String songname) {
		
		String actualSongName = songName;
		closeSong();

		Log.v("JOAO", "Rename SONG '" + actualSongName + " to " + songname + "' = = = = = = ");
		File orig_dir = new File(APPDIR + actualSongName);
		File dest_dir = new File(APPDIR + songname);
		orig_dir.renameTo(dest_dir);

		return openSong(actualSongName);
	}

	/*
	* This method closes song
	* @author joaosantacruz.com
	*/
	private void closeSong() {
		
		saveSettings();
		saveLyrics(getLyricsText());

		isSongOpened = false;
		tracks.clear();

		Log.v("JOAO", "CLOSE SONG '" + songName + "' = = = = = = ");
	}

	boolean deleteSong() {
		closeSong();
		File f = new File(APPDIR + songName);
		deleteDirectory(f);
		Log.v("JOAO", "DELETE SONG '" + songName + "' = = = = = = ");
		openSong(DEFAULT_SONG);

		return true;
	}

	boolean clearTrack(int trackNumber) {
		File f = new File(tracks.get(trackNumber).getTrackPath());
		if (f.exists()) {
			f.delete();

			try {
				tracks.get(trackNumber).createTrackFile();
			} catch (IOException e) {
				e.printStackTrace();
			}

			tracks.get(trackNumber).setRecorded(false);
			Log.v("JOAO", "CLEAR Track '" + tracks.get(trackNumber).getTrackPath() + "' = = = = = = ");

			return true;
		}

		return false;
	}

	private boolean deleteDirectory(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for (File dir : files) {
				if (dir.isDirectory()) {
					deleteDirectory(dir);
				} else {
					dir.delete();
				}
			}
		}

		return path.delete();
	}


	/* Toggle individual track mute button */
	boolean toggleMute(int TrackNumber) {

		if (tracks.get(TrackNumber).isMuted()) {
			tracks.get(TrackNumber).setMuted(false);
		} else {
			tracks.get(TrackNumber).setMuted(true);
		}

		return tracks.get(TrackNumber).isMuted();
	}

	/* Toggle individual track record button */
	boolean toggleRecordable(int TrackNumber) {
		
		if (!isRecordingSong) {
			if (tracks.get(TrackNumber).isRecordable()) {
				tracks.get(TrackNumber).setRecordable(false);
			} else {
				setAllTracksRecordable(false);
				tracks.get(TrackNumber).setRecordable(true);
			}
		}

		return tracks.get(TrackNumber).isRecordable();
	}

	private void setAllTracksRecordable(boolean value) {
        for (Track track : tracks) {
            track.setRecordable(value);
        }
	}
	
	/* Stop Song Recording/Play */
	boolean songStop() {
		for (Track track : tracks) {
			if (track.isPlaying()) {
				track.stopPlaying();
				isPlayingSong = false;
			}
		}

		for (Track track : tracks) {
			if (track.isRecording()) {
				track.stopRecording();
				isRecordingSong = false;
				track.setRecorded(true);
			}
		}

		return isPlayingSong && isRecordingSong;
	}

	/* Start Song Recording  */
	boolean songRecord() {
		if (!isRecordingSong && !isPlayingSong) {

			for (Track track : tracks) {
				if (track.isRecordable()) {
					for (Track track2 : tracks) {
						if (!track2.isRecordable() && track2.isRecorded()) {
							track2.startPlaying();
						}
					}

					track.startRecording();
					isRecordingSong = true;
				}
			}
	
			if (!isRecordingSong) {
				Log.v("JOAO", "Song - No track selected to record!");
			}
		}

		return isRecordingSong;
	}

	/* Play Song */
	boolean songPlay(int currentPositionBarValue) {
		Log.v("JOAO", "SONG-pos-Bar: " + currentPositionBarValue );
		if (!isRecordingSong && !isPlayingSong) {

			for (Track track : tracks) {
				if (track.isRecorded()) {
					track.startPlaying();
					isPlayingSong 	= true;
				}
			}
		}
		
		return isPlayingSong;
	}

	void saveLyrics(String lyrics_text) {
		File f = new File(APPDIR + songName + "/" + LYRICS_FILE);
		Log.v("JOAO", "SAVES LYRICS '" + lyrics_text + "' to " + APPDIR + songName + "/" + LYRICS_FILE);
		FileWriter fw;
		try {
			fw = new FileWriter(f);
			BufferedWriter out = new BufferedWriter(fw);
			out.write(lyrics_text);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	String getLyricsText() {
		return lyricsText;
	}
	void setLyricsText(String lyricsText) {
		this.lyricsText = lyricsText;
	}

	// Read Lyrics
	private String openLyrics() throws Exception {

		File f = new File(APPDIR + songName + "/" + LYRICS_FILE);
		FileReader fr = new FileReader(APPDIR + songName + "/" + LYRICS_FILE);
		BufferedReader br = new BufferedReader(fr);

		this.lyricsText = "";

		if (f.exists() && f.length() > 1) {
			String line;
			while ((line = br.readLine()) != null) {
				this.lyricsText = line;
			}

			fr.close();
		}

		Log.v("JOAO", "OPEN LYRISCS '" + lyricsText + "' from " + APPDIR + songName + "/" + LYRICS_FILE);

		return lyricsText;
	}

	private boolean touchDirectory(String dirname) {
		File f = new File(dirname);
		if (f.exists()) {
			return true;
		} else {
			f.mkdirs();
			return false;
		}
	}

	private boolean touchFile(String filename) {
		File f = new File(filename);
		if (f.exists()) {
			return true;
		} else {
			try {
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}
	}

	File[] getSongsList() {
		File d = new File(APPDIR);
		return d.listFiles();
	}

	int volumeUp(int TrackNumber) {
		if (tracks.get(TrackNumber).getLeftVolume() < 10) {
			tracks.get(TrackNumber).setLeftVolume(tracks.get(TrackNumber).getLeftVolume() + 1);
			tracks.get(TrackNumber).setRightVolume(tracks.get(TrackNumber).getRightVolume() + 1);
			tracks.get(TrackNumber).setVolume();
		}

		saveSettings();

		return tracks.get(TrackNumber).getLeftVolume();
	}

	int volumeDown(int TrackNumber) {

		if (tracks.get(TrackNumber).getLeftVolume() >= 1) {
			tracks.get(TrackNumber).setLeftVolume(tracks.get(TrackNumber).getLeftVolume() - 1);
			tracks.get(TrackNumber).setRightVolume(tracks.get(TrackNumber).getRightVolume() - 1);
		}

		tracks.get(TrackNumber).setVolume();
		saveSettings();

		return tracks.get(TrackNumber).getLeftVolume();
	}
	
	String renameTrack(String trackName, int trackNumber) {
		tracks.get(trackNumber).setTrackName(trackName);
		saveSettings();

		return trackName;
	}

	int maxTrackDuration(String typeOrindex) {

		int maxTrackDuration = 0;
		int maxTrackNumber = 0;
		int i = 0;
		
		for (Track track : tracks) {
			if (track.isRecorded() && !track.isRecording()) {
				if (track.mediaPlayer != null) {
					if (track.mediaPlayer.getDuration()>maxTrackDuration) {
						maxTrackDuration = track.mediaPlayer.getDuration();
						maxTrackNumber = i;
					}
				}
			}

			i++;
		}
		
		if (typeOrindex == "track_number") {
			return maxTrackNumber;
		} else {
			return maxTrackDuration;
		}
		
	}
	
	int setPlayPosition(int newPosition) {
		if (isPlayingSong) {
			for (Track track : tracks)
				track.mediaPlayer.seekTo(newPosition * maxTrackDuration("track_duration") / 100);
		}

		return newPosition;
	}


	void saveSettings() {

		String sep = ";";
		String songSettings = "";

		for (Track track : tracks) {
			songSettings += track.getTrackName();
			songSettings += sep + track.getTrackPath();
			songSettings += sep + track.getTrackNumber();
			songSettings += sep + track.getSongName();
			songSettings += sep + track.getLastUpdate();

			songSettings += sep + track.isRecording();
			songSettings += sep + track.isPlaying();
			songSettings += sep + track.isMuted();
			songSettings += sep + track.isRecordable();
			songSettings += sep + track.isRecorded();
			songSettings += sep + track.getBalance();

			songSettings += sep + track.getLeftVolume();
			songSettings += sep + track.getRightVolume() + "\r\n";
		}

		File f = new File(APPDIR + songName + "/" + SETTINGS_FILE);
		try {
			FileWriter fw = new FileWriter(f);
			BufferedWriter out = new BufferedWriter(fw);
			out.write(songSettings);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Read settings
	private void openSettings() throws Exception {

		File f = new File(APPDIR + songName + "/" + SETTINGS_FILE);
		FileReader fr = new FileReader(APPDIR + songName + "/" + SETTINGS_FILE);
		BufferedReader br = new BufferedReader(fr);

		if (f.exists() && f.length() > 1) {

			String line;
			int lineIndex = 0;

			String songSettings[];

			while ((line = br.readLine()) != null) {

				songSettings = line.split(";");

				tracks.get(lineIndex).setTrackName(songSettings[0]);
				tracks.get(lineIndex).setTrackPath(songSettings[1]);
				tracks.get(lineIndex).setTrackNumber(songSettings[2]);
				tracks.get(lineIndex).setSongName(songSettings[3]);
				tracks.get(lineIndex).setLastUpdate(songSettings[4]);

				tracks.get(lineIndex).setRecording(Boolean.parseBoolean(songSettings[5]));
				tracks.get(lineIndex).setPlaying(Boolean.parseBoolean(songSettings[6]));
				tracks.get(lineIndex).setMuted(Boolean.parseBoolean(songSettings[7]));
				tracks.get(lineIndex).setRecordable(Boolean.parseBoolean(songSettings[8]));
				tracks.get(lineIndex).setRecorded(Boolean.parseBoolean(songSettings[9]));
				tracks.get(lineIndex).setBalance(Integer.parseInt(songSettings[10]));

				tracks.get(lineIndex).setLeftVolume(Integer.parseInt(songSettings[11]));
				tracks.get(lineIndex).setRightVolume(Integer.parseInt(songSettings[12]));

				lineIndex++;
			}

			fr.close();

			Log.v("JOAO", "All tracks configuration was loaded from " + APPDIR + songName + "/" + SETTINGS_FILE);
		}

	}

	boolean isRecordingSong() {
		return isRecordingSong;
	}
	boolean isPlayingSong() {
		return isPlayingSong;
	}
	void setBalance(int i, int progress) {
		tracks.get(i).setBalance(progress);
	}
}
