package org.jfet.batsM3u;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class SrvStart extends Activity {
    
    @Override
    public void onCreate(Bundle siState) {
        super.onCreate(siState);
        
        final Intent iIn = getIntent();

        if (null != iIn.getData()) {
            final Intent iOut = new Intent(getApplicationContext(), M3uPlay.class);
            iOut.putExtra(M3uPlay.START,true);
            iOut.setData(iIn.getData());
            startService(iOut);
        }

        this.finish();
    }

}
