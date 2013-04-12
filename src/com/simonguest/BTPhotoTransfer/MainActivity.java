package com.simonguest.BTPhotoTransfer;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends Activity {

    private static final String TAG = "BTPHOTO/MainActivity";
    private ArrayAdapter<DeviceData> deviceArrayAdapter;
    private Spinner deviceSpinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        /***
         * Setup the handlers
         */
        MainApplication.imageDisplayHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                Log.d(TAG, "Image Display Handler");
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                Bitmap image = BitmapFactory.decodeByteArray(((byte[]) message.obj), 0, ((byte[]) message.obj).length, options);
                ImageView imageView = (ImageView) findViewById(R.id.imageView);
                imageView.setImageBitmap(image);
                TextView textView = (TextView) findViewById(R.id.statusView);
                textView.setText("This image was sent from the other device");
            }
        };

        MainApplication.toastDisplayHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                Log.d(TAG, "Toast Display Handler");
                if (message.obj != null) {
                    Toast.makeText(MainActivity.this, ((String) message.obj), Toast.LENGTH_LONG).show();
                }
            }
        };

        MainApplication.imageCaptureHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                Log.d(TAG, "Image Capture Handler");
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File file = new File(Environment.getExternalStorageDirectory(), "test.jpg");
                Uri outputFileUri = Uri.fromFile(file);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                startActivityForResult(takePictureIntent, MainApplication.PICTURE_RESULT);
            }
        };

        /**
         * Construct the rest of the UI
         */
        ArrayList<DeviceData> deviceDataList = new ArrayList<DeviceData>();
        for (BluetoothDevice device : MainApplication.pairedDevices) {
            deviceDataList.add(new DeviceData(device.getName(), device.getAddress()));
        }

        deviceArrayAdapter = new ArrayAdapter<DeviceData>(this, android.R.layout.simple_spinner_item, deviceDataList);
        deviceArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        deviceSpinner = (Spinner) findViewById(R.id.deviceSpinner);
        deviceSpinner.setAdapter(deviceArrayAdapter);

        Button clientButton = (Button) findViewById(R.id.clientButton);
        clientButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DeviceData deviceData = (DeviceData) deviceSpinner.getSelectedItem();
                for (BluetoothDevice device : MainApplication.btAdapter.getBondedDevices()) {
                    Log.d(TAG, device.getAddress());
                    Log.d(TAG, deviceData.getValue());
                    if (device.getAddress().contains(deviceData.getValue())) {
                        Log.d(TAG, "Starting client thread");
                        if (MainApplication.clientThread != null)
                        {
                            MainApplication.clientThread.cancel();
                        }
                        MainApplication.clientThread = new ClientThread(device, UUID.fromString(MainApplication.UUID_STRING), MainApplication.toastDisplayHandler, MainApplication.imageCaptureHandler);
                        MainApplication.clientThread.start();
                    }
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        MainApplication.serverThread.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainApplication.serverThread = new ServerThread(MainApplication.btAdapter, MainApplication.NAME, UUID.fromString(MainApplication.UUID_STRING), MainApplication.imageDisplayHandler);
        MainApplication.serverThread.start();
        Log.d(TAG, "Server thread has started");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MainApplication.PICTURE_RESULT) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "Photo acquired from camera");
                try {
                    File file = new File(Environment.getExternalStorageDirectory(), "test.jpg");
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 2;
                    Bitmap image = BitmapFactory.decodeFile(file.getAbsolutePath(), options);

                    // Send the image
                    MainApplication.clientThread.sendImage(image);

                    // Display the image locally
                    ImageView imageView = (ImageView) findViewById(R.id.imageView);
                    imageView.setImageBitmap(image);
                    TextView statusView = (TextView) findViewById(R.id.statusView);
                    statusView.setText("This image was sent to the other device");

                } catch (Exception e) {
                    Log.d(TAG, e.toString());
                }
            }
        }
    }

    /**
     * DeviceData used for spinner control on Activity
     */
    class DeviceData {
        public DeviceData(String spinnerText, String value) {
            this.spinnerText = spinnerText;
            this.value = value;
        }

        public String getSpinnerText() {
            return spinnerText;
        }

        public String getValue() {
            return value;
        }

        public String toString() {
            return spinnerText;
        }

        String spinnerText;
        String value;
    }
}