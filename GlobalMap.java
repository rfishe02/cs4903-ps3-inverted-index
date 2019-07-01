
public class GlobalMap {

  TermData[] map;
  int count;

  public GlobalMap(int size) {
    this.map = new TermData[size];
    count = 0;
  }

  public int hash(String str, int i) {

    return ( Math.abs(str.hashCode()) + i ) % this.map.length;

  } // h(k,i) = (h'(k) + i) mod m

  public void put(TermData t) {

    if( (double) count / this.map.length > 0.75 ) {
      resizeTable(1.33);
    }

    add(this.map, t);
  }

  public boolean add(TermData[] m, TermData t) {

    int h;

    for(int i = 0; i < m.length; i++) {
      h = hash(t.getT(),i);

      if(m[ h ] == null) {
        m[ h ] = t;
        count++;
        return true;

      } else if(m[ h ].getT().compareToIgnoreCase(t.getT()) == 0) {
        m[ h ] = t;
        return true;

      }
    }
    return false;
  }

  public TermData get(String str) {

    for(int i = 0; i < this.map.length; i++) {

      if(this.map[hash(str,i)] == null) {
        break;
      } else {
        if(this.map[hash(str,i)].getT().compareTo(str) == 0) {
          return this.map[hash(str,i)];
        }
      }

    }
    return null;

  }

  public void resizeTable(double inc) {

    TermData[] newMap = new TermData[(int)(this.map.length*inc)];

    for(TermData t : this.map) {
      add(newMap,t);
    }

    this.map = newMap;

  }

}
