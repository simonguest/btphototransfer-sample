package com.simonguest.BTPhotoTransfer;

import android.util.Log;

import java.security.MessageDigest;
import java.util.Arrays;

public class Utils {
    private final static String TAG = "BTPHOTO/Utils";

    public static byte[] intToByteArray(int a) {
        byte[] ret = new byte[4];
        ret[3] = (byte) (a & 0xFF);
        ret[2] = (byte) ((a >> 8) & 0xFF);
        ret[1] = (byte) ((a >> 16) & 0xFF);
        ret[0] = (byte) ((a >> 24) & 0xFF);
        return ret;
    }

    public static int byteArrayToInt(byte[] b) {
        return (b[3] & 0xFF) + ((b[2] & 0xFF) << 8) + ((b[1] & 0xFF) << 16) + ((b[0] & 0xFF) << 24);
    }

    public static boolean imageDigestMatch(byte[] imageData, byte[] digestData) {
        return Arrays.equals(getImageDigest(imageData), digestData);
    }

    public static byte[] getImageDigest(byte[] imageData) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            return messageDigest.digest();
        } catch (Exception ex) {
            Log.e(TAG, ex.toString());
            throw new UnsupportedOperationException("MD5 algorithm not available on this device.");
        }
    }
}
