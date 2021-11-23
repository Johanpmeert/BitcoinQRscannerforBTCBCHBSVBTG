package com.johanpmeert.bitcoinqrscannerforbtcbchbsvbtg;

import static java.util.Locale.*;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Bech32;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Bech32utils {

    public static String bech32ToBitcoin(String address) {
        return Base58CheckEncode("00" + hexString5to8bit(byteArrayToHexString(Bech32.decode(address).data).substring(2)));
    }

    private static String hexString5to8bit(String convert) {
        StringBuilder binaryString = new StringBuilder();
        while (convert.length() > 0) {
            String int1String = convert.substring(0, 2);
            String int2String = Integer.toBinaryString(Integer.parseInt(int1String, 16));
            switch (int2String.length()) {
                case 1:
                    int2String = "0000" + int2String;
                    break;
                case 2:
                    int2String = "000" + int2String;
                    break;
                case 3:
                    int2String = "00" + int2String;
                    break;
                case 4:
                    int2String = "0" + int2String;
            }
            binaryString.append(int2String);
            convert = convert.substring(2);
        }
        return new BigInteger(binaryString.toString(), 2).toString(16).toUpperCase(ROOT);
    }

    private static String Base58CheckEncode(String address) {
        String base58encoded = "";
        byte[] checksum1 = hexStringToByteArray(address);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] checksum2 = digest.digest(checksum1);  // first SHA256 hash
            byte[] checksum3 = digest.digest(checksum2);  // second SHA256 hash
            String checksum4 = byteArrayToHexString(checksum3);
            address = address + checksum4.substring(0, 8);  // take the first 4 bytes of the double hash and add them at the end of the original hex string
            base58encoded = Base58.encode(hexStringToByteArray(address));  // encode with base58
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return base58encoded;
    }

    private static byte[] hexStringToByteArray(String hex) {
        hex = hex.length() % 2 != 0 ? "0" + hex : hex;
        byte[] b = new byte[hex.length() / 2];
        for (int i = 0; i < b.length; i++) {
            int index = i * 2;
            int v = Integer.parseInt(hex.substring(index, index + 2), 16);
            b[i] = (byte) v;
        }
        return b;
    }

    private static String byteArrayToHexString(byte[] bytes) {
        final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

}
