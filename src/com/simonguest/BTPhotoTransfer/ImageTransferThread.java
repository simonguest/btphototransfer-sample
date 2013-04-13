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
    private final String TAG = "BTPHOTO/ImageTransferThread";
    private final BluetoothSocket socket;
    private Handler imageDisplayHandler;

    public ImageTransferThread(BluetoothSocket socket, Handler imageDisplayHandler) {
        this.socket = socket;
        this.imageDisplayHandler = imageDisplayHandler;
    }

    public void run() {
        try {
            InputStream inputStream = socket.getInputStream();
            boolean waitingForHeader = true;
            int expectedImageSize = 0;
            ByteArrayOutputStream imageOutputStream = new ByteArrayOutputStream();
            byte[] headerBytes = new byte[22];
            byte[] imageDigest = new byte[16];
            int headerIndex = 0;

            while (true) {
                if (waitingForHeader) {
                    byte[] header = new byte[1];
                    inputStream.read(header, 0, 1);
                    Log.v(TAG, "Received Header Byte: " + header[0]);
                    headerBytes[headerIndex++] = header[0];

                    if (headerIndex == 22) {
                        if ((headerBytes[0] == Constants.HEADER_MSB) && (headerBytes[1] == Constants.HEADER_LSB)) {
                            Log.v(TAG, "Header Received.  Now obtaining length");
                            byte[] imageSizeBuffer = Arrays.copyOfRange(headerBytes, 2, 6);
                            expectedImageSize = Utils.byteArrayToInt(imageSizeBuffer);
                            Log.v(TAG, "Image size: " + expectedImageSize);
                            imageDigest = Arrays.copyOfRange(headerBytes, 6, 22);
                            waitingForHeader = false;
                        } else {
                            Log.e(TAG, "Did not receive correct header.  Closing socket");
                            socket.close();
                            break;
                        }
                    }

                } else {
                    // Read the image data from the stream in chunks
                    byte[] buffer = new byte[Constants.CHUNK_SIZE];
                    Log.v(TAG, "Waiting for data.  Expecting " + expectedImageSize + " more bytes.");
                    int bytesRead = inputStream.read(buffer);
                    Log.v(TAG, "Read " + bytesRead + " bytes into buffer");
                    imageOutputStream.write(buffer, 0, bytesRead);
                    expectedImageSize -= bytesRead;
                    if (expectedImageSize <= 0) {
                        Log.v(TAG, "Complete photo has been received.");
                        break;
                    }
                }
            }

            // check the integrity of the image
            final byte[] finalImage = imageOutputStream.toByteArray();

            if (Utils.imageDigestMatch(finalImage, imageDigest)) {
                Log.v(TAG, "Image digest matches OK.");
                // Return the image to the caller
                Message message = new Message();
                message.obj = finalImage;
                imageDisplayHandler.sendMessage(message);

                // Send the digest back to the client as a confirmation
                Log.v(TAG, "Sending back image digest for confirmation");
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(imageDigest);

            } else {
                Log.e(TAG, "Image digest did not match.  Corrupt image?");
            }

            Log.v(TAG, "Closing server socket");
            socket.close();

        } catch (Exception ex) {
            Log.d(TAG, ex.toString());
        }
    }
}
