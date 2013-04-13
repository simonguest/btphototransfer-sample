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
    private Spinner deviceSpinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        MainApplication.imageDisplayHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
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
                if (message.obj != null) {
                    Toast.makeText(MainActivity.this, ((String) message.obj), Toast.LENGTH_SHORT).show();
                }
            }
        };

        MainApplication.imageCaptureHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File file = new File(Environment.getExternalStorageDirectory(), Constants.IMAGE_FILE_NAME);
                Uri outputFileUri = Uri.fromFile(file);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                startActivityForResult(takePictureIntent, Constants.PICTURE_RESULT_CODE);
            }
        };

        ArrayList<DeviceData> deviceDataList = new ArrayList<DeviceData>();
        for (BluetoothDevice device : MainApplication.pairedDevices) {
            deviceDataList.add(new DeviceData(device.getName(), device.getAddress()));
        }

        ArrayAdapter<DeviceData> deviceArrayAdapter = new ArrayAdapter<DeviceData>(this, android.R.layout.simple_spinner_item, deviceDataList);
        deviceArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        deviceSpinner = (Spinner) findViewById(R.id.deviceSpinner);
        deviceSpinner.setAdapter(deviceArrayAdapter);

        Button clientButton = (Button) findViewById(R.id.clientButton);
        clientButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DeviceData deviceData = (DeviceData) deviceSpinner.getSelectedItem();
                for (BluetoothDevice device : MainApplication.adapter.getBondedDevices()) {
                    if (device.getAddress().contains(deviceData.getValue())) {
                        Log.v(TAG, "Starting client thread");
                        if (MainApplication.clientThread != null) {
                            MainApplication.clientThread.cancel();
                        }
                        MainApplication.clientThread = new ClientThread(device, UUID.fromString(Constants.UUID_STRING), MainApplication.toastDisplayHandler, MainApplication.imageCaptureHandler);
                        MainApplication.clientThread.start();
                    }
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, "Stopping server thread.  No longer able to accept images.");
        MainApplication.serverThread.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "Starting server thread.  Able to accept images.");
        MainApplication.serverThread = new ServerThread(MainApplication.adapter, Constants.NAME, UUID.fromString(Constants.UUID_STRING), MainApplication.imageDisplayHandler);
        MainApplication.serverThread.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.PICTURE_RESULT_CODE) {
            if (resultCode == RESULT_OK) {
                Log.v(TAG, "Photo acquired from camera intent");
                try {
                    File file = new File(Environment.getExternalStorageDirectory(), Constants.IMAGE_FILE_NAME);
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

    class DeviceData {
        public DeviceData(String spinnerText, String value) {
            this.spinnerText = spinnerText;
            this.value = value;
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