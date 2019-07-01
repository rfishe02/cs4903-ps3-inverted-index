
import java.io.*;
import java.util.TreeMap;
import java.util.SortedMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Comparator;

public class UAInvertedIndex {

  static final int RECORD_LENGTH = 20;
  static final int SUB = 4;
  static GlobalMap gh;
  static int termID;
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
    termID = 0;

    algoOne(inDir,outDir);
    algoTwo(outDir);

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
    SortedMap<String, Integer> ht;
    BufferedReader br;
    BufferedWriter bw;
    TermData t;
    String read;
    int totalFreq;

    int docID = 0;

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

        bw = new BufferedWriter(new FileWriter(outDir.getPath()+"/doc"+docID+".temp")); // Open new temporary file f.

        // for all term t in document hash table ht, do

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

        } // Sort all ht entries by term alphabetically.

        docID++;
        br.close();
        bw.close(); // close temp file f.
      }

    } catch(Exception ex) {
      ex.printStackTrace();
      System.exit(1);
    }
  }

  public static void algoTwo(File input) {
    String top = "";
    int topInd = 0;

    try {
      File[] files = input.listFiles();
      BufferedReader[] br = new BufferedReader[files.length];

      for(int a = 0; a < files.length; a++) {

        br[a] = new BufferedReader(new FileReader(files[a]));

      }

      // while all postings haven't been written do
      // find token that is alphabetically first in the buffer
      // update the start field for the token in the global hash table.

      // calculate inverse document frequency for term from gh(t).numberOfDocuments

      // for all files, if files[i] is alphabetically first (?)
      // write postings record for the token (documentID, termFrequency, OR rtf * idf)

      // read the next record from the file
      // recordCount = recordCount + 1;

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

            if(read.compareTo(top) < 0) {

              br[topInd].reset();
              top = read;
              topInd = b;

            } else {
              br[b].reset();
            }

          } else {
            nullCount++;
          }

        }

        System.out.println(top);
      }

      System.out.println("DONE!");

      post.close();

      for(BufferedReader b : br) {
        b.close();
      }


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

    } catch(IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }

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

  static class LineComparator implements Comparator<String> {
    int len = RECORD_LENGTH - SUB;
    public int compare(String s1, String s2) {
      return s1.substring(0,len).compareToIgnoreCase(s2.substring(0,len));
    }
  }

}
