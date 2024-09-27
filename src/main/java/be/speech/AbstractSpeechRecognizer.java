package be.speech;

import java.io.IOException;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.Context;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.decoder.adaptation.ClusteredDensityFileData;
import edu.cmu.sphinx.decoder.adaptation.Stats;
import edu.cmu.sphinx.decoder.adaptation.Transform;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;


/**
 * Base class for high-level speech recognizers.
 */
public class AbstractSpeechRecognizer {

    protected final Context context;
    protected final Recognizer recognizer;
    
    protected ClusteredDensityFileData clusters;

    protected final SpeechSourceProvider speechSourceProvider;

    /**
     * Constructs recognizer object using provided configuration.
     * @param configuration initial configuration
     * @throws IOException if IO went wrong
     */
    public AbstractSpeechRecognizer(Configuration configuration)
        throws IOException
    {
        this(new Context(configuration));
    }

    protected AbstractSpeechRecognizer(Context context) throws IOException {
        this.context = context;
        recognizer = context.getInstance(Recognizer.class);
        speechSourceProvider = new SpeechSourceProvider();
    }

    /**
     * Returns result of the recognition.
     * 
     * @return recognition result or {@code null} if there is no result, e.g., because the
     * 			microphone or input stream has been closed 
     */
    public SpeechResult getResult() {
        Result result = recognizer.recognize();
        return null == result ? null : new SpeechResult(result);
    }
    
    public Stats createStats(int numClasses) {
        clusters = new ClusteredDensityFileData(context.getLoader(), numClasses);
        return new Stats(context.getLoader(), clusters);
    }

    public void setTransform(Transform transform) {
        if (clusters != null && transform != null) {
            context.getLoader().update(transform, clusters);
        }
    }

    public void loadTransform(String path, int numClass) throws Exception {
    	clusters = new ClusteredDensityFileData(context.getLoader(), numClass);
    	Transform transform = new Transform((Sphinx3Loader)context.getLoader(), numClass);
    	transform.load(path);
    	context.getLoader().update(transform, clusters);
    }
}
