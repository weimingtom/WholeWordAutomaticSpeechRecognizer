package org.unirender.asr.wholeword.recognizer;

import java.io.File;
import java.util.LinkedHashMap;

import org.unirender.asr.wholeword.audio.VAD;
import org.unirender.asr.wholeword.language.RecognizerPreset;
import org.unirender.asr.wholeword.language.SupportedLanguages;


public class LiveSpeechRecognizer {

	String recordFile = "capturedaudio";
	SingleWordSpeechRecognizer recognizer;
	VAD voiceActivityDetector;
	public VAD getVoiceActivityDetector() {
		return voiceActivityDetector;
	}


	public LiveSpeechRecognizer (SupportedLanguages language,
			File modelsDirectory, RecognizerPreset preset) throws Exception{
		recognizer = new SingleWordSpeechRecognizer(language,modelsDirectory,preset);
		voiceActivityDetector = new VAD();
	}
	
	
	public double getScore(){
		return recognizer.getBestScore();
	}
	
	public LinkedHashMap<String, Double> getNBest(){
		return recognizer.getNBest();
	}
	
	public String listenAndRecognize() throws Exception{

		boolean caught= false;
		File waveFile = new File(recordFile + ".wav");
		
		try {
			caught = voiceActivityDetector.catchAudio(waveFile);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(">Error in audio capture");
			System.exit(0);
		}
		String recognizedWord = null;
		if (caught) {
			//System.out.println("Audio caught");
			recognizedWord = recognizer.recognize(waveFile);
			System.out.println(">Best transcription "+recognizedWord+" ("+recognizer.getBestScore()+")");
		}
		
		if (recordFile.equals("capturedaudio"))
			recordFile = "capturedaudio1";
		else
			recordFile = "capturedaudio";

		System.out.println();
		return recognizedWord;
	}
}
