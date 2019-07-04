
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
  static int seed = 3000000;

  public static void main(String[] args) {

    if(args.length < 1) {
      args = new String[2];
      args[0] = "./input";
      args[1] = "./output";
    }

    File inDir = new File(args[0]);
    File outDir = new File(args[1]);

    gh = new GlobalMap(seed); // initialize global hash table.

    buildInvertedIndex(inDir,outDir);

  }

  public static void buildInvertedIndex(File inDir, File outDir) {

    algoOne(inDir,new File("temp"));

    File[] tmp = (new File("temp")).listFiles();
    mergeSort(tmp,tmp.length);

    algoTwo(new File("tmp"),outDir);

  }

  public static void algoOne(File inDir, File outDir) {
    SortedMap<String, Integer> ht;  // Sort all ht entries by term alphabetically.
    BufferedReader br;
    BufferedWriter bw = null;
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

          if(read.length() > 8) {
            read = read.substring(0,8);
          }

          if( ht.containsKey( read ) ) {
            ht.put( read ,ht.get( read )+1);
          } else {
            ht.put( formatRecord( read ) ,1);
          }
          totalFreq++;
        }

        termID = writeTempFile(outDir, bw, ht, docID, termID);

        docID++;
        br.close();
      }

    } catch(Exception ex) {
      ex.printStackTrace();
      System.exit(1);
    }
  }

  public static int writeTempFile(File outDir, BufferedWriter bw, SortedMap<String, Integer> ht, int docID, int termID) throws IOException {

    bw = new BufferedWriter(new FileWriter(outDir.getPath()+"/doc"+docID+".temp")); // Open new temporary file f.
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

      bw.write( String.format( "%s %-5d %-5d\n",formatRecord( entry.getKey() ) ,docID, entry.getValue() ) ); // f.write(t, documentID, termFrequency (or tf / totalFrequency));

    }  // for all term t in document hash table ht, do
    bw.close();  // close temp file f.

    return termID;
  }

  public static void algoTwo(File inDir, File outDir) {
    String top = "";
    int topInd = 0;

    try {

      File[] files = inDir.listFiles();
      BufferedReader[] br = new BufferedReader[files.length];

      for(int a = 0; a < files.length; a++) {
        br[a] = new BufferedReader(new FileReader(files[a]));
      }

      RandomAccessFile post = new RandomAccessFile(outDir.getPath()+"/post.raf","rw"); // Create & open a new file for postings, post.raf
      String read = "";
      int nullCount = 0;
      int recordCount = 0;

      while(nullCount < br.length) {

        br[topInd].mark(100);
        top = br[topInd].readLine();
        nullCount = 0;

        for(int b = 0; b < br.length; b++) {

          if(b != topInd) {

            br[b].mark(100);
            if(top == null) {

              top = br[b].readLine();
              topInd = b;
              nullCount++;

            } else if( (read = br[b].readLine()) != null ) {

              if(read.substring(0,(RECORD_LENGTH - SUB)+6).compareTo(top.substring(0,(RECORD_LENGTH - SUB)+6)) < 0) {

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

        } // find token that is alphabetically first in the buffer

        System.out.println(top); // Need to write to postings.

        // update the start field for the token in the global hash table.
        // calculate inverse document frequency for term from gh(t).numberOfDocuments
        // write postings record for the token (documentID, termFrequency, OR rtf * idf)
        // recordCount = recordCount + 1;

      } // while all postings haven't been written do

      post.close();

      for(BufferedReader b : br) {
        b.close();
      }

      System.out.println("DONE");

      //writeDictionary(outDir);

    } catch(IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }

  }

  public static void writeDictionary(File outDir) throws IOException {
    RandomAccessFile dict = new RandomAccessFile(outDir.getPath()+"/dict.raf","rw"); //write global hash table to disk as dictionary file dict.raf
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

  /** Use an iterative merge sort to combine files. The basis for this sort is
      directly from the bottom up sort shown on Wikipedia. However, the merge
      portion is different from the example on the website. It doesn't iterate over
      the array, it just uses the values p and q to combine files.
  */

  public static void mergeSort(File[] A, int n) {
    BufferedReader L = null;
    BufferedReader R = null;
    BufferedWriter bw = null;
    int size;

    try {

      for(int c = 1; c < n; c = 2 * c) {

        size = (int)( (double)(n-1) / c );

        if( size > Math.sqrt(A.length) ) {
          for(int p = 0; p < n-1; p += 2 * c) {

            int q = Math.min(p + (c-1), n-1);
            int r = Math.min(p + 2*(c-1), n-1);

            if( (q+1) < A.length ) {
              merge(A, L, R, bw, p, q, r);
            }
          }
        } else {
          break;  // Stop when the split reaches a certain size.
        }
      }

    } catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  /** Instead of iterating accross the whole array, it opens the files at p and q+1,
     and merges them. It erases the previous files and stores the new file at A[p].

     If we have five files, it may merge files until it merges the
     files at index 0 and 4. The last merge creates a file that holds the data of all
     other files.

     p: 0  q: 1 --> merge the files at index zero and one.
     p: 2  q: 3 --> merge the files at index two and three.
     p: 0  q: 2 --> merge the files at index zero and two.
     p: 0  q: 4 --> merge the files at index zero and four.
  */

  public static void merge(File[] A, BufferedReader L, BufferedReader R, BufferedWriter bw, int p, int q, int r) throws IOException {
    int z = z = q + 1;
    String filename = "tmp/"+p+""+z+""+r+".tmp";

    L = new BufferedReader( new FileReader(A[p]) ); // Open the files at the given indices.
    R = new BufferedReader( new FileReader(A[z]) );

    bw = new BufferedWriter(new FileWriter(filename)); // Create a new file, which will contain the merged data.
    String s1 = L.readLine();
    String s2 = R.readLine();

    while(s1 != null && s2 != null) {

      if(s1.substring(0,(RECORD_LENGTH - SUB)+6).compareTo(s2.substring(0,(RECORD_LENGTH - SUB)+6)) < 0) {
        bw.write(s1+"\n");
        s1 = L.readLine();
      } else {
        bw.write(s2+"\n");
        s2 = R.readLine();
      }

    } // Compare the lines of the file.

    while((s1 = L.readLine()) != null) {
      bw.write(s1+"\n");
    } // Write any remaining lines to the file.
    while((s2 = R.readLine()) != null) {
      bw.write(s2+"\n");
    }

    L.close();
    R.close();
    bw.close();

    if(A[p].exists()) {
      A[p].delete();
    } // Remove the original files.
    if(A[z].exists()) {
      A[z].delete();
    }

    A[p] = new File(filename); // Replace the file at A[p]. Future merges will use the newly merged file.
  }
}
