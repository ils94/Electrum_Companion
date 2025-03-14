package com.droidev.electrumcompanion;

import android.util.Base64;


public class PSBTChecker {


    public static boolean isPSBT(String inputString) {
        try {
            byte[] decodedBytes = Base64.decode(inputString, Base64.DEFAULT);

            if (decodedBytes.length < 5) {
                return false;
            }

            return (decodedBytes[0] == 0x70 &&  // 'p'
                    decodedBytes[1] == 0x73 &&  // 's'
                    decodedBytes[2] == 0x62 &&  // 'b'
                    decodedBytes[3] == 0x74 &&  // 't'
                    decodedBytes[4] == (byte) 0xff);

        } catch (IllegalArgumentException e) {

            return false;
        }
    }

    public static boolean isSignedPSBT(String inputString) {
        try {

            if (inputString.length() % 2 != 0) {
                return false;
            }

            byte[] decodedBytes = hexStringToByteArray(inputString);
            if (decodedBytes.length < 5) {
                return false;
            }

            return (decodedBytes[0] == 0x70 &&  // 'p'
                    decodedBytes[1] == 0x73 &&  // 's'
                    decodedBytes[2] == 0x62 &&  // 'b'
                    decodedBytes[3] == 0x74 &&  // 't'
                    decodedBytes[4] == (byte) 0xff);

        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isSignedTransaction(String inputString) {
        try {
            if (inputString == null || inputString.length() % 2 != 0) {
                return false;
            }

            byte[] txBytes = hexStringToByteArray(inputString);
            if (txBytes.length < 10) {
                return false;
            }

            int version = txBytes[0] & 0xFF;
            if (version != 1 && version != 2) {
                return false;
            }

            int inputCountOffset = 4;
            if (txBytes[4] == 0x00 && txBytes[5] == 0x01) {
                inputCountOffset = 6;
            }

            int inputCount = txBytes[inputCountOffset] & 0xFF;
            if (inputCount == 0) {
                return false;
            }

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static boolean checkPSBT(String inputString) {
        if (inputString == null || inputString.trim().isEmpty()) {

            return false;
        }

        return isPSBT(inputString) || isSignedPSBT(inputString) || isSignedTransaction(inputString);
    }
}