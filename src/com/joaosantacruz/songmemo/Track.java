/*
* SoundMemo Project
* @author joaosantacruz.com
* Created on 10/Feb/2011, 23:20:14
*/

package com.joaosantacruz.songmemo;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Track {

	private static final String APPDIR = Environment.getExternalStorageDirectory().getPath() + "/SongMemo/";
	private static final String PREFIX = "track_";
	private static final String EXTENSION = ".3gpp";

	private boolean isRecording = false;
	private boolean isPlaying = false;
	private boolean isMuted = false;
	private boolean isRecordable = false;
	private boolean isRecorded = false;

	private int leftVolume = 9;
	private int rightVolume = 9;
	private int balance = 50;

	private String trackName = "";
	private String trackPath = "";
	private String trackNumber = "";
	private String songName = "";
	private String lastUpdate = "";

	MediaPlayer mediaPlayer = new MediaPlayer();
	private MediaRecorder mediaRecorder = new MediaRecorder();

	public Track(String songname, String tracknumber) {
		
		try {
			setTrackName(PREFIX + tracknumber);
			setSongName(songname);

			setTrackPath(createTrackFile());
			setTrackNumber(tracknumber);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	* This method starts to record individual track 
	* @author joaosantacruz.com
	*/
	boolean startRecording() {

		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); // DEFAULT - MPEG_4 - RAW_AMR - THREE_GPP	
		mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		mediaRecorder.setOutputFile(trackPath);

		try {
			mediaRecorder.prepare();
		} catch (IOException e) {
			e.printStackTrace();
		}
		mediaRecorder.start();

		setLastUpdate(getDateTime());
		
		Log.v("JOAO", "TRACK - RECORD Track-" + trackNumber);
		
		isRecording = true;
		
		return true;
	}

	/*
	* This method stops individual track from recording
	* @author joaosantacruz.com
	*/
	boolean stopRecording() {
		Log.v("JOAO", "TRACK - STOP record track "+ trackNumber);
		mediaRecorder.stop();
		isRecording = false;

		return false;
	}

	/*
	* This method starts to play individual track 
	* @author joaosantacruz.com
	*/
	boolean startPlaying() {

		mediaPlayer.reset();

		try {
			mediaPlayer.setDataSource(trackPath);
			mediaPlayer.prepare();
		} catch (IOException e) {
			Log.v(("Song MEMO"), e.getMessage());
		}

		mediaPlayer.start();
		setVolume();
		mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

			public void onCompletion(MediaPlayer arg0) {
				mediaPlayer.seekTo(0);
			}

		});

		isPlaying = true;

		return true;
	}

	/*
	* This method stops individual track from playing
	* @author joaosantacruz.com
	*/
	boolean stopPlaying() {
		Log.v("JOAO", "TRACK - STOP play track " + trackNumber);

		try {
			mediaPlayer.reset();
			mediaPlayer.setDataSource(trackPath);
			mediaPlayer.prepare();
			
		} catch (IOException e) {
			Log.v(("Song MEMO"), e.getMessage());
		}

		setPlaying(false);

		return isPlaying;
	}

	
	/*
	* This method mutes individual track 
	* @author joaosantacruz.com
	*/
	void setMuted(boolean isMuted) {
		this.isMuted = isMuted;

		if (isMuted) {
			mediaPlayer.setVolume(0, 0);
		} else {
			setVolume();
		}
	}

	/*
	* This method get date-time using format 'yyyy-MM-dd HH:mm:ss'
	* @author joaosantacruz.com
	*/
	private String getDateTime() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date();

		return dateFormat.format(date);
	}

	/*
	* This method sets individual track volume 
	* @author joaosantacruz.com
	*/
	void setVolume() {
		float trackLeftVolume;
		float trackRightVolume;
		
		if (balance > 50){
			trackLeftVolume = (100 - balance + 1) / 10;
		} else {
			trackLeftVolume = rightVolume; 
		}
		if (balance < 50) {
			trackRightVolume = (balance + 1) / 10;
		} else {
			trackRightVolume = leftVolume;
		}
		
		if (this.isMuted) {
			trackRightVolume = 0;
			trackLeftVolume = 0;
		}
		
		Log.v("JOAO", "SET VOLUME | mute:" + this.isMuted + " (TRACK-" + trackNumber + " > > > MAIN[L/R]=" + rightVolume + "/" + leftVolume + " - - FINAL[L/R]=" + trackLeftVolume / 10 + "/" + trackRightVolume / 10 + ") balance(" + balance + ")");
		mediaPlayer.setVolume(trackLeftVolume / 10, trackRightVolume / 10);
	}

	/*
	* This method creates track file 
	* @author joaosantacruz.com
	*/
	String createTrackFile() throws IOException {

		String filename = trackName + EXTENSION;
		String filepath = APPDIR + songName + "/" + filename;
		File f = new File(filepath);

		if (!f.exists()) {
			try {
				f.createNewFile();
				Log.v("JOAO", "CREATING: " + filepath);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			if (f.length() > 0) {
				setRecorded(true);
			}
		}

		return filepath;
	}

	boolean isRecording() {
		return isRecording;
	}
	void setRecording(boolean isRecording) {
		this.isRecording = isRecording;
	}

	boolean isPlaying() {
		return isPlaying;
	}
	void setPlaying(boolean isPlaying) {
		this.isPlaying = isPlaying;
	}

	boolean isMuted() {
		return isMuted;
	}

	String getLastUpdate() {
		return lastUpdate;
	}
	void setLastUpdate(String lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
	
	boolean isRecordable() {
		return isRecordable;
	}
	void setRecordable(boolean isRecordable) {
		this.isRecordable = isRecordable;
	}

	boolean isRecorded() {
		return isRecorded;
	}
	void setRecorded(boolean isRecorded) {
		this.isRecorded = isRecorded;
	}

	int getLeftVolume() {
		return leftVolume;
	}
	void setLeftVolume(int leftVolume) {
		this.leftVolume = leftVolume;
	}

	int getRightVolume() {
		return rightVolume;
	}
	void setRightVolume(int rightVolume) {
		this.rightVolume = rightVolume;
	}

	String getTrackName() {
		return trackName;
	}
	void setTrackName(String trackName) {
		this.trackName = trackName;
	}

	String getTrackPath() {
		return trackPath;
	}
	void setTrackPath(String trackPath) {
		this.trackPath = trackPath;
	}

	String getTrackNumber() {
		return trackNumber;
	}
	void setTrackNumber(String trackNumber) {
		this.trackNumber = trackNumber;
	}

	String getSongName() {
		return songName;
	}
	void setSongName(String songName) {
		this.songName = songName;
	}

	int getBalance() {
		return balance;
	}
	void setBalance(int balance) {
		this.balance = balance;
		this.setVolume();
	}

	/*
	* This method finalizes 'mediaPlayer' object
	* @author joaosantacruz.com
	*/
	public void finalize() throws Throwable {

	    // deallocate all memory
	    if (mediaPlayer != null) {
	        if (mediaPlayer.isPlaying()) {
	        	mediaPlayer.stop();
	        }
	        mediaPlayer.release();
	        mediaPlayer = null;
	    }

		super.finalize();
	}
}

