package com.droidev.electrumcompanion;

import android.util.Base64;

public class PSBTChecker {

    public static boolean isPSBT(String base64String) {
        try {
            // Decode Base64
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);

            // Check if the length is at least 5 bytes
            if (decodedBytes.length < 5) {
                return false;
            }

            // Check for the "psbt\xff" magic bytes
            return (decodedBytes[0] == 0x70 &&  // 'p'
                    decodedBytes[1] == 0x73 &&  // 's'
                    decodedBytes[2] == 0x62 &&  // 'b'
                    decodedBytes[3] == 0x74 &&  // 't'
                    decodedBytes[4] == (byte) 0xff);

        } catch (IllegalArgumentException e) {
            // If Base64 decoding fails, it's not a PSBT
            return false;
        }
    }
}