package org.jfet.batsM3u;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.webkit.URLUtil;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.util.Base64;
//import android.util.Log;

public class M3uPlay extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {
    private List<String> m3uTracks = null;
    private boolean isPaused = false;
    private boolean lockTrack = false;
    private int trackNum = 0;

    static boolean isRunning = false;
    private MediaPlayer mPlayer = null;
    private WifiLock wlock = null;

    static final String START = "org.jfet.batsM3u.START";
    static final String NEXT = "org.jfet.batsM3u.NEXT";
    static final String PREV = "org.jfet.batsM3u.PREV";
    static final String PAUSE = "org.jfet.batsM3u.PAUSE";
    static final String PLAY = "org.jfet.batsM3u.PLAY";
    static final String STOP = "org.jfet.batsM3u.STOP";
    private static final int bufSize = 4096;

    //static final String logTag = "org.jfet.batsM3u.M3uPlay";

    private Intent nextIntent = null;
    private Intent prevIntent = null;
    private Intent playIntent = null;
    private Intent pauseIntent = null;
    private Intent stopIntent = null;
    private PendingIntent pNextIntent = null;
    private PendingIntent pPrevIntent = null;
    private PendingIntent pPlayIntent = null;
    private PendingIntent pPauseIntent = null;
    private PendingIntent pStopIntent = null;
    private AudioManager am = null;

