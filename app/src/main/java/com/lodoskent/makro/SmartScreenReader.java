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
    public interface StructuredResult {
        void finished(com.google.mlkit.vision.text.Text document, String error);
    }

    /** Reads OCR lines AND bounding boxes for user-approved screen actions. */
    public static void readStructured(AccessibilityService service, StructuredResult callback) {
        if(Build.VERSION.SDK_INT<30){
            callback.finished(null,"Ekran okuma Android 11 veya üstünü gerektirir.");
            return;
        }
        try{
            service.takeScreenshot(Display.DEFAULT_DISPLAY,service.getMainExecutor(),
                new AccessibilityService.TakeScreenshotCallback() {
                    @Override public void onFailure(int errorCode){
                        callback.finished(null,"Android ekran görüntüsü hatası: "+errorCode);
                    }
                    @Override public void onSuccess(AccessibilityService.ScreenshotResult screenshot){
                        Bitmap copy=null;
                        HardwareBuffer buffer=screenshot.getHardwareBuffer();
                        try{
                            ColorSpace space=screenshot.getColorSpace();
                            if(space==null)space=ColorSpace.get(ColorSpace.Named.SRGB);
                            Bitmap original=Bitmap.wrapHardwareBuffer(buffer,space);
                            if(original==null)throw new IllegalStateException("Boş görüntü");
                            copy=original.copy(Bitmap.Config.ARGB_8888,false);
                        }catch(Exception e){
                            callback.finished(null,"Görüntü alınamadı: "+e.getClass().getSimpleName());
                        }finally{
                            if(buffer!=null)buffer.close();
                        }
                        if(copy==null)return;
                        final Bitmap image=copy;
                        final TextRecognizer reader=TextRecognition.getClient(
                            TextRecognizerOptions.DEFAULT_OPTIONS);
                        try{
                            reader.process(InputImage.fromBitmap(image,0))
                                .addOnSuccessListener(t->callback.finished(t,""))
                                .addOnFailureListener(e->callback.finished(null,
                                    "OCR tanıma hatası: "+e.getClass().getSimpleName()))
                                .addOnCompleteListener(done->{
                                    reader.close();
                                    image.recycle();
                                });
                        }catch(Exception e){
                            reader.close();
                            image.recycle();
                            callback.finished(null,"OCR başlatılamadı: "+e.getClass().getSimpleName());
                        }
                    }
                });
        }catch(Exception e){
            callback.finished(null,"Ekran yakalama reddedildi: "+e.getClass().getSimpleName());
        }
    }

}
