package glasscam.viewer.stack;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.ortiz.touch.TouchImageView;
import com.hazia.glasscam.R;

import glasscam.ActivityAbstract;
import glasscam.jni.DngStack;
import glasscam.viewer.dngconvert.DngConvertingFragment;


/**
 * Created by troop on 25.10.2016.
 */

public class DngStackActivity extends ActivityAbstract
{
    private String[] filesToStack = null;
    private Button stackButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dngstackactivity);
        stackButton = findViewById(R.id.button_dngstack);
        stackButton.setOnClickListener(v -> {
            if (filesToStack != null) {
                stackButton.setClickable(false);
                // Native DNG merge (DngStacker.cpp: reads+writes multiple full-resolution TIFF
                // strips) is not cheap - run it off the UI thread to avoid an ANR.
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        DngStack stack = new DngStack(filesToStack);
                        stack.StartStack(getApplicationContext());
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        stackButton.setBackgroundResource(R.drawable.stack_done);
                    }
                }.execute();
            }
        });
        TouchImageView imageView = findViewById(R.id.imageview_dngstack);
        filesToStack = getIntent().getStringArrayExtra(DngConvertingFragment.EXTRA_FILESTOCONVERT);
        if (filesToStack != null)
            ((TextView)findViewById(R.id.rawList)).setText(filesToStack.length+"");


    }

    @Override
    protected void setContentToView() {

    }

}
