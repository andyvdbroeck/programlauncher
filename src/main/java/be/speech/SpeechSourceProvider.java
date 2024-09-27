package be.speech;

public class SpeechSourceProvider {

    Microphone getMicrophone() {
        return new Microphone(16000, 16, true, false);
    }
}
