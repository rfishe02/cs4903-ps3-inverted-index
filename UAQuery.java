
import java.io.*;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.ArrayList;

public class UAQuery {

  // Don't create a TCM for all documents, just those in the query.

  static final int DICT_LEN = 8+4+4;
  static final int POST_LEN = 4+4;
  static final int DOC_LEN = 25;
  static final int seed = 5000;

  public static int hash(String str, int i) {
    return ( Math.abs(str.hashCode()) + i ) % seed;
  }

  public static void main(String[] args) {

    String[] test = {"this","is","a","test"};

    runQuery(test);

  }

  public static String[] runQuery(String[] query) {

    try {

      RandomAccessFile dict = new RandomAccessFile("output/dict.raf","rw");

      dict.seek(hash("youtube",0) * (DICT_LEN+2));
      String record = dict.readUTF();
      int count = dict.readInt();
      int start = dict.readInt();

      System.out.println(record+" "+count+" "+start);

      RandomAccessFile post = new RandomAccessFile("output/post.raf","rw");
      RandomAccessFile map = new RandomAccessFile("output/map.raf","rw");

      File[] files = new File[count];
      int docID;
      int a = 0;

      post.seek(((start-count)+1) * POST_LEN);
      for(int i = 0; i < count; i++) {
        docID = post.readInt();
        System.out.println(docID+" "+post.readFloat());

        map.seek(docID * (DOC_LEN + 2));
        files[a] = new File("input/"+map.readUTF());
        a++;

      }

      Semantic s = new Semantic();
      ArrayList<String> vocab = s.getVocab(files);
      float[][] tcm = s.buildTermContextMatrix(files,vocab,vocab.size(),4);

      int u = s.wordSearch(vocab, "youtube");
      s.getContext(vocab,tcm,10,u);


      dict.close();
      post.close();
      map.close();

    } catch(IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }

    return null;

  }

}
