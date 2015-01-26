package net.gamersconnection.forge;

import java.util.HashMap;

public class WorldHashMap<K, V> extends HashMap<K, V> {
	private static final long serialVersionUID = 1L;

	/*
	 * Write the WorldHashMap object in a python like format.
	 * 
	 * (non-Javadoc)
	 * @see java.util.AbstractMap#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		
		builder.append("{\n");
		for (K key : keySet()) {
			builder.append(String.format("    '%s': '%s',\n",key,get(key)));
		}
		builder.append("}\n");

		return builder.toString();
	}

}
