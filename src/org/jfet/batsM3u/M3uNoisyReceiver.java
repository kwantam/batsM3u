package org.jfet.batsM3u;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
//import android.util.Log;
import android.view.KeyEvent;

public class M3uNoisyReceiver extends BroadcastReceiver {
    //public static String logTag = "org.jfet.batsM3u.M3uNoisyReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        final Intent in = new Intent(context, M3uPlay.class);

        if (intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
            //Log.v(logTag,"got noisy warning");
            if (isRunning(context)) {
                in.putExtra(M3uPlay.PAUSE, true);
                context.startService(in);
            //} else {
            //    Log.v(logTag,"service not running?");
            }
        } else if (intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) {
            final KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            
            // if somehow we didn't get an event or if the service is not running (neither of these should happen!) do nothing
            if (null == event || !isRunning(context) || event.getAction() != KeyEvent.ACTION_DOWN)
                return;
            switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                in.putExtra(M3uPlay.PLAY, true);
                break;

            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_HEADSETHOOK:
                in.putExtra(M3uPlay.PAUSE, true);
                break;
                
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                in.putExtra(M3uPlay.NEXT, true);
                break;
                
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                in.putExtra(M3uPlay.PREV, true);
                break;
                
            case KeyEvent.KEYCODE_MEDIA_STOP:
                in.putExtra(M3uPlay.STOP, true);
                break;
                
            default:
                return;
            }
            
            // send the appropriate message to the service
            context.startService(in);
        }
    }
    
    private boolean isRunning(Context context) {
        final ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo rsi : am.getRunningServices(Integer.MAX_VALUE)) {
            if (M3uPlay.class.getName().equals(rsi.service.getClassName()))
                    return true;
        }
        return false;
    }
}
