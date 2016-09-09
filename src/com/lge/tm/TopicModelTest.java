package com.lge.tm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Pattern;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.Input2CharSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2Label;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.pipe.iterator.ArrayIterator;
import cc.mallet.pipe.iterator.FileIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.IDSorter;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelSequence;

public class TopicModelTest {
	public static void main(String[] args) throws Exception {
		ParallelTopicModel model = null;
		model =  ParallelTopicModel.read(new File("model.mallet"));;
		
		Alphabet dataAlphabet = model.getAlphabet();
		
		FeatureSequence tokens = (FeatureSequence) model.getData().get(0).instance.getData();
		LabelSequence topics = model.getData().get(0).topicSequence;
		
		Formatter out = new Formatter(new StringBuilder(), Locale.US);
		for (int position = 0; position < tokens.getLength(); position++) {
			out.format("%s-%d ", dataAlphabet.lookupObject(tokens.getIndexAtPosition(position)), topics.getIndexAtPosition(position));
		}
		System.out.println(out);
		
		// Estimate the topic distribution of the first instance, 
		//  given the current Gibbs state.
		double[] topicDistribution = model.getTopicProbabilities(0);

		// Get an array of sorted sets of word ID/count pairs
		ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();
				
		// Show top 5 words in topics with proportions for the first document
		for (int topic = 0; topic < 100; topic++) {
				Iterator<IDSorter> iterator = topicSortedWords.get(topic).iterator();
				out = new Formatter(new StringBuilder(), Locale.US);
				out.format("%d\t%.3f\t", topic, topicDistribution[topic]);
				int rank = 0;
				while (iterator.hasNext() && rank < 15 ) {
					IDSorter idCountPair = iterator.next();
					out.format("%s (%.0f) ", dataAlphabet.lookupObject(idCountPair.getID()), idCountPair.getWeight());
					rank++;
				}
				System.out.println(out);
					
			}
		InstanceList testing = InstanceList.load(new File("data.bin"));
		Instance instance = testing.get(20);
		System.out.println(instance.getData());
		System.out.println(instance.getTarget());
		System.out.println(instance.getName());

				
		//File in = new File("data/doc_20728.txt")
		BufferedReader in = new BufferedReader(new FileReader("data/politic/doc_20728.txt"));
		String s = in.readLine();
		System.out.println(s);
		
		BufferedReader in2 = new BufferedReader(new FileReader("data/politic/doc_12837.txt"));
		String s2 = in2.readLine();
		System.out.println(s2);

		
		List<String> texts = new ArrayList<String>();
		texts.add(s);
		texts.add(s2);
		InstanceList testing2 = addInstanceList(testing,texts);
		testing2.addThruPipe(new ArrayIterator(texts));
		Instance instance2 = testing2.get(1);
		
		System.out.println(instance2.getData());
		System.out.println(instance2.getName());
	
/*		
		// Create a new instance named "test instance" with empty target and source fields.
		List<String> texts = new ArrayList<String>();
		texts.add("보험 생명 연금");
		InstanceList testing = createInstanceList(texts);
		//testing.addThruPipe(new Instance("보험 생명", null, "test instance", null));
		//Instance ins = new Instance("보험, 생명", null, "test", null);
*/	
		TopicInferencer inferencer = model.getInferencer();
		double[] testProbabilities = inferencer.getSampledDistribution(instance, 10, 1, 5);
		for (int i = 0 ; i < testProbabilities.length ;i++){
			System.out.println(i + "\t" + testProbabilities[i]*1000);
		}
		
		double[] testProbabilities2 = inferencer.getSampledDistribution(instance2, 10, 1, 5);
		for (int i = 0 ; i < testProbabilities2.length ;i++){
			System.out.println(i + "\t" + testProbabilities2[i]*1000);
		}
		System.out.println(FileIterator.LAST_DIRECTORY);
	}
	
	public static InstanceList createInstanceList(List<String> texts) throws IOException{
		ArrayList<Pipe> pipes = new ArrayList<Pipe>();
		pipes.add(new Input2CharSequence("UTF-8"));
        Pattern tokenPattern =
            Pattern.compile("[\\p{L}\\p{N}_]+");
		pipes.add(new CharSequence2TokenSequence(tokenPattern));

        // Normalize all tokens to all lowercase
        pipes.add(new TokenSequenceLowercase());

        // Remove stopwords from a standard English stoplist.
        //  options: [case sensitive] [mark deletions]
        pipes.add(new TokenSequenceRemoveStopwords(new File("stopword.kr"), "UTF-8", false, false, false));

        // Rather than storing tokens as strings, convert 
        //  them to integers by looking them up in an alphabet.
        pipes.add(new TokenSequence2FeatureSequence());
        
        InstanceList instanceList = new InstanceList(new SerialPipes(pipes));
        instanceList.addThruPipe(new ArrayIterator(texts));
        return instanceList;
	}
	
	public static InstanceList addInstanceList(InstanceList list, List<String> texts) throws IOException{
		Pipe pipe = list.getPipe();       
		Alphabet alpha = pipe.getAlphabet();
        InstanceList instanceList = new InstanceList(pipe);
        instanceList.addThruPipe(new ArrayIterator(texts));
        return instanceList;
	}
}
