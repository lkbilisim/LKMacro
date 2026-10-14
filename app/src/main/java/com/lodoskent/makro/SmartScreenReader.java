package com.lodoskent.makro;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.hardware.HardwareBuffer;
import android.os.Build;
import android.view.Display;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

/**
 * One-shot on-device OCR using an explicitly enabled Accessibility Service.
 * Does not save screenshots or issue game gestures. Android can refuse capture,
 * e.g. secure screens. Callback always runs on the main executor.
 */
public final class SmartScreenReader {
    private SmartScreenReader() {}
    public interface Result {
        void finished(String extractedText, String error);
    }
    public static void read(AccessibilityService service, Result callback) {
        if(Build.VERSION.SDK_INT<30){
            callback.finished("","Ekran okuma Android 11 veya üstünü gerektirir.");
            return;
        }
        try {
            service.takeScreenshot(Display.DEFAULT_DISPLAY,service.getMainExecutor(),
                new AccessibilityService.TakeScreenshotCallback() {
                    @Override public void onFailure(int errorCode) {
                        callback.finished("","Android ekran görüntüsüne izin vermedi (kod "+errorCode+").");
                    }
                    @Override public void onSuccess(AccessibilityService.ScreenshotResult result) {
                        Bitmap bitmap=null;
                        HardwareBuffer buffer=result.getHardwareBuffer();
                        try {
                            ColorSpace space=result.getColorSpace();
                            if(space==null)space=ColorSpace.get(ColorSpace.Named.SRGB);
                            Bitmap hardware=Bitmap.wrapHardwareBuffer(buffer,space);
                            if(hardware==null)throw new IllegalStateException("Görüntü dönüştürülemedi");
                            bitmap=hardware.copy(Bitmap.Config.ARGB_8888,false);
                        } catch(Exception ex) {
                            callback.finished("","Görüntü hazırlanamadı: "+ex.getClass().getSimpleName());
                        } finally {
                            if(buffer!=null)buffer.close();
                        }
                        if(bitmap==null)return;
                        final Bitmap copy=bitmap;
                        TextRecognizer recognizer=TextRecognition.getClient(
                            TextRecognizerOptions.DEFAULT_OPTIONS);
                        try {
                            recognizer.process(InputImage.fromBitmap(copy,0))
                                .addOnSuccessListener(visionText->
                                    callback.finished(visionText.getText(),""))
                                .addOnFailureListener(error->
                                    callback.finished("","OCR başarısız: "+error.getClass().getSimpleName()))
                                .addOnCompleteListener(done->{
                                    recognizer.close();
                                    copy.recycle();
                                });
                        } catch(Exception ex){
                            recognizer.close();
                            copy.recycle();
                            callback.finished("","OCR çalıştırılamadı: "+ex.getClass().getSimpleName());
                        }
                    }
                });
        } catch(Exception ex) {
            callback.finished("","Ekran yakalama başlatılamadı: "+ex.getClass().getSimpleName());
        }
    }
}
