package org.jfet.batsM3u;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.media.MediaPlayer;
import android.media.AudioManager;

public class M3uPlay extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {
    private MediaPlayer mPlayer = null;
    private List<String> m3uTracks = null;
    private boolean isPaused = false;
    private int trackNum = 0;

    static final String START = "org.jfet.batsM3u.START";
    static final String NEXT = "org.jfet.batsM3u.NEXT";
    static final String PREV = "org.jfet.batsM3u.PREV";
    static final String PAUSE = "org.jfet.batsM3u.PAUSE";
    static final String PLAY = "org.jfet.batsM3u.PLAY";
    static final String STOP = "org.jfet.batsM3u.STOP";
    private static final int bufSize = 4096;

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
    static List<String> parseM3u(String m3uFile) {
        final List<String> m3uTracks = new ArrayList<String>();
        final String[] lines = m3uFile.split("\r\n|\r|\n");

        for (int i=0; i<lines.length; i++) {
            if ( (lines[i].length() == 0) || lines[i].substring(0,1).equals("#") ) {
                continue;
            } else {
                m3uTracks.add(lines[i]);
            }
        }

        return m3uTracks;
    }

    List<String> getM3u(Uri uri) throws IOException {
        //final URL url = new URL(uri.toString());
        final List<String> m3uTracks;
        //final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        final InputStream in = getApplicationContext().getContentResolver().openInputStream(uri);

        m3uTracks = parseM3u(istrm2String(in));

        return m3uTracks;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // no binding should be allowed here
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Intent nextIntent = new Intent(getApplicationContext(), M3uPlay.class);
        nextIntent.putExtra(M3uPlay.NEXT, true);
        PendingIntent pNextIntent = PendingIntent.getService(getApplicationContext(), 0, nextIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent prevIntent = new Intent(getApplicationContext(), M3uPlay.class);
        prevIntent.putExtra(M3uPlay.PREV, true);
        PendingIntent pPrevIntent = PendingIntent.getService(getApplicationContext(), 1, prevIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent playIntent = new Intent(getApplicationContext(), M3uPlay.class);
        playIntent.putExtra(M3uPlay.PLAY, true);
        PendingIntent pPlayIntent = PendingIntent.getService(getApplicationContext(), 2, playIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent pauseIntent = new Intent(getApplicationContext(), M3uPlay.class);
        pauseIntent.putExtra(M3uPlay.PAUSE, true);
        PendingIntent pPauseIntent = PendingIntent.getService(getApplicationContext(), 3, pauseIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent stopIntent = new Intent(getApplicationContext(), M3uPlay.class);
        stopIntent.putExtra(M3uPlay.STOP, true);
        PendingIntent pStopIntent = PendingIntent.getService(getApplicationContext(), 4, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        boolean justResume = false;
        final Notification.Builder nb = new Notification.Builder(getApplicationContext());
        nb.setContentIntent(pStopIntent)
        .setSmallIcon(R.drawable.ic_stat_notify)
        .setWhen(System.currentTimeMillis())
        .setAutoCancel(true)
        .setContentTitle("Bats! M3u")
        .addAction(android.R.drawable.ic_media_previous, "prev", pPrevIntent);

        if (null == mPlayer) {
            mPlayer = new MediaPlayer();
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);            
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnErrorListener(this);
            mPlayer.setOnCompletionListener(this);
        }

        if (intent != null) {
            if (intent.getBooleanExtra(M3uPlay.START, false) && null != intent.getData()) {
                try {
                    m3uTracks = getM3u(intent.getData());
                    trackNum = 0;
                } catch (IOException ex) {
                    return stopPlayer();
                }
            } else if (m3uTracks != null) {
                if (intent.getBooleanExtra(M3uPlay.NEXT, false)) {
                    if (++trackNum == m3uTracks.size())
                        trackNum = m3uTracks.size() - 1;
                } else if (intent.getBooleanExtra(M3uPlay.PREV, false)) {
                    if (--trackNum < 0)
                        trackNum = 0;
                } else if (intent.getBooleanExtra(M3uPlay.PAUSE, false)) {
                    isPaused = true;
                } else if (intent.getBooleanExtra(M3uPlay.PLAY, false)) {
                    isPaused = false;
                    justResume = true;
                } else if (intent.getBooleanExtra(M3uPlay.STOP, false)) {
                    return stopPlayer();
                } else {
                    return stopPlayer();
                }
            } else {
                return stopPlayer();
            }
        }

        final String url = m3uTracks.get(trackNum);
        final String fileName;
        if (url.indexOf('/') != -1)
            fileName = URLDecoder.decode(url.substring(url.lastIndexOf('/')+1));
        else
            fileName = URLDecoder.decode(url);

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
                    mPlayer.setDataSource(url);
                    mPlayer.prepareAsync();
                } catch (IOException ex) {
                    return stopPlayer();
                } catch (IllegalArgumentException ex) {
                    return stopPlayer();
                }

            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);

        if (null != mPlayer) {
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
        }
    }

    @Override
    public void onPrepared(MediaPlayer p) {
        p.start();
    }

    @Override
    public boolean onError(MediaPlayer p, int what, int extra) {
        stopPlayer();
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer p) {
        final Intent in = new Intent(getApplicationContext(), M3uPlay.class);
        in.putExtra(M3uPlay.NEXT, true);
        startService(in);
    }

    private int stopPlayer() {
        mPlayer.reset();
        mPlayer.release();
        mPlayer = null;
        stopForeground(true);
        return START_NOT_STICKY;
    }
}
