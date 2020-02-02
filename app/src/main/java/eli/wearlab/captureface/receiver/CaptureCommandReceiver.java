package eli.wearlab.captureface.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient;

import eli.wearlab.captureface.MainActivity;

public class CaptureCommandReceiver extends BroadcastReceiver {

    private MainActivity mainAct;

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction() != null && intent.getAction().equals(VuzixSpeechClient.ACTION_VOICE_COMMAND)){
            if(intent.getExtras() != null){
                String phrase = intent.getStringExtra(VuzixSpeechClient.PHRASE_STRING_EXTRA);
                if(phrase != null && phrase.equals("capture_face"))
                    mainAct.takeCapture();
            }
        }
    }

    public CaptureCommandReceiver(MainActivity main){
        mainAct = main;
        main.registerReceiver(this, new IntentFilter(VuzixSpeechClient.ACTION_VOICE_COMMAND));
        try {
            VuzixSpeechClient sc = new VuzixSpeechClient(main);
            sc.insertPhrase("capture face");
            VuzixSpeechClient.EnableRecognizer(main, true);
        } catch (Exception e) {
            Toast.makeText(main.getApplicationContext(), "Could not update vocab!", Toast.LENGTH_LONG).show();
        }
    }

    public void unregister() {
        try {
            mainAct.unregisterReceiver(this);
            Log.i("Debug", "Custom vocab removed");
            mainAct = null;
        }catch (Exception e) {
            Log.e("Debug", "Custom vocab died " + e.getMessage());
        }
    }
}
