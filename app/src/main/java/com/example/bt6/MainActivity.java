package com.example.bt6;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.loader.app.LoaderManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_AUDIO_REQUEST = 1;

    private static final int STORAGE_PERMISSION_CODE = 100;
    private TextView songName;

    private TextView currentTime;

    private TextView durationTime;

    private View playButton;

    private View rewindButton;
    private View forwardButton;

    private ImageButton loopButton;
    private SeekBar seekBar;
    private Button chooseButton;
    private MediaPlayer mediaPlayer;
    private MediaMetadataRetriever metadataRetriever;

    private Handler handler = new Handler();

    private ActivityResultLauncher<Intent> musicLauncher;

    private ArrayList<Song> songList = new ArrayList<>();

    private ListView songListView;

    private boolean isLooping = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);


        songName = findViewById(R.id.songName);
        seekBar = findViewById(R.id.seekBar);
        currentTime = findViewById(R.id.currentTime);
        durationTime = findViewById(R.id.durationTime);

        rewindButton = findViewById(R.id.rewindButton);
        playButton = findViewById(R.id.playButton);
        forwardButton = findViewById(R.id.forwardButton);
        loopButton = findViewById(R.id.loopButton);
        chooseButton = findViewById(R.id.chooseButton);
        songListView = findViewById(R.id.songList);


        mediaPlayer = new MediaPlayer();
        metadataRetriever = new MediaMetadataRetriever();

        // Open the audio picker when the button is clicked
        chooseButton.setOnClickListener(v -> openAudioPicker());
        playButton.setOnClickListener(v -> togglePlayPause());
        rewindButton.setOnClickListener(v -> rewindAudio());
        forwardButton.setOnClickListener(v -> forwardAudio());
        loopButton.setOnClickListener(v -> toggleLoop());


        // Allow the user to seek to a different position in the audio
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });


        SongListAdapter adapter = new SongListAdapter(this, R.layout.song_item, songList);
        songListView.setAdapter(adapter);
        // Register ActivityResultLauncher for audio picking
        musicLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri audioUri = result.getData().getData();

                        Log.d("AudioSelection", "Clicked song URI: " + audioUri.toString());
                        if (audioUri != null) {
                            for(Song song:songList) {
                                song.setSelected(false);
                                if(getLastSegment(audioUri).equals(getLastSegment(song.getUri()))) {
                                    song.setSelected(true);
                                    playAudio(song.getUri());
                                    retrieveMetadata(song.getUri());
                                }
                            }

                            // Notify the adapter to update the ListView
                            adapter.notifyDataSetChanged();
                        }
                    }
                });

        songListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Song clickedSong = songList.get(position);

                Log.d("AudioSelection", "Clicked song URI: " + clickedSong.getUri().toString());
                // If clicked song is already selected, do nothing
                if (clickedSong.isSelected()) return;

                // Deselect all songs
                for (Song song : songList) {
                    song.setSelected(false);
                }

                // Select the clicked song
                clickedSong.setSelected(true);
                playAudio(clickedSong.getUri());
                retrieveMetadata(clickedSong.getUri());

                // Notify the adapter to update the ListView
                adapter.notifyDataSetChanged();
            }
        });
    }

    private String getLastSegment(Uri uri) {
        // Get the last path segment from the URI
        String lastSegment = uri.getLastPathSegment();
        if (lastSegment != null) {
            // If the segment contains a prefix, split it by ':' and take the last part
            String[] parts = lastSegment.split(":");
            return parts[parts.length - 1]; // Return the last part
        }
        return null; // Return null if no segment is found
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadAllAudioFiles();
            } else {
                Toast.makeText(this, "Permissions denied to read audio files", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void toggleLoop() {
        isLooping = !isLooping;
        mediaPlayer.setLooping(isLooping);

        // Change the loop button appearance based on the loop state
        if (isLooping) {
            loopButton.setImageResource(R.drawable.ic_loop_icon); // Replace with the actual icon for loop active
            Toast.makeText(this, "Looping enabled", Toast.LENGTH_SHORT).show();
        } else {
            loopButton.setImageResource(R.drawable.ic_block_icon); // Replace with the actual icon for loop inactive
            Toast.makeText(this, "Looping disabled", Toast.LENGTH_SHORT).show();
        }
    }


    // Open file picker to choose audio file
    private void openAudioPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");  // Only show audio files
        musicLauncher.launch(intent);
    }

    // Play the selected audio file
    private void playAudio(Uri audioUri) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(this, audioUri);
            mediaPlayer.prepare();
            mediaPlayer.start();

            // Set SeekBar max to media duration
            seekBar.setMax(mediaPlayer.getDuration());
            durationTime.setText(formatDuration(mediaPlayer.getDuration()));

            // Update SeekBar position using a Handler
            handler.postDelayed(updateSeekBar, 1000);

            playButton.setBackgroundResource(R.drawable.ic_pause_icon);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            playButton.setBackgroundResource(R.drawable.ic_play_icon);
            handler.removeCallbacks(updateSeekBar);  // Stop updating the SeekBar

        } else {
            mediaPlayer.start();
            playButton.setBackgroundResource(R.drawable.ic_pause_icon);
            handler.post(updateSeekBar);  // Start updating the SeekBar from the current position

        }
    }

    private void rewindAudio() {
        if (mediaPlayer != null) {
            int newPosition = mediaPlayer.getCurrentPosition() - 5000; // Rewind by 5 seconds
            mediaPlayer.seekTo(Math.max(newPosition, 0));
            updateCurrentTime();
        }
    }

    private void forwardAudio() {
        if (mediaPlayer != null) {
            int newPosition = mediaPlayer.getCurrentPosition() + 5000; // Forward by 5 seconds
            mediaPlayer.seekTo(Math.min(newPosition, mediaPlayer.getDuration()));
            updateCurrentTime();
        }
    }

    private void updateCurrentTime() {
        currentTime.setText(formatDuration(mediaPlayer.getCurrentPosition()));
    }


    // Update SeekBar with the current position
    private Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                seekBar.setProgress(mediaPlayer.getCurrentPosition());
                updateCurrentTime();
                handler.postDelayed(this, 250);
            }
        }
    };

    // Retrieve metadata from audio file
    private void retrieveMetadata(Uri audioUri) {
        String filePath = "Unknown Title";

        // Attempt to retrieve the title metadata
        metadataRetriever.setDataSource(this, audioUri);
        String title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);

        if (title != null) {
            filePath = title;  // If title metadata is available, use it
        } else {
            Cursor cursor = getContentResolver().query(audioUri, null, null, null, null);
            if (cursor != null) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (cursor.moveToFirst() && nameIndex != -1) {

                    filePath = cursor.getString(nameIndex);
                    if (filePath.contains(".")) {
                        filePath = filePath.substring(0, filePath.lastIndexOf('.')); // Remove extension
                    }
                }
                cursor.close();
            } else {
                filePath = "unknown title";
            }
        }

        songName.setText(filePath);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Release media player resources if needed
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        // Safely release metadata retriever resources
        if (metadataRetriever != null) {
            try {
                metadataRetriever.release();
            } catch (IOException e) {
                e.printStackTrace();
            }
            metadataRetriever = null;
        }

        // Remove handler callbacks to avoid memory leaks
        handler.removeCallbacks(updateSeekBar);
    }

    private void loadAllAudioFiles() {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION},
                    MediaStore.Audio.Media.MIME_TYPE + "=?",
                    new String[]{"audio/mpeg"},  // MIME type for MP3 files
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {  // Ensure cursor is not null and has data
                songList.clear();  // Clear the list before loading new data

                int titleIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
                int idIndex = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
                int dataIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA); // Column for file path
                int durationIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);

                // Check if the column indices are valid
                if (titleIndex != -1 && idIndex != -1 && durationIndex != -1) {
                    do {
                        String songTitle = cursor.getString(titleIndex);
                        long songDuration = cursor.getLong(durationIndex);
                        long songId = cursor.getLong(idIndex);
                        String filePath = cursor.getString(dataIndex); // Get file path

                        Uri songUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId);
                        Song newSong = new Song(songTitle, songDuration, songUri, filePath);
                        songList.add(newSong);
                    } while (cursor.moveToNext());  // Continue until the end of the cursor
                } else {
                    Toast.makeText(this, "No audio files found", Toast.LENGTH_SHORT).show();
                }

                // Notify adapter that the data has changed
                ((ArrayAdapter<Song>) songListView.getAdapter()).notifyDataSetChanged();
            } else {
                Toast.makeText(this, "No audio files found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error loading audio files: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) {
                cursor.close();  // Ensure cursor is closed
            }
        }
    }

    // Helper method to format duration from milliseconds to a readable format
    private String formatDuration(long duration) {
        int seconds = (int) (duration / 1000) % 60;
        int minutes = (int) (duration / (1000 * 60)) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

}