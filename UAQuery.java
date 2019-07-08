
import java.io.*;

public class UAQuery {

  // Don't create a TCM for all documents, just those in the query.

  static final int RECORD_LENGTH = 8+4+4;
  static final int seed = 5000;

  public static void main(String[] args) {

    String[] test = {"this","is","a","test"};

    runQuery(test);

  }

  public static String[] runQuery(String[] query) {

    try {

      RandomAccessFile dict = new RandomAccessFile("output/dict.raf","rw");

      dict.seek(hash("youtube",0) * (RECORD_LENGTH+2));
      String record = dict.readUTF();
      int count = dict.readInt();
      int start = dict.readInt();

      System.out.println(record+" "+count+" "+start);

      RandomAccessFile post = new RandomAccessFile("output/post.raf","rw");

      post.seek(start+2);
      for(int i = start; i < start+count; i++) {
        System.out.println(post.readInt()+" "+post.readFloat());
      }

      dict.close();
      post.close();

    } catch(IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }

    return null;

  }

  public static int hash(String str, int i) {

    return ( Math.abs(str.hashCode()) + i ) % seed;

  } // h(k,i) = (h'(k) + i) mod m


}
