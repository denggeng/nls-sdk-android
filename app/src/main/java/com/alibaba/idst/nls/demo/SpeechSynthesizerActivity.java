package com.alibaba.idst.nls.demo;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.alibaba.idst.util.NlsClient;
import com.alibaba.idst.util.SpeechSynthesizer;
import com.alibaba.idst.util.SpeechSynthesizerCallback;

public class SpeechSynthesizerActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, SeekBar.OnSeekBarChangeListener {
    private static final String TAG="AliSpeechDemo";

    private static final int SAMPLE_RATE = 16000;
    // 初始化播放器
    private int iMinBufSize = AudioTrack.getMinBufferSize(SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT);
    private AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO
            , AudioFormat.ENCODING_PCM_16BIT,
            iMinBufSize, AudioTrack.MODE_STREAM);

    NlsClient client;
    private SpeechSynthesizer speechSynthesizer;
    private Spinner spinner;
    private String[] voices;
    private String chosenVoice;
    private int speechRate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech_synthesizer);

        SeekBar speedBar = findViewById(R.id.speed);
        speedBar.setOnSeekBarChangeListener(this);

        spinner = findViewById(R.id.voice);
        voices = new String[]{
                SpeechSynthesizer.VOICE_AMEI,
                SpeechSynthesizer.VOICE_NINGER,
                SpeechSynthesizer.VOICE_RUOXI,
                SpeechSynthesizer.VOICE_SICHENG,
                SpeechSynthesizer.VOICE_SIJIA,
                SpeechSynthesizer.VOICE_SIQI,
                SpeechSynthesizer.VOICE_SITONG,
                SpeechSynthesizer.VOICE_SIYUE,
                SpeechSynthesizer.VOICE_XIAOBEI,
                SpeechSynthesizer.VOICE_XIAOGANG,
                SpeechSynthesizer.VOICE_XIAOMEI,
                SpeechSynthesizer.VOICE_XIAOMENG,
                SpeechSynthesizer.VOICE_XIAOWEI,
                SpeechSynthesizer.VOICE_XIAOXUE,
                SpeechSynthesizer.VOICE_XIAOYUN,
                SpeechSynthesizer.VOICE_YINA};
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, voices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        // 第一步，创建client实例，client只需要创建一次，可以用它多次创建synthesizer
        client = new NlsClient();
    }

    // ======================= Code for SeekBar ===============================
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        speechRate = progress - 200;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    // ======================= Code for Spinner ===============================
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
        chosenVoice = voices[position];
        Log.i(TAG, "User choose " + chosenVoice);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    @Override
    public void onDestroy(){
        // 最终，释放客户端
        client.release();
        super.onDestroy();
    }

    public void startSynthesizer(View view){
        EditText editText = findViewById(R.id.editText);
        String text = editText.getText().toString();

        // 第二步，定义语音合成回调类
        SpeechSynthesizerCallback callback = new MyCallback(audioTrack);
        // 第三步，创建语音合成对象
        speechSynthesizer = client.createSynthesizerRequest(callback);

        // 第四步，设置token和appkey
        // Token有有效期，请使用https://help.aliyun.com/document_detail/72153.html 动态生成token
        speechSynthesizer.setToken("");
        // 请使用阿里云语音服务管控台(https://nls-portal.console.aliyun.com/)生成您的appkey
        speechSynthesizer.setAppkey("");

        // 第五步，设置相关参数，以下选项都会改变最终合成的语音效果，可以按文档调整试听效果
        // 设置人声
        Log.i(TAG, "Set chosen voice " + chosenVoice);
        speechSynthesizer.setVoice(chosenVoice);
        // 设置语速
        Log.i(TAG, "User set speechRate " + speechRate);
        speechSynthesizer.setSpeechRate(speechRate);
        // 设置要转为语音的文本
        speechSynthesizer.setText(text);
        // 设置语音数据采样率
        speechSynthesizer.setSampleRate(SpeechSynthesizer.SAMPLE_RATE_16K);
        // 设置语音编码，pcm编码可以直接用audioTrack播放，其他编码不行
        speechSynthesizer.setFormat(SpeechSynthesizer.FORMAT_PCM);

        // 第六步，开始合成
        if(speechSynthesizer.start() < 0)
        {
            Toast.makeText(SpeechSynthesizerActivity.this, "启动语音合成失败！", Toast.LENGTH_LONG).show();
            speechSynthesizer.stop();
            return;
        }
        Log.d(TAG,"speechSynthesizer start done");
        // 第八步，结束合成
        speechSynthesizer.stop();
    }

    private static class MyCallback implements SpeechSynthesizerCallback {
        private AudioTrack audioTrack;

        boolean playing = false;

        public MyCallback(AudioTrack audioTrack) {
            this.audioTrack = audioTrack;
        }

        // 语音合成开始的回调
        @Override
        public void onSynthesisStarted(String msg, int code)
        {
            Log.d(TAG,"OnSynthesisStarted " + msg + ": " + String.valueOf(code));
        }

        // 第七步，获取音频数据的回调，在这里把音频写入播放器
        @Override
        public void onBinaryReceived(byte[] data, int code)
        {
            Log.i(TAG, "binary received length: " + data.length);
            if (!playing) {
                playing = true;
                audioTrack.play();
            }
            audioTrack.write(data, 0, data.length);
        }

        // 语音合成完成的回调，在这里关闭播放器
        @Override
        public void onSynthesisCompleted(final String msg, int code)
        {
            Log.d(TAG,"OnSynthesisCompleted " + msg + ": " + String.valueOf(code));
            audioTrack.stop();
        }

        // 调用失败的回调
        @Override
        public void onTaskFailed(String msg, int code)
        {
            Log.d(TAG,"OnTaskFailed " + msg + ": " + String.valueOf(code));
        }

        // 连接关闭的回调
        @Override
        public void onChannelClosed(String msg, int code) {
            Log.d(TAG, "OnChannelClosed " + msg + ": " + String.valueOf(code));
        }
    }
}
