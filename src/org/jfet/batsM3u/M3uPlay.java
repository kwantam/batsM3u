package org.jfet.batsM3u;

import java.io.IOException;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.media.MediaPlayer;
import android.media.AudioManager;

public class M3uPlay extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {
    private MediaPlayer mPlayer;

    @Override
    public IBinder onBind(Intent intent) {
        // no binding should be allowed here
        return null;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (null != mPlayer) {
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
            stopForeground(true);
            return START_NOT_STICKY;
        } else {
            final String url = "http://kwant.am/ween-bigjilm.mp3";


            final PendingIntent pi = PendingIntent.getService(getApplicationContext(),0 ,new Intent(getApplicationContext(), M3uPlay.class), PendingIntent.FLAG_UPDATE_CURRENT);
            final Notification.Builder nb = new Notification.Builder(getApplicationContext());
            nb.setContentIntent(pi)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setTicker(url)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setContentTitle("Bats! M3u")
                .setContentText("Playing");
            final Notification note = nb.build();
            startForeground(1, note);
            
            try {
                mPlayer = new MediaPlayer();
                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);            
                mPlayer.setDataSource(url);
                mPlayer.setOnPreparedListener(this);
                mPlayer.setOnErrorListener(this);
                mPlayer.setOnCompletionListener(this);
                mPlayer.prepareAsync();
            } catch (IOException ex) {
                stopForeground(true);
                mPlayer.reset();
                mPlayer.release();
                mPlayer = null;
                return START_NOT_STICKY;
            } catch (IllegalArgumentException ex) {
                stopForeground(true);
                mPlayer.reset();
                mPlayer.release();
                mPlayer = null;
                return START_NOT_STICKY;
            }
            
            return START_STICKY;
        }
    }
    
    @Override
    public void onDestroy() {
        stopForeground(true);
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
        stopForeground(true);
        mPlayer.reset();
        mPlayer.release();
        this.mPlayer = null;
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer p) {
        stopForeground(true);
        mPlayer.reset();
        mPlayer.release();
        this.mPlayer = null;
    }

}
