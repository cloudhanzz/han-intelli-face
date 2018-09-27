package han.cloud.ai.util;

/**
 * A convenient generic class for holding a key-value pair.
 * 
 * @author Jiayun Han
 * 
 * @param <K>
 *            The parameter Type of the key
 * @param <V>
 *            The parameter Type of the value
 */
public final class KeyValuePair<K, V> {

	private K k;
	private V v;

	public KeyValuePair(K k, V v) {
		this.k = k;
		this.v = v;
	}

	public K getKey() {
		return k;
	}

	public V getValue() {
		return v;
	}

	public void setKey(K k) {
		this.k = k;
	}

	public void setValue(V v) {
		this.v = v;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((k == null) ? 0 : k.hashCode());
		result = prime * result + ((v == null) ? 0 : v.hashCode());
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
		KeyValuePair<?, ?> other = (KeyValuePair<?, ?>) obj;
		if (k == null) {
			if (other.k != null)
				return false;
		} else if (!k.equals(other.k))
			return false;
		if (v == null) {
			if (other.v != null)
				return false;
		} else if (!v.equals(other.v))
			return false;
		return true;
	}
}
