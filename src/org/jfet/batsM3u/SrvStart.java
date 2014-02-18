package org.jfet.batsM3u;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class SrvStart extends Activity {
    
    @Override
    public void onCreate(Bundle siState) {
        super.onCreate(siState);
        startService(new Intent(getApplicationContext(), M3uPlay.class));
        this.finish();
    }

}
