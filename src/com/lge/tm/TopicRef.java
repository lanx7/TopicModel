package com.lge.tm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.pipe.iterator.ArrayIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.Alphabet;
import cc.mallet.types.IDSorter;
import cc.mallet.types.InstanceList;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class Lda {

  @Inject private com.benmccann.topicmodel.TextProvider textProvider;

  InstanceList createInstanceList(List<String> texts) throws IOException {
    ArrayList<Pipe> pipes = new ArrayList<Pipe>();
    pipes.add(new CharSequence2TokenSequence());
    pipes.add(new TokenSequenceLowercase());
    pipes.add(new TokenSequenceRemoveStopwords());
    pipes.add(new TokenSequence2FeatureSequence());
    InstanceList instanceList = new InstanceList(new SerialPipes(pipes));
    instanceList.addThruPipe(new ArrayIterator(texts));
    return instanceList;
  }

  private ParallelTopicModel createNewModel() throws IOException {
    List<String> texts = textProvider.getTexts();
    InstanceList instanceList = createInstanceList(texts);
    int numTopics = instanceList.size() / 5;
    ParallelTopicModel model = new ParallelTopicModel(numTopics);
    model.addInstances(instanceList);
    model.estimate();
    return model;
  }

  ParallelTopicModel getOrCreateModel() throws Exception {
    return getOrCreateModel("model");
  }

  private ParallelTopicModel getOrCreateModel(String directoryPath)
      throws Exception {
    File directory = new File(directoryPath);
    if (!directory.exists()) {
      directory.mkdir();
    }
    File file = new File(directory, "mallet-lda.model");
    ParallelTopicModel model = null;
    if (!file.exists()) {
      model = createNewModel();
      model.write(file);
    } else {
      model = ParallelTopicModel.read(file);
    }
    return model;
  }

  public void printTopics() throws Exception {
    ParallelTopicModel model = getOrCreateModel();
    Alphabet alphabet = model.getAlphabet();
    for (TreeSet<IDSorter> set : model.getSortedWords()) {
      System.out.print("TOPIC: ");
      for (IDSorter s : set) {
        System.out.print(alphabet.lookupObject(s.getID()) + ", ");
      }
      System.out.println();
    }
  }

  public static void main(String[] args) throws Exception {
    Injector injector = Guice.createInjector();
    Lda lda = injector.getInstance(Lda.class);
    lda.printTopics();
  }

}
