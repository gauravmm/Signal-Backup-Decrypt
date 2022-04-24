package securesms.util;

public class Pair<T extends Comparable<T>, U extends Comparable<U>> implements Comparable {
    T p1;
    U p2;

    public Pair(T p1, U p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    @Override
    public int compareTo(Object o) {
        Pair<T, U> other = (Pair<T, U>) o;
        int rv = p1.compareTo(other.p1);
        if (rv == 0)
            return p2.compareTo(other.p2);
        return rv;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((p1 == null) ? 0 : p1.hashCode());
        result = prime * result + ((p2 == null) ? 0 : p2.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Pair<T, U> other = (Pair<T, U>) obj;
        if (p1 == null) {
            if (other.p1 != null)
                return false;
        } else if (!p1.equals(other.p1))
            return false;
        if (p2 == null) {
            if (other.p2 != null)
                return false;
        } else if (!p2.equals(other.p2))
            return false;
        return true;
    }

}
