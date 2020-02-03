package eli.wearlab.captureface;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.vuzix.connectivity.sdk.Connectivity;
import com.vuzix.connectivity.sdk.Device;
import com.vuzix.hud.actionmenu.ActionMenuActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import eli.wearlab.captureface.receiver.CaptureCommandReceiver;

public class MainActivity extends ActionMenuActivity {

    private CaptureCommandReceiver captureReceiver;
    private Connectivity connection;
    static final int REQUEST_TAKE_PHOTO = 1;
    private ImageView image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        captureReceiver = new CaptureCommandReceiver(this);
        image = findViewById(R.id.imageView);
        image.setVisibility(ImageView.INVISIBLE);
        Context context = getContext();
        connection = Connectivity.get(context);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        captureReceiver.unregister();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Toast.makeText(this, "RESULT", Toast.LENGTH_SHORT).show();
        if (requestCode == 1 && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            if(imageBitmap == null) {
                Toast.makeText(this, "Couldn't retrieve picture!", Toast.LENGTH_LONG).show();
                return;
            }

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
            byte[] byteData = bytes.toByteArray();

            File photoFile;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e("Debug", "Couldn't create file!");
                return;
            }
            //write the bytes in file
            FileOutputStream fo;
            try {
                fo = new FileOutputStream(photoFile);
                fo.write(byteData);
                fo.close();
            } catch (Exception e) {
                Log.e("Debug", "FILE IO EXCEPTION");
                return;
            }

            image.setImageBitmap(imageBitmap);
            image.setVisibility(View.VISIBLE);
            galleryAddPic(photoFile);

            sendImage(byteData);
        }
    }

    public void takeCapture(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
        }
    }

    @SuppressLint("SimpleDateFormat")
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        // Save a file: path for use with ACTION_VIEW intents
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }

    private void galleryAddPic(File photoFile) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(photoFile);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void sendImage(byte[] imageData){
        //Do connectivity stuff here

        if(!connection.isLinked()){
            Log.e("Debug", "Cannot send image: No connection found.");
            return;
        }

        String SEND_ACTION = "eli.wearlab.captureface.SEND_IMAGE";
        Intent sendBroadcast = new Intent(SEND_ACTION);
        sendBroadcast.setPackage("eli.wearlab.capturefacecompanion");
        sendBroadcast.putExtra("data", imageData);

        Device device = connection.getDevice();

        connection.sendOrderedBroadcast(device, sendBroadcast, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (getResultCode() == RESULT_OK) {
                    // do something with results
                    //UNPACK RETURN DATA AND DISPLAY TO USER
                    Bundle returnExtras = intent.getExtras();

                    Log.d("Debug", "------------------------------------");
                    if (returnExtras != null) {
                        for (String key : returnExtras.keySet()) {
                            Log.d("Debug", key + " : " + (returnExtras.get(key) != null ? returnExtras.get(key) : "NULL"));
                        }
                    }
                    Log.d("Debug", "------------------------------------");

                    /*if(returnExtras != null && returnExtras.getString("toast") != null)
                        Log.d("Debug", "Return: " + returnExtras.getString("toast"));
                    else
                        Log.d("Debug", "Couldn't retrieve return data");*/
                }
            }
        });

    }
}
