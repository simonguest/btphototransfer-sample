package com.simonguest.BTPhotoTransfer;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class ImageTransferThread extends Thread {
    private final BluetoothSocket btSocket;
    private String TAG = "BTPHOTO/ImageTransferThread";
    private Handler imageDisplayHandler;

    public ImageTransferThread(BluetoothSocket socket, Handler imageDisplayHandler) {
        btSocket = socket;
        this.imageDisplayHandler = imageDisplayHandler;
    }

    public void run() {
        try {
            InputStream is = btSocket.getInputStream();

            boolean waitingForHeader = true;
            int expectedImageSize = 0;
            ByteArrayOutputStream imageOutputStream = new ByteArrayOutputStream();

            byte[] headerBytes = new byte[22];
            byte[] imageDigest = new byte[16];
            int headerIndex = 0;


            while (true) {


                if (waitingForHeader) {
                    byte[] header = new byte[1];
                    Log.d(TAG, "Waiting for next header byte");
                    is.read(header, 0, 1);
                    Log.d(TAG, "Adding header byte to array: " + header[0]);
                    headerBytes[headerIndex++] = header[0];

                    if (headerIndex == 22) {
                        if ((headerBytes[0] == 0x10) && (headerBytes[1] == 0x55)) {

                            Log.d(TAG, "Header Received.  Now obtaining length");
                            byte[] imageSizeBuffer = Arrays.copyOfRange(headerBytes, 2, 6);
                            expectedImageSize = Utils.byteArrayToInt(imageSizeBuffer);
                            Log.d(TAG, "Image size: " + expectedImageSize);

                            Log.d(TAG, "Now getting image digest.");
                            imageDigest = Arrays.copyOfRange(headerBytes, 6, 22);

                            waitingForHeader = false;
                        } else {
                            Log.d(TAG, "Did not receive correct header.  Closing socket");
                            btSocket.close();
                            break;
                        }
                    }

                } else {
                    byte[] buffer = new byte[4192];
                    Log.d(TAG, "Waiting for data.  Expecting " + expectedImageSize + " more bytes.");
                    int bytesRead = is.read(buffer);
                    Log.d(TAG, "Read " + bytesRead + " bytes into buffer");
                    imageOutputStream.write(buffer, 0, bytesRead);
                    expectedImageSize -= bytesRead;

                    if (expectedImageSize <= 0) {
                        Log.d(TAG, "Complete photo has been received.");
                        break;
                    }
                }
            }

            // check to see whether we have the image
            final byte[] finalImage = imageOutputStream.toByteArray();

            if (Utils.imageDigestMatch(finalImage, imageDigest)) {
                Log.d(TAG, "Image digest matches OK.  Passing back to Handler.");
                Message message = new Message();
                message.obj = finalImage;
                imageDisplayHandler.sendMessage(message);

                // Send the digest back as a confirmation
                Log.d(TAG, "Sending back image digest for confirmation");
                OutputStream os = btSocket.getOutputStream();
                os.write(imageDigest);

            } else {
                Log.d(TAG, "Image digest did not match.");
            }

            Log.d(TAG, "Closing server socket");
            btSocket.close();

        } catch (Exception ex) {
            Log.d(TAG, ex.toString());
        }
    }
}
