
public class GlobalMap {
  TermData[] map;

  public GlobalMap(int size) {
    this.map = new TermData[size];
  }

  public int hash(String str, int i, int n) {
    return ( Math.abs(str.hashCode()) + i ) % n;
  } // h(k,i) = (h'(k) + i) mod m

  public void put(TermData t) {
    boolean success = false;

    while(!success) {
      success = add(this.map, t);
      if(!success) {
        resizeTable(1.25);
      }
    }
  }

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

  public TermData get(String str) {
    int h;

    for(int i = 0; i < this.map.length; i++) {
      h = hash(str,i,this.map.length);

      if(this.map[ h ] == null) {
        break;
      } else {
        if(this.map[ h ].getT().compareTo(str) == 0) {
          return this.map[ h ];
        }
      }

    }
    return null;

  }

  public void resizeTable(double inc) {

    System.out.println("RESIZE");

    TermData[] newMap = new TermData[(int)(this.map.length*inc) + 1];

    for(TermData t : this.map) {
      add(newMap,t);
    }

    this.map = newMap;

  }

}
