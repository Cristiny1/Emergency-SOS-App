package com.example.emergency_sos_app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Smart AI Safety Assistant powered by Google Gemini (via Backend Proxy).
 */
public class ChatbotActivity extends BaseActivity implements ChatAdapter.OnActionClickListener {

    private RecyclerView rvChat;
    private ChatAdapter adapter;
    private List<ChatMessage> messages = new ArrayList<>();
    private EditText etMessage;
    private ImageView btnMic;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SpeechRecognizer speechRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        setupEdgeToEdge();
        initViews();
        setupSpeech();
        
        postBotMessage("Hello! I am your AI Safety Assistant. How can I help you today?");
    }

    private void initViews() {
        rvChat = findViewById(R.id.rvChat);
        etMessage = findViewById(R.id.etMessage);
        btnMic = findViewById(R.id.btnMic);
        View btnSend = findViewById(R.id.btnSend);
        ChipGroup chipGroup = findViewById(R.id.chipGroupActions);

        adapter = new ChatAdapter(messages, this);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> handleUserMessage(etMessage.getText().toString().trim()));
        btnMic.setOnClickListener(v -> toggleSpeech());

        // Chips
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip) {
                child.setOnClickListener(v -> handleUserMessage(((Chip) v).getText().toString()));
            }
        }
    }

    private void handleUserMessage(String text) {
        if (text.isEmpty()) return;
        
        etMessage.setText("");
        messages.add(new ChatMessage(text, true));
        adapter.notifyItemInserted(messages.size() - 1);
        rvChat.smoothScrollToPosition(messages.size() - 1);

        ChatMessage typing = ChatMessage.typing();
        messages.add(typing);
        adapter.notifyItemInserted(messages.size() - 1);

        String lang = LanguageManager.getLanguage(this);
        GeminiManager.getInstance().askSafetyAssistant(text, lang, new GeminiManager.GeminiCallback() {
            @Override
            public void onSuccess(AIResponse response) {
                runOnUiThread(() -> {
                    removeTyping(typing);
                    messages.add(new ChatMessage(response));
                    adapter.notifyItemInserted(messages.size() - 1);
                    rvChat.smoothScrollToPosition(messages.size() - 1);
                });
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    removeTyping(typing);
                    postBotMessage("I am having trouble connecting to my AI brain. Please use the SOS button if you are in danger.");
                });
            }
        });
    }

    private void removeTyping(ChatMessage typing) {
        int index = messages.indexOf(typing);
        if (index != -1) {
            messages.remove(index);
            adapter.notifyItemRemoved(index);
        }
    }

    private void postBotMessage(String text) {
        messages.add(new ChatMessage(text, false));
        adapter.notifyItemInserted(messages.size() - 1);
        rvChat.smoothScrollToPosition(messages.size() - 1);
    }

    // --- Action Button Handlers ---
    @Override
    public void onSOSClick() {
        // Trigger existing SOS flow
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        Toast.makeText(this, "Hold SOS button for 3 seconds!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCallClick(String category) {
        String number = "117"; // Default
        if ("FIRE".equals(category)) number = "118";
        
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number)));
    }

    @Override
    public void onReportClick(String category) {
        Intent intent = new Intent(this, CitizenReportActivity.class);
        intent.putExtra("PREFILL_CATEGORY", category);
        startFadeActivity(intent);
    }

    // --- Speech Recognition ---
    private void setupSpeech() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) { btnMic.setColorFilter(getColor(R.color.sos_red)); }
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() { btnMic.setColorFilter(getColor(R.color.text_gray)); }
                @Override public void onError(int error) { btnMic.setColorFilter(getColor(R.color.text_gray)); }
                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) handleUserMessage(matches.get(0));
                }
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
        }
    }

    private void toggleSpeech() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechRecognizer.startListening(intent);
    }

    private void setupEdgeToEdge() {
        View root = findViewById(R.id.chatbotRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int bottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            v.setPadding(0, top, 0, bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) speechRecognizer.destroy();
    }
}
