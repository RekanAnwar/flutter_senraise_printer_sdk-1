package com.senraise.senraise_printer;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.util.Log;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import recieptservice.com.recieptservice.PrinterInterface;

/**
 * SenraisePrinterPlugin
 */
public class SenraisePrinterPlugin implements FlutterPlugin, MethodCallHandler {
    private static final String RECEIPT_SERVICE_PACKAGE = "recieptservice.com.recieptservice";
    private static final String RECEIPT_SERVICE_CLASS = "recieptservice.com.recieptservice.service.PrinterService";
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel channel;
    PrinterInterface printerInterface;
    private FlutterPluginBinding pluginBinding;
    private boolean isBound;


    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            printerInterface = PrinterInterface.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            printerInterface = null;
        }
    };

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        pluginBinding = flutterPluginBinding;
        bindPrinterServiceIfAvailable();
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "senraise_printer");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        try {
            if (call.method.equals("isServiceAvailable")) {
                result.success(isPrinterAvailable());
            } else if (call.method.equals("getPlatformVersion")) {
                result.success("Android " + android.os.Build.VERSION.RELEASE);
            } else if (!ensurePrinterReady(result)) {
                return;
            } else if (call.method.equals("printEpson")) {
                printerInterface.printEpson(call.argument("data"));
                result.success("printEpson success");
            } else if (call.method.equals("printText")) {
                printerInterface.printText(call.argument("text"));
                result.success("printText success");
            } else if (call.method.equals("printPic")) {
                byte[]data=call.argument("pic");
                Bitmap bitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(data));
                printerInterface.printBitmap(bitmap);
                result.success("printPic success");
            } else if (call.method.equals("printBarCode")) {
                printerInterface.printBarCode(call.argument("data"),call.argument("symbology"),call.argument("height"),call.argument("width"));
                result.success("printBarCode success");
            } else if (call.method.equals("printQRCode")) {
                printerInterface.printQRCode(call.argument("data"),call.argument("modulesize"),call.argument("errorlevel"));
                result.success("printQRCode success");
            } else if (call.method.equals("setAlignment")) {
                printerInterface.setAlignment(call.argument("alignment"));
                result.success("setAlignment success");
            } else if (call.method.equals("setTextSize")) {
                printerInterface.setTextSize(((Double)call.argument("textSize")).floatValue());
                result.success("setTextSize success");
            } else if (call.method.equals("nextLine")) {
                printerInterface.nextLine(call.argument("line"));
                result.success("nextLine success");
            } else if (call.method.equals("setTextBold")) {
                printerInterface.setTextBold(Boolean.TRUE.equals(call.argument("bold")));
                result.success("setTextBold success");
            }else if (call.method.equals("setDark")) {
                printerInterface.setDark(call.argument("value"));
                result.success("setDark success");
            }else if (call.method.equals("setLineHeight")) {
                printerInterface.setLineHeight(((Double)call.argument("lineHeight")).floatValue());
                result.success("setLineHeight success");
            }else if (call.method.equals("setTextDoubleWidth")) {
                printerInterface.setTextDoubleWidth(Boolean.TRUE.equals(call.argument("enable")));
                result.success("setTextDoubleWidth success");
            }else if (call.method.equals("setTextDoubleHeight")) {
                printerInterface.setTextDoubleHeight(Boolean.TRUE.equals(call.argument("enable")));
                result.success("setTextDoubleHeight success");
            }else if (call.method.equals("setCode")) {
                printerInterface.setCode(call.argument("code"));
                result.success("setCode success");
            }else {
                result.notImplemented();
            }
        } catch (Exception e) {
            Log.e("Failed to print",e.getMessage());
            result.error("Failed to print", e.getMessage(), null);
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        if (isBound) {
            binding.getApplicationContext().unbindService(serviceConnection);
            isBound = false;
        }
        printerInterface = null;
        channel.setMethodCallHandler(null);
    }

    private boolean ensurePrinterReady(@NonNull Result result) {
        bindPrinterServiceIfAvailable();

        if (printerInterface == null) {
            result.error("printer_unavailable", "Senraise printer service is not available on this device", null);
            return false;
        }

        return true;
    }

    private boolean isPrinterAvailable() {
        return hasPrinterService() && printerInterface != null;
    }

    private boolean hasPrinterService() {
        if (pluginBinding == null) {
            return false;
        }

        Intent intent = new Intent();
        intent.setClassName(RECEIPT_SERVICE_PACKAGE, RECEIPT_SERVICE_CLASS);
        PackageManager packageManager = pluginBinding.getApplicationContext().getPackageManager();
        return packageManager.resolveService(intent, 0) != null;
    }

    private void bindPrinterServiceIfAvailable() {
        if (pluginBinding == null || isBound || !hasPrinterService()) {
            return;
        }

        Intent intent = new Intent();
        intent.setClassName(RECEIPT_SERVICE_PACKAGE, RECEIPT_SERVICE_CLASS);

        try {
            pluginBinding.getApplicationContext().startService(intent);
            isBound = pluginBinding.getApplicationContext()
                    .bindService(intent, serviceConnection, Service.BIND_AUTO_CREATE);
        } catch (Exception e) {
            Log.e("SenraisePrinter", "Failed to bind printer service", e);
            isBound = false;
            printerInterface = null;
        }
    }
}