    @Override
    public void onCreate() {
        super.onCreate();

        nextIntent = new Intent(getApplicationContext(), M3uPlay.class);
        nextIntent.putExtra(M3uPlay.NEXT, true);
        pNextIntent = PendingIntent.getService(getApplicationContext(), 0, nextIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        prevIntent = new Intent(getApplicationContext(), M3uPlay.class);
        prevIntent.putExtra(M3uPlay.PREV, true);
        pPrevIntent = PendingIntent.getService(getApplicationContext(), 1, prevIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        playIntent = new Intent(getApplicationContext(), M3uPlay.class);
        playIntent.putExtra(M3uPlay.PLAY, true);
        pPlayIntent = PendingIntent.getService(getApplicationContext(), 2, playIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        pauseIntent = new Intent(getApplicationContext(), M3uPlay.class);
        pauseIntent.putExtra(M3uPlay.PAUSE, true);
        pPauseIntent = PendingIntent.getService(getApplicationContext(), 3, pauseIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        stopIntent = new Intent(getApplicationContext(), M3uPlay.class);
        stopIntent.putExtra(M3uPlay.STOP, true);
        pStopIntent = PendingIntent.getService(getApplicationContext(), 4, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        am = ((AudioManager) getSystemService(Context.AUDIO_SERVICE));
    }

   @Override
    public IBinder onBind(Intent intent) {
        // no binding should be allowed here
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        boolean justResume = false;
        final NotificationCompat.Builder nb = new NotificationCompat.Builder(getApplicationContext());
        nb.setContentIntent(pStopIntent)
        .setSmallIcon(R.drawable.ic_stat_notify)
        .setWhen(System.currentTimeMillis())
        .setAutoCancel(true)
        .setContentTitle("Bats! M3u")
        .addAction(android.R.drawable.ic_media_previous, "prev", pPrevIntent);

        if (null == mPlayer) {
        	if (isRunning)	// shouldn't ever have another instance going! something wrong here...
        		stopSelf();
        	isRunning = true;
            //doLog("allocating new media player");
            mPlayer = new MediaPlayer();
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnErrorListener(this);
            mPlayer.setOnCompletionListener(this);
            mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            // if we're playing over WiFi, get a WiFi lock
            final NetworkInfo my_network_info = ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
            if (my_network_info != null && my_network_info.getType() == ConnectivityManager.TYPE_WIFI) {
                wlock = ((WifiManager) getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "batsM3u");
                wlock.setReferenceCounted(false);   // no need for ref counting here, just hold or don't
                wlock.acquire();
            }

            // get audio focus
            final int focusReq = am.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (focusReq != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // DENIED ; clean up and go away I guess
                return stopPlayer();
            }
        }

        if (intent != null) {
            if (intent.getBooleanExtra(M3uPlay.START, false) && null != intent.getData()) {
                try {
                    //doLog("getting M3u");
                    final Uri theUri = intent.getData();
                    m3uTracks = parseM3u(istrm2String(getApplicationContext().getContentResolver().openInputStream(theUri)),theUri.toString());
                } catch (IOException ex) {
                    //doLog("failed to get M3u");
                    return stopPlayer();
                }
                //doLog("got M3u");
                // we're kinda restarting from scratch here, so we need to reset some other vars too
                trackNum = 0;
                isPaused = false;
                lockTrack = false;
            } else if (null != m3uTracks) {
                if (intent.getBooleanExtra(M3uPlay.STOP, false)) {
                    //doLog("asked to stop");
                    return stopPlayer();
                } else if (! lockTrack) {   // only allow NEXT, PREV, PAUSE, and PLAY if we're not asynchronously prepping a track
                    if (intent.getBooleanExtra(M3uPlay.NEXT, false)) {
                        //doLog("NEXT");
                    	trackNum += 1;
                    	if (! checkTrackBounds())
                    		return stopPlayer();
                    } else if (intent.getBooleanExtra(M3uPlay.PREV, false)) {
                        //doLog("PREV");
                    	trackNum -= 1;
                    	if (! checkTrackBounds())
                    		return stopPlayer();
                    } else if (intent.getBooleanExtra(M3uPlay.PAUSE, false)) {
                    	isPaused = true;
                    } else if (intent.getBooleanExtra(M3uPlay.PLAY, false)) {
                        //doLog("PLAY");
                    	if (isPaused) { // play can also be used to toggle the pause state (for convenience of media keys
                    		isPaused = false;
                    		justResume = true;
                    	} else {
                    		isPaused = true;
                    	}
                    } else {
                        //doLog("track unlocked, but unknown intent");
                        return START_NOT_STICKY;    // don't know wtf this is; do nothing
                    }
                } else {
                    //doLog("doing nothing, track locked");
                    return START_NOT_STICKY;    // if lockTrack, do nothing
                }
            } else {
                //doLog("m3uTracks null, unknown intent");
                return stopPlayer();
            }
        } else {
        	// if we get a null intent, do nothing and also indicate
        	// that we don't need any more such nulls in the future.
        	// (this happens presumably only when we're not playing,
        	// so we don't need to remain sticky.)
        	return START_NOT_STICKY;
        }

        if ( (null == m3uTracks) || (0 == m3uTracks.size()) ) {
        	return stopPlayer();
        }

        // what if we register again every time through? does that keep us from losing focus?
        am.registerMediaButtonEventReceiver(new ComponentName(getPackageName(),M3uNoisyReceiver.class.getCanonicalName()));
        final String url = m3uTracks.get(trackNum);
        final String fileName;
        if (url.indexOf('/') != -1) {
            fileName = Uri.decode(url.substring(url.lastIndexOf('/') + 1));
        } else {
            fileName = Uri.decode(url);
        }

        if (isPaused) {
            nb.setTicker("Paused")
            .setContentText("Paused (" + fileName + ")")
            .addAction(android.R.drawable.ic_media_play, "play", pPlayIntent)
            .addAction(android.R.drawable.ic_media_next, "next", pNextIntent);
            startForeground(1, nb.build());

            mPlayer.pause();
        } else {
            nb.setTicker(fileName)
            .setContentText("Playing " + fileName)
            .addAction(android.R.drawable.ic_media_pause, "pause", pPauseIntent)
            .addAction(android.R.drawable.ic_media_next, "next", pNextIntent);
            startForeground(1, nb.build());

            if (justResume) {
                justResume = false;
                mPlayer.start();
            } else {
                try {
                    mPlayer.reset();
                    final Uri thisUri = Uri.parse(url);
                    final String userInfo = thisUri.getUserInfo();

                    // handle username/password in URL
                    //doLog("prepping media player");
                    if (null != userInfo && Build.VERSION.SDK_INT >= 14) {
                        // add headers --- only available in ICS or newer
                        final Map<String, String> extra_headers = new HashMap<String, String>();
                        extra_headers.put("Authorization", "Basic " + Base64.encodeToString(userInfo.getBytes(), Base64.DEFAULT));
                        mPlayer.setDataSource(getApplicationContext(), thisUri, extra_headers);
                    } else {
                        mPlayer.setDataSource(getApplicationContext(), thisUri);
                    }

                    // indicate (for fast-forward, rewind, play, pause) that we are preparing
                    lockTrack = true;
                    mPlayer.prepareAsync();
                } catch (IOException ex) {
                    //doLog("io exception prepping media player");
                    return stopPlayer();
                } catch (IllegalArgumentException ex) {
                    //doLog("illegal argument prepping media player");
                    return stopPlayer();
                }

            }
        }

        return START_NOT_STICKY;
    }

/*
    private void doLog(String msg) {
        Log.v(logTag, msg);
    }
*/

    @Override
    public void onDestroy() {
        //doLog("destroying");
        stopForeground(true);

        if (null != mPlayer) {
            stopPlayer();
        }
    }

// media player stuff
// ******************
    @Override
    public void onPrepared(MediaPlayer p) {
        //doLog("prepared");
        mPlayer.start();
        lockTrack = false;
    }

    // returns false if there are no more tracks to play and we should give up
    private boolean checkTrackBounds() {
    	if (m3uTracks.size() == 0)
    		return false;
    	else if (trackNum < 0)
    		trackNum = 0;
    	else if (trackNum >= m3uTracks.size())
    		trackNum = m3uTracks.size() - 1;
    	
    	return true;
    }

    @Override
    public boolean onError(MediaPlayer p, int what, int extra) {
        //doLog("media player error: " + what + " " + extra);

    	if ( (1 == what) && ((-1004 == extra) || (-2147483648 == extra)) ) {
    		mPlayer.reset();
    		lockTrack = false;
    		isPaused = false;
    		// probably just a 404; remove the offending file
    		//doLog("not found: " + m3uTracks.get(trackNum));
            if (trackNum >= 0 && trackNum < m3uTracks.size()) {
                m3uTracks.remove(trackNum--);
            } else {
                // invalid track number; when NEXT executes, this will reset to track #0
                trackNum = -1;
            }
    		
    		// we decremented above, so sending NEXT
    		// will cause the player to do the right thing
    		startService(nextIntent);
    	} else {
    		stopPlayer();
    	}

        return true;
    }

    @Override
    public void onCompletion(MediaPlayer p) {
        //doLog("completed track " + trackNum);
        if ( (m3uTracks.size() - 1) <= trackNum ) {
            stopPlayer();
        } else {
            startService(nextIntent);
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
        case AudioManager.AUDIOFOCUS_GAIN:
            // returning from LOSS_TRANSIENT or LOSS_TRANSIENT_CAN_DUCK
            if (null != mPlayer) {
                mPlayer.setVolume(1.0f, 1.0f);
                if (isPaused)
                    // unpause, if necessary
                    startService(playIntent);
            }
            break;

        case AudioManager.AUDIOFOCUS_LOSS:
            // long-term loss, we just give up and die
            stopPlayer();
            break;

        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            // pause music transiently
            startService(pauseIntent);
            break;

        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
            // don't have to pause, just lower volume a bit
            if ( (null != mPlayer) && (mPlayer.isPlaying()) )
                mPlayer.setVolume(0.3f,0.3f);
            break;
        }
    }

    private int stopPlayer() {
        //doLog("stopping media player");
    	isRunning = false;
        mPlayer.release();
        mPlayer = null;
        m3uTracks = null;
        if (null != wlock) {
            wlock.release();
            wlock = null;
        }
        am.abandonAudioFocus(this);
        am.unregisterMediaButtonEventReceiver(new ComponentName(getPackageName(),M3uNoisyReceiver.class.getName()));
        stopForeground(true);
        stopSelf();
        return START_NOT_STICKY;
    }

// M3u related functions
// *********************
    // take an input stream and turn it into a string
    static String istrm2String(InputStream in) {
        final StringBuilder sb = new StringBuilder();
        final char[] buffer = new char[bufSize];
        int nRead;

        try {
            final InputStreamReader inRd = new InputStreamReader(in,"UTF-8");
            try {
                while (true) {
                    nRead = inRd.read(buffer);
                    if (nRead < 0)
                        break;
                    sb.append(buffer,0,nRead);
                }
            } finally {
                inRd.close();
                in.close();
            }
        } catch (IOException ex) {
            return "";
        }

        return sb.toString();
    }

    // parse an m3u file
    static List<String> parseM3u(String m3uContents, String m3uUri) {
        final List<String> m3uTracks = new ArrayList<String>();
        final String[] lines = m3uContents.split("\r\n|\r|\n");
        final String m3uFile;
        if (URLUtil.isFileUrl(m3uUri) && (-1 != m3uUri.lastIndexOf('/')))
            m3uFile = m3uUri.substring(0,m3uUri.lastIndexOf('/')+1);
        else
            m3uFile = null;

        for (int i=0; i<lines.length; i++) {
            if ( (lines[i].length() == 0) || lines[i].substring(0,1).equals("#"))   // ignore comments and such
                continue;
            else if (URLUtil.isNetworkUrl(lines[i]) || URLUtil.isContentUrl(lines[i]) || URLUtil.isFileUrl(lines[i])) // valid URL?
                m3uTracks.add(lines[i]);
            else if (null != m3uFile) {
                // hey, it *could* be a relative path; let's find out
                final String absStr = m3uFile + lines[i];
                try {
                if ((new File(new URI(absStr))).exists())
                    m3uTracks.add(absStr);
                } catch (URISyntaxException ex) {
                    continue;
                } catch (IllegalArgumentException ex) {
                    continue;
                }
            }
        }

        return m3uTracks;
    }
}
