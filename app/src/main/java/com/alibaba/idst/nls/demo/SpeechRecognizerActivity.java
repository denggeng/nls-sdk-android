package com.alibaba.idst.nls.demo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.idst.token.AccessToken;
import com.alibaba.idst.util.NlsClient;
import com.alibaba.idst.util.SpeechRecognizer;
import com.alibaba.idst.util.SpeechRecognizerCallback;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import static android.media.AudioRecord.STATE_UNINITIALIZED;

public class SpeechRecognizerActivity extends AppCompatActivity implements SpeechRecognizerCallback{
    private static final String TAG="AliSpeechDemo";

    private Button button;
    private EditText mFullEdit;
    private EditText mResultEdit;
    private NlsClient client;
    private SpeechRecognizer speechRecognizer;
    private RecordTask recordTask;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech_recognizer);
        button = (Button) findViewById(R.id.button);
        mFullEdit = (EditText) findViewById(R.id.editText2);
        mResultEdit = (EditText) findViewById(R.id.editText);

        final Context t = this.getApplicationContext();
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        startRecognizer(view);
                        return true;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        break;
                    }
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP: {
                        stopRecognizer(view);
                        return true;
                    }
                }
                return false;
            }
        });

        //第一步，创建client实例，client只需要创建一次，可以用它多次创建recognizer
        client = new NlsClient();
        Log.i(TAG, "Simple activity loaded!");
    }

    @Override
    public void onDestroy(){
        if (speechRecognizer != null){
            speechRecognizer.stop();
        }

        // 最终，退出时释放client
        client.release();
        super.onDestroy();
    }

    /**
     * 启动录音和识别
     * @param view
     */
    public void startRecognizer(View view){
        mFullEdit.setText("");
        mResultEdit.setText("");

        // 第二步，新建识别回调类

        // 第三步，创建识别request
        speechRecognizer = client.createRecognizerRequest(this);
        // 第四步，设置相关参数
        // Token有有效期，请使用https://help.aliyun.com/document_detail/72153.html 动态生成token
        speechRecognizer.setToken("");
        // 请使用阿里云语音服务管控台(https://nls-portal.console.aliyun.com/)生成您的appkey
        speechRecognizer.setAppkey("");
        // 设置返回中间结果，更多参数请参考官方文档
        speechRecognizer.enableIntermediateResult(false);
        speechRecognizer.enableInverseTextNormalization(true);
        speechRecognizer.enableVoiceDetection(true);
        speechRecognizer.setMaxStartSilence(3000);
        speechRecognizer.setMaxEndSilence(600);
        speechRecognizer.start();

        //启动录音线程
        recordTask = new RecordTask(this);
        recordTask.execute();
    }

    /**
     * 停止录音和识别
     * @param view
     */
    public void stopRecognizer(View view){
        // 停止录音
        Log.i(TAG, "Stoping recognizer...");
        recordTask.stop();
        speechRecognizer.stop();
    }

    // 语音识别回调类，用户在这里得到语音识别结果，加入您自己的业务处理逻辑
    // 注意不要在回调方法里执行耗时操作
    @Override
    public void onRecognizedStarted(String msg, int code)
    {
        Log.d(TAG,"OnRecognizedStarted " + msg + ": " + String.valueOf(code));
    }

    // 请求失败
    @Override
    public void onTaskFailed(String msg, int code)
    {
        Log.d(TAG,"OnTaskFailed: " + msg + ": " + String.valueOf(code));
        recordTask.stop();
        speechRecognizer.stop();
        final String fullResult = msg;
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String result = null;
                if (!fullResult.equals("")){
                    JSONObject jsonObject = JSONObject.parseObject(fullResult);
                    if (jsonObject.containsKey("payload")){
                        result = jsonObject.getJSONObject("payload").getString("result");
                    }
                }
                mFullEdit.setText(fullResult);
                mResultEdit.setText(result);
            }
        });
    }

    // 识别返回中间结果，只有enableIntermediateResult(true)时才会回调
    @Override
    public void onRecognizedResultChanged(final String msg, int code)
    {
        Log.d(TAG,"OnRecognizedResultChanged " + msg + ": " + String.valueOf(code));
        final String fullResult = msg;
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String result = null;
                if (!fullResult.equals("")){
                    JSONObject jsonObject = JSONObject.parseObject(fullResult);
                    if (jsonObject.containsKey("payload")){
                        result = jsonObject.getJSONObject("payload").getString("result");
                    }
                }
                mFullEdit.setText(fullResult);
                mResultEdit.setText(result);
            }
        });
    }

    // 第七步，识别结束，得到最终完整结果
    @Override
    public void onRecognizedCompleted(final String msg, int code)
    {
        Log.d(TAG,"OnRecognizedCompleted " + msg + ": " + String.valueOf(code));
        recordTask.stop();
        final String fullResult = msg;
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String result = null;
                if (!fullResult.equals("")){
                    JSONObject jsonObject = JSONObject.parseObject(fullResult);
                    if (jsonObject.containsKey("payload")){
                        result = jsonObject.getJSONObject("payload").getString("result");
                    }
                }
                mFullEdit.setText(fullResult);
                mResultEdit.setText(result);
                button.setEnabled(true);
            }
        });
    }

    // 请求结束，关闭连接
    @Override
    public void onChannelClosed(String msg, int code) {

        Log.d(TAG, "OnChannelClosed " + msg + ": " + String.valueOf(code));
    }

    static class RecordTask extends AsyncTask<Void, Integer, Void> {
        final static int SAMPLE_RATE = 16000;
        final static int SAMPLES_PER_FRAME = 640;

        private boolean sending;

        WeakReference<SpeechRecognizerActivity> activityWeakReference;

        public RecordTask(SpeechRecognizerActivity activity) {
            activityWeakReference = new WeakReference<>(activity);
        }

        public void stop() {
            sending = false;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            SpeechRecognizerActivity activity = activityWeakReference.get();
            Log.d(TAG,"Init audio recorder");
            int bufferSizeInBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            AudioRecord mAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes * 2);

            if (mAudioRecorder == null || mAudioRecorder.getState() == STATE_UNINITIALIZED) {
                throw new IllegalStateException("Failed to initialize AudioRecord!");
            }
            mAudioRecorder.startRecording();

            ByteBuffer buf = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME);
            sending = true;
            while(sending){
                buf.clear();
                // 采集语音
                int readBytes = mAudioRecorder.read(buf, SAMPLES_PER_FRAME);
                byte[] bytes = new byte[SAMPLES_PER_FRAME];
                buf.get(bytes, 0, SAMPLES_PER_FRAME);
                if (readBytes>0 && sending){
                    // 第六步，发送语音数据到识别服务
                    int code = activity.speechRecognizer.sendAudio(bytes, bytes.length);
                    if (code < 0) {
                        Log.i(TAG, "Failed to send audio!");
                        activity.speechRecognizer.stop();
                        break;
                    }
                    Log.d(TAG, "Send audio data length: " + bytes.length);
                }
                buf.position(readBytes);
                buf.flip();
            }
            activity.speechRecognizer.stop();
            mAudioRecorder.stop();
            return null;
        }
    }
}
