
/********************************
Name: Renae Fisher
Username: text05
Problem Set: PS3
Due Date: 7/15/19
********************************/

/** This class holds the logic used to maintain the global hash table. */

public class GlobalMap {
  TermData[] map;

  /**
  @param size An initial size for the global hash table.
  */

  public GlobalMap(int size) {
    this.map = new TermData[size];
  }

  /** A method used to create a hashcode for a given term. It uses the formula for a linear probe.
  @param str A term that may be converted to a hash code.
  @param i An index.
  @param n The size of the hash table.
  @return A hashcode for a given term.
  */

  public static int hash(String str, int i, int n) {
    return ( Math.abs(str.hashCode()) + i ) % n;
  } // h(k,i) = (h'(k) + i) mod m

  /** The method used to add a term object to the global hash table. If the add method is unsuccessful,
  it will resize the hash table and make another attempt.
  @param t An object that holds the term and its document count.
   */

  public void put(TermData t) {
    boolean success = false;

    while(!success) {
      success = add(this.map, t);
      if(!success) {
        resizeTable(1.25);
      }
    }
  }

  /** This method attempts to add an object to the global hash table.
  If the object already exists, it merely repleces the object.
  @param m The term data array where the term object will be hashed to.
  @param t An object that holds the term and its document count.
  @return A value that indicates success or failure.
  */

  public boolean add(TermData[] m, TermData t) {
    int h;

    for(int i = 0; i < m.length; i++) {
      h = hash(t.getT(),i,m.length);

      if(m[ h ] == null) {
        m[ h ] = t;
        return true;

      } else if(m[ h ].getT().compareToIgnoreCase(t.getT()) == 0) {
        m[ h ] = t;
        return true;

      }
    }
    return false;
  }

  /** A method used to find a given term in the global hash table.
  @param str The desired term.
  @return A class that contains information such as the the document count.
  */

  public TermData get(String str) {
    int h;

    for(int i = 0; i < this.map.length; i++) {
      h = hash(str,i,this.map.length);

      if(this.map[ h ] == null) {
        return null;
      } else {
        if(this.map[ h ].getT().compareToIgnoreCase(str) == 0) {
          return this.map[ h ];
        }
      }

    }
    return null;

  }

  /** This method resizes the hash table. It creates a new table and re-hashes each entry in the orignal hash table.
  Then, it reassigns the new hash table as the global hash table.
  @param inc The multiplier used to increase the size of the hash table.
  */

  public void resizeTable(double inc) {
    TermData[] newMap = new TermData[ (int)(this.map.length*inc) + 1 ];

    for(int i = 0; i < this.map.length; i++) {
      if(this.map[i] != null) {
        add(newMap,this.map[i]);
      }
    }

    this.map = newMap;
  }

}
