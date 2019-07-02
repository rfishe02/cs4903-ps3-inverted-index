
import java.io.*;
import java.util.TreeMap;
import java.util.SortedMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Comparator;

public class UAInvertedIndex {

  /*
  temp file
  term, document ID, term frequency (within document)

  post.raf -- the documents where the term appears
  document ID, term frequency (within document) or rtf*idf

  dict.raf -- the global hash table
  term or termID, document count, start

  map.raf
  */

  static final int RECORD_LENGTH = 20;
  static final int SUB = 4;
  static GlobalMap gh;
  //static int seed = 3000000;
  static int seed = 25;

  public static void main(String[] args) {

    if(args.length < 1) {
      args = new String[2];
      args[0] = "./input";
      args[1] = "./output";
    }

    File inDir = new File(args[0]);
    File outDir = new File(args[1]);

    gh = new GlobalMap(seed); // initialize global hash table.

    //algoOne(inDir,outDir);
    //algoTwo(outDir);

    File[] test = (new File("output/")).listFiles();

    mergeSort(test,test.length);

    /*
    try {
      RandomAccessFile test = new RandomAccessFile("dict.raf","rw");

      test.seek(gh.hash("cat",0) * (RECORD_LENGTH + 2));
      String record = test.readUTF();
      int count = test.readInt();

      System.out.println(record+" "+count);

    } catch(IOException ex) {
      ex.printStackTrace();
    }*/

  }

  public void buildInvertedIndex() {



  }

  public static void algoOne(File inDir, File outDir) {
    SortedMap<String, Integer> ht;  // Sort all ht entries by term alphabetically.
    BufferedReader br;
    String read;
    int totalFreq;

    int docID = 0;
    int termID = 0;

    try {

      for(File d : inDir.listFiles()) {

        br = new BufferedReader(new FileReader(d));

        ht = new TreeMap<String,Integer>(new TermComparator()); // initialize a document hash table.
        totalFreq = 0; // set totalFreq to zero.

        while((read = br.readLine())!=null) {
          if(ht.containsKey(read)) {
            ht.put(read,ht.get(read)+1);
          } else {
            ht.put(read,1);
          }
          totalFreq++;
        }

        termID = writeTempFile(outDir, ht, docID, termID);

        docID++;
        br.close();
      }

    } catch(Exception ex) {
      ex.printStackTrace();
      System.exit(1);
    }
  }

  public static int writeTempFile(File outDir, SortedMap<String, Integer> ht, int docID, int termID) throws IOException {

    BufferedWriter bw = new BufferedWriter(new FileWriter(outDir.getPath()+"/doc"+docID+".temp")); // Open new temporary file f.
    TermData t;

    for(Map.Entry<String,Integer> entry : ht.entrySet()) {

      if( ( t = gh.get( entry.getKey() ) ) != null ) {

        t.setCount(t.getCount() + 1);
        gh.put(t);

      } else {

        t = new TermData(entry.getKey(),termID,1); // put(t, <termID, # documents = 1>)
        gh.put(t);
        termID = termID + 1;

      } // if a term hasn't been found in prior documents.

      bw.write( formatRecord(entry.getKey()) +" "+ docID +" "+ entry.getValue() +"\n"); // f.write(t, documentID, termFrequency (or tf / totalFrequency));

    }  // for all term t in document hash table ht, do
    bw.close();  // close temp file f.

    return termID;
  }

  public static void algoTwo(File input) {
    String top = "";
    int topInd = 0;

    try {
      File[] files = input.listFiles();

      /*

      */

      BufferedReader[] br = new BufferedReader[files.length];

      for(int a = 0; a < files.length; a++) {
        br[a] = new BufferedReader(new FileReader(files[a]));
      }

      RandomAccessFile post = new RandomAccessFile("post.raf","rw"); // Create & open a new file for postings, post.raf
      String read = "";
      int nullCount = 0;
      int recordCount = 0;

      while(nullCount < br.length) {

        br[topInd].mark(100);
        top = br[topInd].readLine();
        nullCount = 0;

        for(int b = 0; b < br.length; b++) {

          br[b].mark(100);
          if((read = br[b].readLine()) != null) {

            if(read.substring(0,(RECORD_LENGTH - SUB)).compareTo(top.substring(0,(RECORD_LENGTH - SUB))) < 0) {

              br[topInd].reset();
              top = read;
              topInd = b;

            } else {
              br[b].reset();
            }

          } else {
            nullCount++;
          }

        } // find token that is alphabetically first in the buffer

        System.out.println(top); // Need to write to postings.

        // update the start field for the token in the global hash table.
        // calculate inverse document frequency for term from gh(t).numberOfDocuments
        // write postings record for the token (documentID, termFrequency, OR rtf * idf)
        // recordCount = recordCount + 1;

      } // while all postings haven't been written do

      System.out.println("DONE!");

      post.close();

      for(BufferedReader b : br) {
        b.close();
      }

      writeDictionary();

    } catch(IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }

  }

  public static void writeDictionary() throws IOException {

    RandomAccessFile dict = new RandomAccessFile("dict.raf","rw"); //write global hash table to disk as dictionary file dict.raf
    String s;
    int c;

    for(TermData t : gh.map) {

      if(t != null) {
        s = t.getT();
        c = t.getCount();
      } else {
        s = "NA";
        c = -1;
      }

      dict.writeUTF( formatRecord(s) );
      dict.writeInt( c );
    }

    dict.close();

  }

  public static String formatRecord(String str) {
    int len = RECORD_LENGTH - SUB;
    if(str.length() > (len)) {
      str = str.substring(0,len);
    }
    return String.format("%-"+len+"s",str);
  }

  static class TermComparator implements Comparator<String> {
    public int compare(String s1, String s2) {
      return s1.compareToIgnoreCase(s2);
    }
  }

  //----------------------------------------------------------------------------

  public static void mergeSort(File[] A, int n) {

    try {

      for(int c = 1; c < n; c = 2 * c) {

        for(int p = 0; p < n-1; p += 2 * c) {

          int q = Math.min(p + (c-1), n-1);
          int r = Math.min(p + 2*(c-1), n-1);

          merge(A, p, q, r);

        }

      }

    } catch(Exception ex) {
      ex.printStackTrace();
    }

  }

  public static void merge(File[] A, int p, int q, int r) throws IOException {

    BufferedReader L = new BufferedReader( new FileReader(A[p]) );
    BufferedReader R;

    if((q + 1) < A.length) {
      R = new BufferedReader( new FileReader(A[q + 1]) );
    } else {
      R = new BufferedReader( new FileReader(A[q]) );
    }

    String filename = "test/"+p+""+(q+1)+""+r+".tmp";
    BufferedWriter bw = new BufferedWriter(new FileWriter(filename));

    String s1;
    String s2;

    while((s1 = L.readLine()) != null && (s2 = R.readLine()) != null) {

      if(s1.compareTo(s2) <= 0) {
        bw.write(s1+"\n"+s2+"\n");

      } else {
        bw.write(s2+"\n"+s1+"\n");
      }

    }

    while((s1 = L.readLine()) != null) {
      bw.write(s1+"\n");
    }
    while((s2 = R.readLine()) != null) {
      bw.write(s2+"\n");
    }

    L.close();
    R.close();
    bw.close();

    if((q+1) > 1) {
      if(A[p].exists()) {
        A[p].delete();
      }
    }
    A[p] = new File(filename);

  }
}
