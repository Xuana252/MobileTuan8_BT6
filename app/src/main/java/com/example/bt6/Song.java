package com.example.bt6;

import android.net.Uri;

public class Song {
    private String name;
    private long duration;

    private Uri uri;

    private String path;

    private boolean isSelected = false;

    public Song (String NAME,long DURATION,Uri URI,String PATH) {
        this.name=NAME;
        this.duration=DURATION;
        this.uri=URI;
        this.path=PATH;
    }

    public String getName() {
        return this.name;
    }

    public Uri getUri() {
        return this.uri;
    }

    public String getPath() {
        return this.path;
    }

    public long getDuration() {
        return this.duration;
    }

    public boolean isSelected() {
        return this.isSelected;
    }

    public void setSelected(boolean state) {
        this.isSelected=state;
    }
}
