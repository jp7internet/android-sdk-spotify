package com.example.androidtest;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.view.View;
import android.widget.ImageView;


import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

public class MainActivity extends AppCompatActivity {

    private static final String CLIENT_ID = "b2fba06d5d054c589fe54a5ce19b9855";
    private static final String REDIRECT_URI = "spotify-ios-quick-start://spotify-login-callback";
    private static final int REQUEST_CODE = 1337;

    private SpotifyAppRemote mSpotifyAppRemote;
    AuthenticationRequest.Builder builder =
            new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);

    private static Button previous = null;
    private static Button next = null;
    private static Button login = null;
    private static Button playPause = null;
    private static ImageView coverArt = null;
    private static Boolean paused = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.previous = findViewById(R.id.previous);
        this.next = findViewById(R.id.next);
        this.login = findViewById(R.id.btn_login);
        this.playPause = findViewById(R.id.play_pause);
        this.coverArt = findViewById(R.id.imageView);

        previous.setOnClickListener(v -> mSpotifyAppRemote.getPlayerApi().skipPrevious());
        next.setOnClickListener(v -> mSpotifyAppRemote.getPlayerApi().skipNext());
        playPause.setOnClickListener(v -> {
                if (paused) {
                    mSpotifyAppRemote.getPlayerApi().resume();
                } else {
                    mSpotifyAppRemote.getPlayerApi().pause();
                }
            }
        );
        login.setOnClickListener(v -> {
            builder.setScopes(new String[]{"streaming"});
            AuthenticationRequest request = builder.build();
            AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    login.setVisibility(View.GONE);
                    next.setVisibility(View.VISIBLE);
                    previous.setVisibility(View.VISIBLE);
                    playPause.setVisibility(View.VISIBLE);

                    ConnectionParams connectionParams =
                        new ConnectionParams.Builder(CLIENT_ID)
                            .setRedirectUri(REDIRECT_URI)
                            .build();

                    SpotifyAppRemote.connect(this, connectionParams,
                        new Connector.ConnectionListener() {

                            @Override
                            public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                                mSpotifyAppRemote = spotifyAppRemote;
                                Log.d("MainActivity", "Connected! Yay!");
                                // Now you can start interacting with App Remote

                                mSpotifyAppRemote.getPlayerApi().play("spotify:playlist:37i9dQZF1DX2sUQwD7tbmL");
                                mSpotifyAppRemote.getPlayerApi()
                                    .subscribeToPlayerState()
                                    .setEventCallback(playerState -> {
                                        final Track track = playerState.track;
                                        MainActivity.playPause.setText(playerState.isPaused ? "Play" : "Pause");
                                        MainActivity.paused = playerState.isPaused;
                                        mSpotifyAppRemote.getImagesApi()
                                            .getImage(playerState.track.imageUri)
                                                .setResultCallback(bitmap -> MainActivity.coverArt.setImageBitmap(bitmap));
                                        if (track != null) {
                                            Log.d("MainActivity", track.name + " by " + track.artist.name);
                                        }
                                    });
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                Log.e("MainActivity", throwable.getMessage(), throwable);

                                // Something went wrong when attempting to connect! Handle errors here
                            }
                    });
                    Log.d("MainActivity", "Connected! Yay!");
                    Log.d("MainActivity", "Token: " + response.getAccessToken());

                    break;

                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    break;

                // Most likely auth flow was cancelled
                default:
                    // Handle other cases
            }
        }
    }
}
