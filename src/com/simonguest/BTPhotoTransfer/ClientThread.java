package com.simonguest.BTPhotoTransfer;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class ClientThread extends Thread {
    private String TAG = "BTPHOTO/ClientThread";
    private final BluetoothSocket btSocket;
    private Handler toastDisplayHandler;
    private Handler imageCaptureHandler;
    private UUID uuid;

    public ClientThread(BluetoothDevice device, UUID uuid, Handler toastDisplayHandler, Handler imageCaptureHandler) {
        BluetoothSocket tmp = null;
        this.toastDisplayHandler = toastDisplayHandler;
        this.imageCaptureHandler = imageCaptureHandler;
        this.uuid = uuid;


        try {
            tmp = device.createRfcommSocketToServiceRecord(uuid);
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
        Log.d(TAG, "Setting up client thread tmp/device");
        btSocket = tmp;
    }

    public void run() {
        try {
            Log.d(TAG, "Opening client socket");
            btSocket.connect();
            Log.d(TAG, "Socket established!!!");

            // Now that we have a socket, callback and request an image from the UI thread
            imageCaptureHandler.sendEmptyMessage(0);

        } catch (Exception e) {
            // Couldn't connect to the server - let the user know
            Message message = new Message();
            message.obj = "Could not connect to the other device.  Is the application running?";
            toastDisplayHandler.sendMessage(message);
            Log.d(TAG, e.toString());
            try {
                btSocket.close();
            } catch (Exception ce) {
                Log.d(TAG, "Close exception: " + ce.toString());
            }
        }
    }

    public void sendImage(Bitmap image) {
        Log.d(TAG, "Will be sending the image!");

        try {
            OutputStream os = btSocket.getOutputStream();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, 10, baos);
            byte[] compressedImage = baos.toByteArray();
            Log.d(TAG, "Compressed image size: " + compressedImage.length);

            // Send the header control first
            os.write(0x10);
            os.write(0x55);

            // write size
            os.write(Utils.intToByteArray(compressedImage.length));

            // write digest
            byte[] imageDigest = Utils.getImageDigest(compressedImage);
            os.write(imageDigest);

            // now write the image
            os.write(compressedImage);
            os.flush();

            //os.write(new String("Hello world").getBytes());
            Log.d(TAG, "Image sent successfully!");

            Log.d(TAG, "Waiting for image digest as confirmation");
            InputStream is = btSocket.getInputStream();

            byte[] incomingImageDigest = new byte[16];
            int incomingImageIndex = 0;


            try {
                while (true) {
                    byte[] header = new byte[1];
                    Log.d(TAG, "Waiting for next digest byte");
                    is.read(header, 0, 1);
                    Log.d(TAG, "Adding digest byte to array: " + header[0]);
                    incomingImageDigest[incomingImageIndex++] = header[0];

                    if (incomingImageIndex == 16) {
                        // check digest
                        Log.d(TAG, "Validating received digest.");
                        if (Utils.imageDigestMatch(compressedImage, incomingImageDigest))
                        {
                            Log.d(TAG, "Digest matched OK.  Image was received OK.");

                            Message message = new Message();
                            message.obj = "Image was received OK by host.";
                            toastDisplayHandler.dispatchMessage(message);

                        }
                        else
                        {
                            Log.d(TAG, "Digest did not match.  Might want to resend.");
                        }

                        break;
                    }
                }
            } catch (Exception ex) {
                Log.d(TAG, ex.toString());
            }


        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
    }

    public void cancel() {
        try {
            btSocket.close();
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
    }
}

