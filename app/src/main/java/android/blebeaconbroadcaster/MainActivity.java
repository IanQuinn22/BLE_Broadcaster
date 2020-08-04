package android.blebeaconbroadcaster;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.PublishCallback;
import com.google.android.gms.nearby.messages.PublishOptions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private MessageListener mMessageListener;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int TTL_IN_SECONDS = 3 * 60;
    private final static int REQUEST_ENABLE_BT = 1;
    private Message message;
    private GoogleApiClient mGoogleApiClient;
    private static final Strategy PUB_SUB_STRATEGY = new Strategy.Builder().setTtlSeconds(TTL_IN_SECONDS).build();
    private Switch publish;
    private Switch subscribe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        publish = findViewById(R.id.publish);
        subscribe = findViewById(R.id.subscribe);
        message = new Message("HI".getBytes());

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        mMessageListener = new MessageListener() {
            @Override
            public void onFound(Message message) {
                Log.d(TAG, "Found message: " + new String(message.getContent()));
            }

            @Override
            public void onLost(Message message) {
                Log.d(TAG, "Lost sight of message: " + new String(message.getContent()));
            }
        };

        subscribe.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    if (isChecked) {
                        subscribe();
                    } else {
                        unsubscribe();
                    }

                }
            }
        });

        publish.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    if (isChecked) {
                        publish();
                    } else {
                        unpublish();
                    }

                }
            }
        });
        buildGoogleApiClient();
    }

    private void buildGoogleApiClient() {

        if (mGoogleApiClient != null) {
            return;
        }
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, this)
                .build();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        publish.setEnabled(false);
        subscribe.setEnabled(false);
        Log.e("ERROR","Exception while connecting to Google Play services: " +
                connectionResult.getErrorMessage());
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("ERROR","Connection suspended. Error code: " + i);
    }


    @Override

    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "GoogleApiClient connected");
        if (publish.isChecked()) {
            publish();
        }
        if (subscribe.isChecked()) {
            subscribe();
        }
    }

    private void subscribe() {
        Log.i(TAG, "Subscribing");
        SubscribeOptions options = new SubscribeOptions.Builder()
                .setStrategy(PUB_SUB_STRATEGY)
                .setCallback(new SubscribeCallback() {
                    @Override
                    public void onExpired() {
                        super.onExpired();
                        Log.i(TAG, "No longer subscribing");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                subscribe.setChecked(false);
                            }
                        });
                    }
                }).build();
        Nearby.getMessagesClient(this).subscribe(mMessageListener, options);
    }

    private void publish() {
        Log.i(TAG, "Publishing");
        PublishOptions options = new PublishOptions.Builder()
                .setStrategy(PUB_SUB_STRATEGY)
                .setCallback(new PublishCallback() {
                    @Override
                    public void onExpired() {
                        super.onExpired();
                        Log.i(TAG, "No longer publishing");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                publish.setChecked(false);
                            }
                        });
                    }
                }).build();
        Nearby.getMessagesClient(this).publish(message, options);
    }

    private void unsubscribe() {
        Log.i(TAG, "Unsubscribing.");
        Nearby.getMessagesClient(this).unsubscribe(mMessageListener);
    }

    private void unpublish() {
        Log.i(TAG, "Unpublishing.");
        Nearby.getMessagesClient(this).unpublish(message);
    }

}