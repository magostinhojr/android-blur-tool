package br.com.magostinhojr.blurtool;

/**
 * Created with Android Studio
 * User: marcelo.agostinho
 */

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;

import java.io.FileNotFoundException;


public class MainActivity extends Activity {


    final int REQUEST_IMAGE_GALLERY_CODE = 1;

    Button btnLoadImage;
    ImageView imageResult, imageDrawingPane;

    Uri imageSource;
    Bitmap selectedImageBitmap;
    Canvas selectedImageCanvas;
    Bitmap rectBitmapDrawingPane;
    Canvas rectcanvasDrawingPane;
    Rect rectToBlur = new Rect(0,0,0,0);
    ProjectPt startPt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnLoadImage = (Button)findViewById(R.id.main_activity_load_image_bt);
        imageResult = (ImageView)findViewById(R.id.main_activity_loaded_image);
        imageDrawingPane = (ImageView)findViewById(R.id.main_activity_blur_overlay);

        imageResult.setOnTouchListener(new OnTouchListener(){

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                int action = event.getAction();
                int x = (int) event.getX();
                int y = (int) event.getY();
                switch(action){
                    case MotionEvent.ACTION_DOWN:
                        startPt = projectXY((ImageView)v, selectedImageBitmap, x, y);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        drawOnRectProjectedBitMap((ImageView)v, selectedImageBitmap, x, y);
                        break;
                    case MotionEvent.ACTION_UP:
                        drawOnRectProjectedBitMap((ImageView) v, selectedImageBitmap, x, y);
                        blurSelectedRegion();
                        break;
                }

                return true;
            }});

    }

    public void rotateRect(Rect rect){
        //TODO Refactor Rotate to use Matrix Transform
        //When the rectangle is draw from right to left / bottom to top,
        //we got an illegalargumentexception because we cant create a Blur Rectangle Bitmap with
        //negative values.

        Rect tempRect = new Rect(rect);

        if(rect.width() < 0 && rect.height() < 0){
            rect.top = tempRect.bottom;
            rect.bottom = tempRect.top;
            rect.left = tempRect.right;
            rect.right = tempRect.left;
        } else if (rect.width() < 0){
            rect.left = tempRect.right;
            rect.right = tempRect.left;
        } else if (rect.height() < 0){
            rect.top = tempRect.bottom;
            rect.bottom = tempRect.top;
        }


    }

    private void blurSelectedRegion() {

        rotateRect(rectToBlur);

        if(rectToBlur.width() > 0 && rectToBlur.height() > 0) {
            final RenderScript mBlurScript = RenderScript.create(MainActivity.this);
            final ScriptIntrinsicBlur mIntrinsicScript = ScriptIntrinsicBlur.create(mBlurScript, Element.U8_4(mBlurScript));
            final Bitmap mBlurredBitmap = Bitmap.createBitmap(rectToBlur.width(), rectToBlur.height(), selectedImageBitmap.getConfig());
            final Bitmap blurSource = Bitmap.createBitmap(selectedImageBitmap, rectToBlur.left, rectToBlur.top, rectToBlur.width(), rectToBlur.height());
            final Allocation inAlloc = Allocation.createFromBitmap(mBlurScript, blurSource, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_GRAPHICS_TEXTURE);
            final Allocation outAlloc = Allocation.createFromBitmap(mBlurScript, mBlurredBitmap);


            mIntrinsicScript.setRadius(25.0f);
            mIntrinsicScript.setInput(inAlloc);
            mIntrinsicScript.forEach(outAlloc);
            outAlloc.copyTo(mBlurredBitmap);

            selectedImageCanvas.drawBitmap(mBlurredBitmap, rectToBlur.left, rectToBlur.top, null);
            rectcanvasDrawingPane.drawColor(Color.TRANSPARENT, Mode.CLEAR);
        }
    }


    public void openImageGallery(View view){
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_GALLERY_CODE);
    }

    class ProjectPt {
        int x;
        int y;

        ProjectPt(int tx, int ty){
            x = tx;
            y = ty;
        }
    }

    private ProjectPt projectXY(ImageView iv, Bitmap bm, int x, int y){
        if(x<0 || y<0 || x > iv.getWidth() || y > iv.getHeight()){
            return null;
        }else{
            int projectedX = (int)((double)x * ((double)bm.getWidth()/(double)iv.getWidth()));
            int projectedY = (int)((double)y * ((double)bm.getHeight()/(double)iv.getHeight()));

            return new ProjectPt(projectedX, projectedY);
        }
    }

    private void drawOnRectProjectedBitMap(ImageView iv, Bitmap bm, int x, int y){

        Log.d("Blur Tool", "drawOnRectProjectedBitmap");

        if(x<0 || y<0 || x > iv.getWidth() || y > iv.getHeight()){
            return;
        }else{
            int projectedX = (int)((double)x * ((double)bm.getWidth()/(double)iv.getWidth()));
            int projectedY = (int)((double)y * ((double)bm.getHeight()/(double)iv.getHeight()));

            rectcanvasDrawingPane.drawColor(Color.TRANSPARENT, Mode.CLEAR);

            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(3);
            rectcanvasDrawingPane.drawRect(startPt.x, startPt.y, projectedX, projectedY, paint);
            rectToBlur.set(startPt.x, startPt.y, projectedX, projectedY);
            imageDrawingPane.invalidate();

        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Bitmap tempBitmap;

        if(resultCode == RESULT_OK){
            switch (requestCode){
                case REQUEST_IMAGE_GALLERY_CODE:
                    imageSource = data.getData();

                    try {
                        tempBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageSource));

                        selectedImageBitmap = Bitmap.createBitmap(tempBitmap.getWidth(), tempBitmap.getHeight(), Config.ARGB_8888);
                        selectedImageCanvas = new Canvas(selectedImageBitmap);
                        selectedImageCanvas.drawBitmap(tempBitmap, 0, 0, null);

                        imageResult.setImageBitmap(selectedImageBitmap);

                        rectBitmapDrawingPane = Bitmap.createBitmap(tempBitmap.getWidth(), tempBitmap.getHeight(), Config.ARGB_8888);
                        rectcanvasDrawingPane = new Canvas(rectBitmapDrawingPane);
                        imageDrawingPane.setImageBitmap(rectBitmapDrawingPane);


                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    break;
            }
        }
    }
}
