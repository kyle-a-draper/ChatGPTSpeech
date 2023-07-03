package main.java.org.example;

import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.speech.AudioException;
import javax.speech.Central;
import javax.speech.EngineException;
import javax.speech.synthesis.SynthesizerModeDesc;
import javax.speech.synthesis.Synthesizer;
import java.util.Locale;

public class SpeechRecognition {
    // Replace speech key and region with your own
    private static String speechKey = "YOUR-COGNITIVE-SPEECH-KEY";
    private static String speechRegion = "YOUR-COGNITIVE-SPEECH-REGION";

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        SpeechConfig speechConfig = SpeechConfig.fromSubscription(speechKey, speechRegion);
        speechConfig.setSpeechRecognitionLanguage("en-US");
        recognizeFromMicrophone(speechConfig);
    }

    public static void recognizeFromMicrophone(SpeechConfig speechConfig) throws InterruptedException, ExecutionException {
        AudioConfig audioConfig = AudioConfig.fromDefaultMicrophoneInput();
        SpeechRecognizer speechRecognizer = new SpeechRecognizer(speechConfig, audioConfig);
        System.out.println("Ask me a question.");
        Future<SpeechRecognitionResult> task = speechRecognizer.recognizeOnceAsync();
        SpeechRecognitionResult speechRecognitionResult = task.get();
        System.out.println("Processing...");
        if (speechRecognitionResult.getReason() == ResultReason.RecognizedSpeech) {
            String recognizedText = speechRecognitionResult.getText();
            try {
                chatGPT(recognizedText);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            System.out.println();
            System.out.println("Input Speech = " + recognizedText);
        }
        else if (speechRecognitionResult.getReason() == ResultReason.NoMatch) {
            System.out.println("NOMATCH: Speech could not be recognized.");
        }
        else if (speechRecognitionResult.getReason() == ResultReason.Canceled) {
            CancellationDetails cancellation = CancellationDetails.fromResult(speechRecognitionResult);
            System.out.println("CANCELED: Reason=" + cancellation.getReason());

            if (cancellation.getReason() == CancellationReason.Error) {
                System.out.println("CANCELED: ErrorCode=" + cancellation.getErrorCode());
                System.out.println("CANCELED: ErrorDetails=" + cancellation.getErrorDetails());
            }
        }

        System.exit(0);
    }

    // Using https://gist.github.com/gantoin/190684c344bb70e5c5f9f2339c7be6ed
    public static void chatGPT(String text) throws Exception {
        String url = "https://api.openai.com/v1/completions";
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        // Replace with your ChatGPT API Key
        con.setRequestProperty("Authorization", "Bearer YOUR-CHATGPT-API-KEY");
        JSONObject data = new JSONObject();
        data.put("model", "text-davinci-003");
        data.put("prompt", text);
        data.put("max_tokens", 4000);
        data.put("temperature", 1.0);
        con.setDoOutput(true);
        con.getOutputStream().write(data.toString().getBytes());
        String output = new BufferedReader(new InputStreamReader(con.getInputStream())).lines()
                .reduce((a, b) -> a + b).get();
        String chatGPTOUT = new JSONObject(output).getJSONArray("choices").getJSONObject(0).getString("text");
        System.out.println(chatGPTOUT);
        speak(chatGPTOUT);
    }

    // Uses FreeTTS https://freetts.com/
    private static void speak(String input) throws EngineException, AudioException, InterruptedException {
        System.setProperty(
                "freetts.voices",
                "com.sun.speech.freetts.en.us"
                        + ".cmu_us_kal.KevinVoiceDirectory");
        Central.registerEngineCentral(
                "com.sun.speech.freetts"
                        + ".jsapi.FreeTTSEngineCentral");
        Synthesizer synthesizer
                = Central.createSynthesizer(
                new SynthesizerModeDesc(Locale.US));
        synthesizer.allocate();
        synthesizer.resume();
        synthesizer.speakPlainText(
                input, null);
        synthesizer.waitEngineState(
                Synthesizer.QUEUE_EMPTY);
        synthesizer.deallocate();
    }
}
