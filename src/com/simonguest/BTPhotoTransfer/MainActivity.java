package com.simonguest.BTPhotoTransfer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.*;

import com.simonguest.btxfr.MessageType;
import com.simonguest.btxfr.ClientThread;
import com.simonguest.btxfr.ProgressData;
import com.simonguest.btxfr.ServerThread;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private static final String TAG = "BTPHOTO/MainActivity";
    private Spinner deviceSpinner;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        MainApplication.clientHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MessageType.READY_FOR_DATA: {
                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        File file = new File(Environment.getExternalStorageDirectory(), MainApplication.TEMP_IMAGE_FILE_NAME);
                        Uri outputFileUri = Uri.fromFile(file);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                        startActivityForResult(takePictureIntent, MainApplication.PICTURE_RESULT_CODE);
                        break;
                    }

                    case MessageType.COULD_NOT_CONNECT: {
                        Toast.makeText(MainActivity.this, "Could not connect to the paired device", Toast.LENGTH_SHORT).show();
                        break;
                    }

                    case MessageType.SENDING_DATA: {
                        progressDialog = new ProgressDialog(MainActivity.this);
                        progressDialog.setMessage("Sending photo...");
                        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        progressDialog.show();
                        break;
                    }

                    case MessageType.DATA_SENT_OK: {
                        if (progressDialog != null) {
                            progressDialog.dismiss();
                            progressDialog = null;
                        }
                        Toast.makeText(MainActivity.this, "Photo was sent successfully", Toast.LENGTH_SHORT).show();
                        break;
                    }

                    case MessageType.DIGEST_DID_NOT_MATCH: {
                        Toast.makeText(MainActivity.this, "Photo was sent, but didn't go through correctly", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
            }
        };


        MainApplication.serverHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MessageType.DATA_RECEIVED: {
                        if (progressDialog != null) {
                            progressDialog.dismiss();
                            progressDialog = null;
                        }

                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 2;
                        Bitmap image = BitmapFactory.decodeByteArray(((byte[]) message.obj), 0, ((byte[]) message.obj).length, options);
                        ImageView imageView = (ImageView) findViewById(R.id.imageView);
                        imageView.setImageBitmap(image);
                        break;
                    }

                    case MessageType.DIGEST_DID_NOT_MATCH: {
                        Toast.makeText(MainActivity.this, "Photo was received, but didn't come through correctly", Toast.LENGTH_SHORT).show();
                        break;
                    }

                    case MessageType.DATA_PROGRESS_UPDATE: {
                        // some kind of update
                        MainApplication.progressData = (ProgressData) message.obj;
                        double pctRemaining = 100 - (((double) MainApplication.progressData.remainingSize / MainApplication.progressData.totalSize) * 100);
                        if (progressDialog == null) {
                            progressDialog = new ProgressDialog(MainActivity.this);
                            progressDialog.setMessage("Receiving photo...");
                            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                            progressDialog.setProgress(0);
                            progressDialog.setMax(100);
                            progressDialog.show();
                        }
                        progressDialog.setProgress((int) Math.floor(pctRemaining));
                        break;
                    }

                    case MessageType.INVALID_HEADER: {
                        Toast.makeText(MainActivity.this, "Photo was sent, but the header was formatted incorrectly", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
            }
        };

        if (MainApplication.pairedDevices != null) {
            if (MainApplication.serverThread == null) {
                Log.v(TAG, "Starting server thread.  Able to accept photos.");
                MainApplication.serverThread = new ServerThread(MainApplication.adapter, MainApplication.serverHandler);
                MainApplication.serverThread.start();
            }
        }

        if (MainApplication.pairedDevices != null) {
            ArrayList<DeviceData> deviceDataList = new ArrayList<DeviceData>();
            for (BluetoothDevice device : MainApplication.pairedDevices) {
                deviceDataList.add(new DeviceData(device.getName(), device.getAddress()));
            }

            ArrayAdapter<DeviceData> deviceArrayAdapter = new ArrayAdapter<DeviceData>(this, android.R.layout.simple_spinner_item, deviceDataList);
            deviceArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
                            MainApplication.clientThread = new ClientThread(device, MainApplication.clientHandler);
                            MainApplication.clientThread.start();
                        }
                    }
                }
            });
        } else {
            Toast.makeText(this, "Bluetooth is not enabled or supported on this device", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MainApplication.PICTURE_RESULT_CODE) {
            if (resultCode == RESULT_OK) {
                Log.v(TAG, "Photo acquired from camera intent");
                try {
                    File file = new File(Environment.getExternalStorageDirectory(), MainApplication.TEMP_IMAGE_FILE_NAME);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 2;
                    Bitmap image = BitmapFactory.decodeFile(file.getAbsolutePath(), options);

                    ByteArrayOutputStream compressedImageStream = new ByteArrayOutputStream();
                    image.compress(Bitmap.CompressFormat.JPEG, MainApplication.IMAGE_QUALITY, compressedImageStream);
                    byte[] compressedImage = compressedImageStream.toByteArray();
                    Log.v(TAG, "Compressed image size: " + compressedImage.length);

                    // Invoke client thread to send
                    Message message = new Message();
                    message.obj = compressedImage;
                    MainApplication.clientThread.incomingHandler.sendMessage(message);

                    // Display the image locally
                    ImageView imageView = (ImageView) findViewById(R.id.imageView);
                    imageView.setImageBitmap(image);

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