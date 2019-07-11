
import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

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

    String[] test = {"xxxxx"};

    runQuery(test);

  }

  public static String[] runQuery(String[] query) {

    String filename;

    try {

      /*
      Build a document term matrix, then use RTFIDF to calculate cosine similarity.
      Built a |V| x |D| matrix, of floats.

      known: |V|
      unknown: |D|
      */

      HashMap<Integer,Integer> docMap = new HashMap<>();
      HashMap<String,Integer> termMap = new HashMap<>();

      mapRowsCols(docMap,termMap,query);

      System.out.println(termMap.size());

      RandomAccessFile map = new RandomAccessFile("output/map.raf","rw");
      map.close();

    } catch(IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }

    return null;
  }

  public static void mapRowsCols(HashMap<Integer,Integer> docMap, HashMap<String,Integer> termMap, String[] query) throws IOException {
    RandomAccessFile dict = new RandomAccessFile("output/dict.raf","rw");
    RandomAccessFile post = new RandomAccessFile("output/post.raf","rw");
    String record;
    int row = 0;
    int col = 0;
    int count;
    int start;
    int docID;
    int i;

    for(String s : query) {
      i = 0;
      do {
        dict.seek(hash(s,i) * (DICT_LEN+2));
        record = dict.readUTF();
        i++;
      } while(record.trim().compareTo("NA") != 0 && record.trim().compareTo(s) != 0); // Find the term in the dictionary.

      if(record.trim().compareTo("NA") != 0) {
        if(!termMap.containsKey(s)) {
          termMap.put(s,row);
          row++;
        } // Map terms to rows in the document term matrix.

        count = dict.readInt();
        start = dict.readInt();

        post.seek(((start-count)+1) * POST_LEN);
        for(int x = 0; x < count; x++) {
          docID = post.readInt();
          post.readFloat(); // rtfIDF

          if(!docMap.containsKey(docID)) {
            docMap.put(docID,col);
            col++;
          } // Map document ID to column.

        } // Read each posting listed.
      }
    } // Map terms & documets to columns.

    dict.close();
    post.close();

  }

  public static float[][] documentTermMatrix(HashMap<String,Integer> docMap, HashMap<String,Integer> termMap, HashSet<String> files, int row, int col) {

    BufferedReader br;
    String read;

    /*
    try {

      Iterator it = mp.entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry pair = (Map.Entry)it.next();

        br = new BufferedReader(new FileReader(inDir.getPath()+"/"+(String)pair.getKey()));

        while((read=br.readLine())!=null) {

          if(termMap.get(read)!=null) {
            res[ termMap.get( read ) ][ (int)pair.getValue() ]++;
          }

        }

        br.close()
      }

    } catch(IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }*/

    return null;

  }

  /*
  dict.seek(hash("youtube",0) * (DICT_LEN+2));
  String record = dict.readUTF();
  int count = dict.readInt();
  int start = dict.readInt();

  System.out.println(record+" "+count+" "+start);

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
  float[][] tcm = s.buildTermContextMatrix(files,vocab,vocab.size(),8);

  int u = s.wordSearch(vocab, "youtube");
  s.getContext(vocab,tcm,10,u);
  */

}
