package org.jfet.batsM3u;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class SrvStart extends Activity {

    @Override
    public void onCreate(Bundle siState) {
        super.onCreate(siState);

    	final boolean oldVer = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN;
        if (oldVer)
        	setContentView(R.layout.activity_srvstart);

        final Intent iIn = getIntent();

        // if we have something to pass on to the service, do so; else die
        if (null != iIn.getData()) {
            final Intent iOut = new Intent(getApplicationContext(), M3uPlay.class);
            iOut.putExtra(M3uPlay.START,true);
            iOut.setData(iIn.getData());
            startService(iOut);
        } else {
        	finish();
        }

        if (!oldVer)
        	// since we have action buttons, we have no UI
        	this.finish();
    }
    
    @Override
    public void onRestart() {
    	super.onRestart();
    	if (! M3uPlay.isRunning)
    		finish();
    }
    
    public void handleButton (View view) {
    	if (! M3uPlay.isRunning)
    		finish();

    	final Intent in = new Intent(getApplicationContext(), M3uPlay.class);
    	
    	// which button pushed us?
    	switch (view.getId()) {
    	case R.id.playButton:
    		in.putExtra(M3uPlay.PLAY, true); break;
    		
    	case R.id.nextButton:
    		in.putExtra(M3uPlay.NEXT, true); break;
    		
    	case R.id.prevButton:
    		in.putExtra(M3uPlay.PREV, true); break;
    		
    	case R.id.pauseButton:
    		in.putExtra(M3uPlay.PAUSE, true); break;
    		
    	case R.id.stopButton:
    		in.putExtra(M3uPlay.STOP, true);
    	}

    	startService(in);
    	
    	if (view.getId() == R.id.stopButton)
    		finish();
    }

}
