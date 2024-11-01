package com.example.bt6;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Locale;

public class SongListAdapter extends ArrayAdapter<Song> {
    int resource;
    private List<Song> songs;

    public SongListAdapter(Context context, int resource, List<Song> contacts) {
        super(context, resource, contacts);
        this.songs = contacts;
        this.resource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(this.getContext());
            v = vi.inflate(this.resource, null);
        }
        Song s = getItem(position);

        if (s != null) {
            TextView songName = (TextView) v.findViewById(R.id.songName);
            TextView songDuration = (TextView) v.findViewById(R.id.songDuration);
            LinearLayout songItemLayout = (LinearLayout) v.findViewById(R.id.songItem);


            if (songName != null) {
                songName.setText(s.getName());
            }
            if (songDuration != null) {
                songDuration.setText(formatTime(s.getDuration()));
            }

            if(s.isSelected()) {
                TypedValue typedValue = new TypedValue();
                getContext().getTheme().resolveAttribute(R.attr.selected_song_color, typedValue, true);
                int color = typedValue.data;
                songItemLayout.setBackgroundColor(color);
            } else {
                songItemLayout.setBackgroundColor(Color.TRANSPARENT);
            }



        }
        return v;
    }

    private String formatTime(long milliseconds) {
        long minutes = (milliseconds / 1000) / 60;
        long seconds = (milliseconds / 1000) % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

}
