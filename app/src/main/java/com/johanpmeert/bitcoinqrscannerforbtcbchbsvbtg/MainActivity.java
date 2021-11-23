package com.johanpmeert.bitcoinqrscannerforbtcbchbsvbtg;

/*-
 * -----------------------LICENSE_START-----------------------
 * QR scanner for bitcoin (BTC), bitcoin cash (BCH), bitcoin sv (BSV) and bitcoin gold (BTG)
 * %%
 * Copyright (C) 2020 Johan MEERT
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------LICENSE_END-----------------------
 */

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.annotations.SerializedName;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Bech32;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import static com.johanpmeert.bitcoinqrscannerforbtcbchbsvbtg.BitcoinAddressValidator.validateBitcoinAddress;
import static com.johanpmeert.bitcoinqrscannerforbtcbchbsvbtg.BitcoinCashAddressConverter.toCashAddress;
import static com.johanpmeert.bitcoinqrscannerforbtcbchbsvbtg.BitcoinCashAddressConverter.toLegacyAddress;
import static com.johanpmeert.bitcoinqrscannerforbtcbchbsvbtg.BitcoinGoldAddressConverter.BTCtoBTG;
import static com.johanpmeert.bitcoinqrscannerforbtcbchbsvbtg.BitcoinGoldAddressConverter.BTGtoBTC;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_QR = 42574;  // random number
    private static final String BTC_TEST_ADDRESS = "15VEJBFyCBSFpnkxiLZXPCoo2x72ACwHpt";  // test address, should contain 10 BTC/BCH/BSV/BTG :)
    private volatile boolean usePublicServerBoolean = true;
    EditText bitcoinAddressPreviewEditText;
    TextView statusTextView;
    TextView btcPreviewTextView, bchnPreviewTextView, bsvPreviewTextView, btgPreviewTextView;
    TextView btcValueTextView, bchnValueTextView, bsvValueTextView, btgValueTextView;
    TextView btcUsdTextView, bchnUsdTextView, bsvUsdTextView, btgUsdTextView, totalValueTextView;
    Button getValueButton, copyBtcButton, copyBchButton, copyBsvButton, copyBtgButton;
    private volatile BigDecimal cgBTC = new BigDecimal("1.0");
    private volatile BigDecimal cgBCH = new BigDecimal("1.0");
    private volatile BigDecimal cgBSV = new BigDecimal("1.0");
    private volatile BigDecimal cgBTG = new BigDecimal("1.0");

    enum BtcAddressStatus {INVALID, BTCFORMAT, BECH32FORMAT}

    enum CoinType {
        BTC("BTC"), BCHN("BCHN"), BSV("BSV"), BTG("BTG");
        private final String type;

        CoinType(String type) {
            this.type = type;
        }
    }

    ActivityResultLauncher<Intent> scanActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode()==Activity.RESULT_OK){
                        Intent data = result.getData();
                        String qrResult = data.getStringExtra("QR");
                        Log.i("QR-code: ", qrResult);
                        if (qrResult != null) {
                            Log.i("QR_code_returned: ", qrResult);
                            bitcoinAddressPreviewEditText.setText(qrResult);
                            statusTextView.setText(R.string.QRscansuccess);
                            statusTextView.setTextColor(Color.GREEN);
                            activateAllCopyButtons();
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        bitcoinAddressPreviewEditText = findViewById(R.id.editTextQRCode);
        statusTextView = findViewById(R.id.textViewStatus);
        btcPreviewTextView = findViewById(R.id.textViewBTCaddress);
        bchnPreviewTextView = findViewById(R.id.textViewBCHaddress);
        bsvPreviewTextView = findViewById(R.id.textViewBSVaddress);
        btgPreviewTextView = findViewById(R.id.textViewBTGaddress);
        btcValueTextView = findViewById(R.id.textViewBTCvalue);
        bchnValueTextView = findViewById(R.id.textViewBCHvalue);
        bsvValueTextView = findViewById(R.id.textViewBSVvalue);
        btgValueTextView = findViewById(R.id.textViewBTGvalue);
        bitcoinAddressPreviewEditText.setText(BTC_TEST_ADDRESS);
        getValueButton = findViewById(R.id.buttonGetValue);
        copyBtcButton = findViewById(R.id.buttonCopyBTC);
        copyBchButton = findViewById(R.id.buttonCopyBCH);
        copyBsvButton = findViewById(R.id.buttonCopyBSV);
        copyBtgButton = findViewById(R.id.buttonCopyBTG);
        btcUsdTextView = findViewById(R.id.btcusd);
        bchnUsdTextView = findViewById(R.id.bchnusd);
        bsvUsdTextView = findViewById(R.id.bsvusd);
        btgUsdTextView = findViewById(R.id.btgusd);
        totalValueTextView = findViewById(R.id.totalvalue);
        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        GetAllExchangeRates();
    }

    public void OnClickScan(View V) {
        Intent intent = new Intent(this, Qrscanner.class);
        scanActivityResultLauncher.launch(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_items, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (usePublicServerBoolean) {
            menu.getItem(0).setChecked(true);
            menu.getItem(1).setChecked(false);
        } else {
            menu.getItem(0).setChecked(false);
            menu.getItem(1).setChecked(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.usePublicServers:
                item.setChecked(true);
                usePublicServerBoolean = true;
                return true;
            case R.id.useJpmServer:
                item.setChecked(true);
                usePublicServerBoolean = false;
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void OnClickPaste(View V) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if ((clipboard.hasPrimaryClip()) && (clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))) {  // clipboard not empty & contains text
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            String pasteAddress = item.getText().toString();
            bitcoinAddressPreviewEditText.setText(pasteAddress);
            statusTextView.setText(R.string.Pastesuccess);
            statusTextView.setTextColor(Color.GREEN);
        } else {
            statusTextView.setText(R.string.Clipempty);
            statusTextView.setTextColor(Color.RED);
        }
    }

    public void OnClickCheckAddress(View V) {
        String testAddress = bitcoinAddressPreviewEditText.getText().toString();
        if (simpleAddressIsValid(testAddress)) {
            if (isBTGAddress(testAddress)) {
                statusTextView.setText(R.string.validBTG);
                statusTextView.setTextColor(Color.GREEN);
                String legacyAddress = BTGtoBTC(testAddress);
                btcPreviewTextView.setText(legacyAddress);
                bchnPreviewTextView.setText(toCashAddress(legacyAddress));
                bsvPreviewTextView.setText(legacyAddress);
                btgPreviewTextView.setText(testAddress);
            } else if (isBTCAddress(testAddress) == BtcAddressStatus.BTCFORMAT) {
                statusTextView.setText(R.string.validBTC);
                statusTextView.setTextColor(Color.GREEN);
                btcPreviewTextView.setText(testAddress);
                bchnPreviewTextView.setText(toCashAddress(testAddress));
                bsvPreviewTextView.setText(testAddress);
                btgPreviewTextView.setText(BTCtoBTG(testAddress));
            } else if (isBTCAddress(testAddress) == BtcAddressStatus.BECH32FORMAT) {
                statusTextView.setText(R.string.validB32);
                statusTextView.setTextColor(Color.GREEN);
                testAddress = testAddress.toLowerCase();
                btcPreviewTextView.setText(testAddress);
                String originalBTC = Bech32utils.bech32ToBitcoin(testAddress);
                btgPreviewTextView.setText(BTCtoBTG(originalBTC));
                bchnPreviewTextView.setText(toCashAddress(originalBTC));
                bsvPreviewTextView.setText(originalBTC);
            } else if (isBCHAddress(testAddress)) {
                statusTextView.setText(R.string.validBCH);
                statusTextView.setTextColor(Color.GREEN);
                String legacyAddress = toLegacyAddress(testAddress);
                btcPreviewTextView.setText(legacyAddress);
                bchnPreviewTextView.setText(testAddress);
                bsvPreviewTextView.setText(legacyAddress);
                btgPreviewTextView.setText(BTCtoBTG(legacyAddress));
            } else {
                statusTextView.setText(R.string.invalidaddress);
                statusTextView.setTextColor(Color.RED);
                return;
            }
            getValueButton.setEnabled(true);
            activateAllCopyButtons();
        }
    }

    public void OnClickGetValue(View V) {
        if (usePublicServerBoolean) {
            UpdateAllValueFromWeb();
        } else {
            // Or getting response from my private server:
            UpdateAllValueFromJPM();
        }
    }

    public void OnClickClear(View V) {
        bitcoinAddressPreviewEditText.setText("");
        statusTextView.setText(R.string.previewcleared);
        statusTextView.setTextColor(Color.GREEN);
        getValueButton.setEnabled(false);
        btcPreviewTextView.setText("");
        bchnPreviewTextView.setText("");
        bsvPreviewTextView.setText("");
        btgPreviewTextView.setText("");
        btcValueTextView.setText("0.0");
        bchnValueTextView.setText("0.0");
        bsvValueTextView.setText("0.0");
        btgValueTextView.setText("0.0");
        totalValueTextView.setText("0.00 $");
        deactivateAllCopyButtons();
    }

    public void OnClickCopyBTC(View V) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("btc address", btcPreviewTextView.getText().toString());
        clipboard.setPrimaryClip(clip);
        statusTextView.setText(R.string.copyclip);
        statusTextView.setTextColor(Color.GREEN);
    }

    public void OnClickCopyBCH(View V) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("btc address", bchnPreviewTextView.getText().toString());
        clipboard.setPrimaryClip(clip);
        statusTextView.setText(R.string.copyclip);
        statusTextView.setTextColor(Color.GREEN);
    }

    public void OnClickCopyBSV(View V) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("btc address", bsvPreviewTextView.getText().toString());
        clipboard.setPrimaryClip(clip);
        statusTextView.setText(R.string.copyclip);
        statusTextView.setTextColor(Color.GREEN);
    }

    public void OnClickCopyBTG(View V) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("btc address", btgPreviewTextView.getText().toString());
        clipboard.setPrimaryClip(clip);
        statusTextView.setText(R.string.copyclip);
        statusTextView.setTextColor(Color.GREEN);
    }

    public void OnClickQuit(View V) {
        finish();
        System.exit(0);
    }


    // all other Methods from here

    private boolean simpleAddressIsValid(String address) {
        // check 1: IsEmpty ?
        if (bitcoinAddressPreviewEditText.getText().toString().isEmpty()) {
            statusTextView.setText(R.string.addressprevempty);
            statusTextView.setTextColor(Color.RED);
            return false;
        } else {
            address = address.trim();  // remove spaces
            bitcoinAddressPreviewEditText.setText(address);
        }
        // check 2: contains illegal characters ?
        if (!address.matches("-?[:0-9a-zA-HJ-NP-Z]+")) {
            // can contain only base58 (base64 without 0lIO) and also colon ':' for 'bitcoincash:address', 0 is also included because of bitcoin cash, bech32 address compatible
            statusTextView.setText(R.string.illegalchar);
            statusTextView.setTextColor(Color.RED);
            return false;
        }
        if (address.contains(":")) {
            if (!address.contains("bitcoincash:")) {
                statusTextView.setText(R.string.colonbch);
                statusTextView.setTextColor(Color.RED);
                return false;
            }
        }
        statusTextView.setText(R.string.addrok);
        statusTextView.setTextColor(Color.GREEN);
        return true;
    }

    private BtcAddressStatus isBTCAddress(String address) {
        if (validateBitcoinAddress(address))
            return BtcAddressStatus.BTCFORMAT;  // check for classic bitcoin address
        try {
            Bech32.Bech32Data b32d = Bech32.decode(address);  // check for bech32 address
            return BtcAddressStatus.BECH32FORMAT;
        } catch (AddressFormatException.InvalidCharacter | AddressFormatException.InvalidDataLength | AddressFormatException.InvalidPrefix | AddressFormatException.InvalidChecksum ae) {
            Log.e("Bech32 conversion:", ae.toString());
        }
        return BtcAddressStatus.INVALID;
    }

    private boolean isBCHAddress(String address) {
        if (address.length() != 54) return false;
        if (!address.contains("bitcoincash:")) return false;
        return address.matches("-?[:02-9a-z]+");
    }

    private boolean isBTGAddress(String address) {
        return (address.charAt(0) == 'G') || (address.charAt(0) == 'A');
    }

    private void activateAllCopyButtons() {
        copyBtcButton.setEnabled(true);
        copyBchButton.setEnabled(true);
        copyBsvButton.setEnabled(true);
        copyBtgButton.setEnabled(true);
    }

    private void deactivateAllCopyButtons() {
        copyBtcButton.setEnabled(false);
        copyBchButton.setEnabled(false);
        copyBsvButton.setEnabled(false);
        copyBtgButton.setEnabled(false);
    }

    /**
     * Gets the balance from my private server, interfacing a MySQL server through a Http+JSON backend
     * Added a network_security_config XML file to allow HTTP access (needed from Android 9 and up)
     * Update: removed old Asyntask method, replaced by a retrofit, rxjava, rxandroid threaded method
     */

    public void UpdateAllValueFromWeb() {
        final String testAddressBTC = btcPreviewTextView.getText().toString();
        final String testAddressBSV = bsvPreviewTextView.getText().toString();
        final String testAddressBTG = btgPreviewTextView.getText().toString();

        String baseURL = "https://chain.api.btc.com/v3/address/";
        Retrofit retrofit1 = new Retrofit.Builder()
                .baseUrl(baseURL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
        ApiServiceBch apiService1 = retrofit1.create(ApiServiceBch.class);
        Single<BchValue> coinData1 = apiService1.getCoinData(testAddressBTC);
        coinData1.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<BchValue>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull BchValue bchValue) {
                        btcValueTextView.setText(bchValue.data.balance.divide(new BigDecimal("1e8")).toString());
                        CalculateTotals();
                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                        btcValueTextView.setText("0.0");
                    }
                });

        baseURL = "https://bch-chain.api.btc.com/v3/address/";
        Retrofit retrofit2 = new Retrofit.Builder()
                .baseUrl(baseURL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
        ApiServiceBch apiService2 = retrofit2.create(ApiServiceBch.class);
        Single<BchValue> coinData2 = apiService2.getCoinData(testAddressBSV);
        coinData2.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<BchValue>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull BchValue bchValue) {
                        bchnValueTextView.setText(bchValue.data.balance.divide(new BigDecimal("1e8")).toString());
                        CalculateTotals();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        bchnValueTextView.setText("0.00");
                    }
                });

        baseURL = "https://api.whatsonchain.com/v1/bsv/main/address/";
        Retrofit retrofit3 = new Retrofit.Builder()
                .baseUrl(baseURL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
        ApiServiceBsv apiService3 = retrofit3.create(ApiServiceBsv.class);
        Single<BsvValue> coinData3 = apiService3.getCoinData(testAddressBSV);
        coinData3.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<BsvValue>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull BsvValue bsvValue) {
                        bsvValueTextView.setText(bsvValue.confirmed.divide(new BigDecimal("1e8")).toString());
                        CalculateTotals();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        bsvValueTextView.setText("0.0");
                    }
                });

        baseURL = "https://explorer.bitcoingold.org/insight-api/addr/";
        Retrofit retrofit4 = new Retrofit.Builder()
                .baseUrl(baseURL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
        ApiServiceBtg apiService4 = retrofit4.create(ApiServiceBtg.class);
        Single<BtgValue> coinData4 = apiService4.getCoinData(testAddressBTG);
        coinData4.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<BtgValue>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull BtgValue btgValue) {
                        btgValueTextView.setText(btgValue.balance.toString());
                        CalculateTotals();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        btgValueTextView.setText("0.00");
                    }
                });

    }


    public interface ApiServiceBch {
        @GET("{address}")
        Single<BchValue> getCoinData(@Path("address") String coinAddress);
    }

    public interface ApiServiceBsv {
        @GET("{address}/balance")
        Single<BsvValue> getCoinData(@Path("address") String coinAddress);
    }

    public interface ApiServiceBtg {
        @GET("{address}")
        Single<BtgValue> getCoinData(@Path("address") String coinAddress);
    }

    private class BchValue {
        BchData data;
        int err_code;
        int err_no;
        String message;
        String status;
    }

    private class BchData {
        String address;
        BigDecimal received;
        BigDecimal sent;
        BigDecimal balance;
        int tx_count;
        int unconfirmed_tx_count;
        BigDecimal unconfirmed_received;
        BigDecimal unconfirmed_sent;
        int unspent_tx_count;
    }

    private static class BsvValue {
        BigDecimal confirmed;
        BigDecimal unconfirmed;
    }

    private static class BtgValue {
        String addStr;
        BigDecimal balance;
        BigDecimal balanceSat;
        BigDecimal totalReceived;
        BigDecimal totalReceivedSat;
        BigDecimal totalSent;
        BigDecimal totalSentSat;
        BigDecimal unconfirmedBalance;
        BigDecimal unconfirmedBalanceSat;
        int unconfirmedTxApperances;
        int txApperances;
        String[] transactions;
    }

    /**
     * Gets the balance from my private server, interfacing a MySQL server through a Http+JSON backend
     * Added a network_security_config XML file to allow non HTTPS access (needed from Android 9 and up)
     * Update: removed old Asyntask method, replaced by a retrofit, rxjava, rxandroid threaded method
     */

    public void UpdateAllValueFromJPM() {
        final String baseURL = "http://johanpmeert.ddns.net:8100/balance/";
        final String testAddressBTC = btcPreviewTextView.getText().toString();
        final String testAddressBCH = bchnPreviewTextView.getText().toString();
        final String testAddressBSV = bsvPreviewTextView.getText().toString();
        final String testAddressBTG = btgPreviewTextView.getText().toString();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseURL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
        ApiService apiService = retrofit.create(ApiService.class);

        Single<JPMbalance> coinData1 = apiService.getCoinData(CoinType.BTC.type, testAddressBTC);
        Single<JPMbalance> coinData2 = apiService.getCoinData(CoinType.BCHN.type, testAddressBCH);
        Single<JPMbalance> coinData3 = apiService.getCoinData(CoinType.BSV.type, testAddressBSV);
        Single<JPMbalance> coinData4 = apiService.getCoinData(CoinType.BTG.type, testAddressBTG);

        coinData1.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<JPMbalance>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull JPMbalance jpmbalance) {
                        btcValueTextView.setText(jpmbalance.balance.toString());
                        CalculateTotals();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        btcValueTextView.setText("0.0");
                    }
                });
        coinData2.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<JPMbalance>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull JPMbalance jpmbalance) {
                        bchnValueTextView.setText(jpmbalance.balance.toString());
                        CalculateTotals();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        bchnValueTextView.setText("0.0");
                    }
                });
        coinData3.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<JPMbalance>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull JPMbalance jpmbalance) {
                        bsvValueTextView.setText(jpmbalance.balance.toString());
                        CalculateTotals();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        bsvValueTextView.setText("0.0");
                    }
                });
        coinData4.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<JPMbalance>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull JPMbalance jpmbalance) {
                        btgValueTextView.setText(jpmbalance.balance.toString());
                        CalculateTotals();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        btgValueTextView.setText("0.0");
                    }
                });
    }

    public interface ApiService {
        @GET("{type}={address}")
        Single<JPMbalance> getCoinData(@Path("type") String coinType,
                                       @Path("address") String coinAddress);
    }

    public void CalculateTotals() {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        BigDecimal btctotal = cgBTC.multiply(new BigDecimal(btcValueTextView.getText().toString()));
        BigDecimal bchntotal = cgBCH.multiply(new BigDecimal(bchnValueTextView.getText().toString()));
        BigDecimal bsvtotal = cgBSV.multiply(new BigDecimal(bsvValueTextView.getText().toString()));
        BigDecimal btgtotal = cgBTG.multiply(new BigDecimal(btgValueTextView.getText().toString()));
        btcUsdTextView.setText(df.format(btctotal) + " $");
        bchnUsdTextView.setText(df.format(bchntotal) + " $");
        bsvUsdTextView.setText(df.format(bsvtotal) + " $");
        btgUsdTextView.setText(df.format(btgtotal) + " $");
        totalValueTextView.setText(df.format(btctotal.add(bchntotal).add(bsvtotal).add(btgtotal)) + " $");
    }

    public void GetAllExchangeRates() {
        String baseURL = "https://api.coingecko.com/api/v3/simple/";
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseURL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
        ApiServiceValueCG apiService = retrofit.create(ApiServiceValueCG.class);
        Single<RawCoingeckoData2> coinData = apiService.getCoinData();
        coinData.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<RawCoingeckoData2>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull RawCoingeckoData2 rawCoingeckoData) {
                        cgBTC = rawCoingeckoData.bitcoin.usd;
                        cgBCH = rawCoingeckoData.bitcoinbch.usd;
                        cgBSV = rawCoingeckoData.bitcoinbsv.usd;
                        cgBTG = rawCoingeckoData.bitcoingold.usd;
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                    }
                });
    }

    public interface ApiServiceValueCG {
        @GET("price?ids=bitcoin,bitcoin-cash,bitcoin-cash-sv,bitcoin-gold&vs_currencies=usd")
        Single<RawCoingeckoData2> getCoinData();
    }

    private static class RawCoingeckoData2 {
        @SerializedName("bitcoin-gold")
        cgbtg bitcoingold;
        @SerializedName("bitcoin")
        cgbtc bitcoin;
        @SerializedName("bitcoin-cash-sv")
        cgbsv bitcoinbsv;
        @SerializedName("bitcoin-cash")
        cgbch bitcoinbch;
    }

    private static class cgbtg {
        BigDecimal usd;
    }

    private static class cgbtc {
        BigDecimal usd;
    }

    private static class cgbsv {
        BigDecimal usd;
    }

    private static class cgbch {
        BigDecimal usd;
    }

    private static class JPMbalance {
        public BigDecimal balance;
        public CoinType currency;
        public int blockheight;
        public String errortype;
    }

}
