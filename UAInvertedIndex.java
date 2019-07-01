
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

  /*
    Try to perform the merge here?
    Instead of 1 : 1 , try consolidating the files.

    Use an array for the files, then merge pairs of files.
    Writing the new temp file to disk.

    Open leftmost file, don't treat it like an array.
    Make rightmost files null as we merge, & delete them on filesystem.
    Or, just always read the leftmost, or first, file.
  */

  public static void algoMerge(File inDir) {

    try {



    } catch(Exception ex) {
      ex.printStackTrace();
      System.exit(1);
    }

  }

  public static void merge(File[] in, int p, int r) {

    try {



    } catch(Exception ex) {
      ex.printStackTrace();
      System.exit(1);
    }

  }

  //----------------------------------------------------------------------------

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

  //----------------------------------------------------------------------------

  /*
  MERGE-SORT(A,p,r)
    if p < r
      q = floor( (p+r)/2 )
      MERGE-SORT(A,p,q)
      MERGE-SORT(A,q+1,r)
      MERGE(A,p,q,r)

  MERGE(A,p,q,r)
    n1 = q-p + 1
    n2 = r-q
    let L[1 .. n1 + 1] and R[1 .. n2 + 1] be new arrays (two open files)
    for i = 1 to n1
      L[i] = A[p+i-1]
    for j = 1 to n2
      R[j] = A[q+j]
    L[n1 + 1] = infinity
    R[n2 + 1] = infinity
    i = 1
    j = 1
    for k = p to r
      if L[i] <= R[j]
        A[k] = L[i] (What is A? A RAF? A newer, slightly bigger file? What if we used filnames? Index locations?)
        i = i + 1
      else A[k] = R[j]
        j = j + 1
  */

}
