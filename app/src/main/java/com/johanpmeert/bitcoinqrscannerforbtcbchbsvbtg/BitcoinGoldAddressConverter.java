package com.johanpmeert.bitcoinqrscannerforbtcbchbsvbtg;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Sha256Hash;
import org.bouncycastle.util.encoders.Hex;

import wf.bitcoin.krotjson.HexCoder;

public class BitcoinGoldAddressConverter {

    public static String BTCtoBTG(String btc) {
        byte[] btcDecode = Base58.decodeChecked(btc);
        if (btcDecode[0] == 0x00) btcDecode[0] = 0x26;
        else if (btcDecode[0] == 0x05) btcDecode[0] = 0x17;
        else return "Error";
        String base = HexCoder.encode(btcDecode);
        String checksum = HexCoder.encode(Sha256Hash.hashTwice(btcDecode));
        String endResult = base + checksum.substring(0, 8);
        return Base58.encode(Hex.decode(endResult));
    }

    public static String BTGtoBTC(String btg) {
        byte[] btcDecode = Base58.decodeChecked(btg); // base 58 decode + delete checksum (4 last bytes)
        if (btcDecode[0] == 0x26) btcDecode[0] = 0x00;
        else if (btcDecode[0] == 0x17) btcDecode[0] = 0x05;
        else return "Error";
        String base = HexCoder.encode(btcDecode);
        String checksum = HexCoder.encode(Sha256Hash.hashTwice(btcDecode));
        String endResult = base + checksum.substring(0, 8);
        return Base58.encode(Hex.decode(endResult));
    }

}
