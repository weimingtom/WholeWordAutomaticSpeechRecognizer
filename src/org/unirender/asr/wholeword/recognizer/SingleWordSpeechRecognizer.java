package org.unirender.asr.wholeword.recognizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import javax.sound.sampled.AudioFormat;

import org.unirender.asr.wholeword.audio.AudioBits;
import org.unirender.asr.wholeword.audio.AudioProcessing;
import org.unirender.asr.wholeword.audio.MfccExtraction;
import org.unirender.asr.wholeword.language.ILanguageModel;
import org.unirender.asr.wholeword.language.LanguageModelGM;
import org.unirender.asr.wholeword.language.RecognizerPreset;
import org.unirender.asr.wholeword.language.SupportedLanguages;

import be.ac.ulg.montefiore.run.jahmm.Hmm;
import be.ac.ulg.montefiore.run.jahmm.ObservationVector;

public class SingleWordSpeechRecognizer {

	private ILanguageModel lm;
	private double bestScore = -Double.MAX_VALUE;
	private String bestWord = "";
	private LinkedList<Hmm<ObservationVector>> hmmsList;
	private LinkedList<String> wordsToRecognize;
	private File modelsFolder ;
	private LinkedHashMap<String,Double> NBest;
	
	public LinkedHashMap<String, Double> getNBest() {
		return NBest;
	}

	public SingleWordSpeechRecognizer(SupportedLanguages language,File modelsFolder,RecognizerPreset preset)
			throws Exception {
		this.modelsFolder = modelsFolder;
		init(language, preset);
	}


	@SuppressWarnings("unchecked")
	public void init(SupportedLanguages language, RecognizerPreset preset) throws Exception {

		hmmsList = new LinkedList<Hmm<ObservationVector>>();

		//String resource = "MODELS/" + language.name();
		File resource = new File(modelsFolder, language.name()+"/"+preset.name());
		
		if (language == SupportedLanguages.IT)
			lm = new LanguageModelGM();

		wordsToRecognize = new LinkedList<String>();

		//URL in = ClassLoader.getSystemClassLoader().getResource(resource);
		
		//File[] files = new File(in.toURI()).listFiles();
		File[] files = resource.listFiles();
		
		System.out.println("All models in the path:" + Arrays.asList(files));
		
		for (int i = 0; i < files.length; i++) {
			File f = files[i];
			String hmmPath = f.getAbsolutePath();
			String hmmName = f.getName().substring(0,
					f.getName().lastIndexOf("."));

			{

				ObjectInputStream ois = new ObjectInputStream(
						new FileInputStream(new File(hmmPath)));
				Object hmmobs = ois.readObject();
				Hmm<ObservationVector> hmm = null;
				if (hmmobs instanceof Hmm)
					hmm = (Hmm<ObservationVector>) hmmobs;
				ois.close();
				wordsToRecognize.add(hmmName);
				hmmsList.add(hmm);
			}
		}

	}

	/**
	 * Recognize a sequence of numeric observations
	 * 
	 * @param X
	 * @return
	 */
	public String recognizeObservations(double[][] X) {

		int bestindex = 0;
		double bestvalue = -Double.MAX_VALUE;
		int index = 0;
		
		ArrayList<Double> likelihoods = new ArrayList<Double>();
		HashMap<Double, String> modelsScore = new HashMap<Double,String>();
		
		List<ObservationVector> oseq = AudioProcessing.selectObservations(0,
				X.length - 1, X);
		for (Hmm<ObservationVector> hmm : hmmsList) {
			
			// apply Viterbi
			double like = hmm.lnProbability(oseq);

			String hname = wordsToRecognize.get(index);
			

			// apply language model
			like = lm.singleWordLanguageModel(hname, like);
			//System.out.println("Like of " + hname + " : " + like);
			//System.out.println("Post Like of " + hname + " : " + like);
			likelihoods.add(like);
			modelsScore.put(like, hname);
			if (like > bestvalue) {
				bestindex = index;
				bestvalue = like;
			}
			index++;
		}
		
		java.util.Collections.sort(likelihoods,Collections.reverseOrder());
		int counter = 1;
		NBest = new LinkedHashMap<String,Double>();
		for (Double likelihood:likelihoods){
			String m = modelsScore.get(likelihood);
			//System.out.println(counter + ": "+m+" "+likelihood);
			NBest.put(m, likelihood);
			counter++;
		}
		
		bestWord = wordsToRecognize.get(bestindex);
		bestScore = bestvalue;

		return bestWord;
	}

	/**
	 * Recognizes an audio file: transforms from audio to string
	 */
	public String recognize(File audiofile) throws Exception {
		AudioBits audio = new AudioBits(audiofile);
		short[] shortaudio = audio.getShortVectorAudio();
		AudioFormat af = audio.getAudioFormat();
		String supportedAudioFormat = "PCM_SIGNED 8000.0 Hz, 16 bit, mono, 2 bytes/frame, little-endian";
		
		//System.out.println("AudioFormat: " + (""+af));
		//System.out.println("Supported AudioFormat" + supportedAudioFormat);
		
		if (!supportedAudioFormat.equals(""+af))
			throw new Exception("Audio format not supported");

		float sf = af.getSampleRate();
		//System.out.println("Sample Rate: " + sf);
		audio.deallocateAudio();

		// framesize - frame corresponding to 5 ms
		int frameSize = (int) (sf * 0.005);
		shortaudio = AudioProcessing.trim(shortaudio, frameSize);

		// features matrix
		double X[][] = null;
		MfccExtraction mfcc = new MfccExtraction(sf);

		X = mfcc.extractMFCC(shortaudio);

		return recognizeObservations(X);

	}

	public ILanguageModel getLm() {
		return lm;
	}

	public void setLm(ILanguageModel lm) {
		this.lm = lm;
	}


	public double getBestScore() {
		return bestScore;
	}

	public void setBestScore(double bestScore) {
		this.bestScore = bestScore;
	}

	public String getBestWord() {
		return bestWord;
	}

	public void setBestWord(String bestWord) {
		this.bestWord = bestWord;
	}

	public LinkedList<Hmm<ObservationVector>> getHmmsList() {
		return hmmsList;
	}

	public void setHmmsList(LinkedList<Hmm<ObservationVector>> hmmsList) {
		this.hmmsList = hmmsList;
	}

	public LinkedList<String> getWordsToRecognize() {
		return wordsToRecognize;
	}

	public void setWordsToRecognize(LinkedList<String> words) {
		this.wordsToRecognize = words;
	}

}
