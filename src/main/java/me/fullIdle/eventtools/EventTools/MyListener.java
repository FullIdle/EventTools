package me.fullIdle.eventtools.EventTools;

import org.bukkit.event.Listener;

import java.io.File;

public class MyListener implements Listener {
    private final File file;
    public MyListener(File file){
        this.file = file;
    }

    public File getFile() {
        return file;
    }
}
