package com.fasterxml.jackson.databind.util;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper for simple bounded maps used for reusing lookup values.
 *<p>
 * Note that serialization behavior is such that contents are NOT serialized,
 * on assumption that all use cases are for caching where persistence
 * does not make sense. The only thing serialized is the cache size of Map.
 *<p>
 * NOTE: since version 2.4.2, this is <b>NOT</b> an LRU-based at all; reason
 * being that it is not possible to use JDK components that do LRU _AND_ perform
 * well wrt synchronization on multi-core systems. So we choose efficient synchronization
 * over potentially more effecient handling of entries.
 */
public class LRUMap<K,V>
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    protected final transient int _maxEntries;

    protected final transient ConcurrentHashMap<K,V> _map;
    
    public LRUMap(int initialEntries, int maxEntries)
    {
        // We'll use concurrency level of 4, seems reasonable
        _map = new ConcurrentHashMap<K,V>(initialEntries, 0.8f, 4);
        _maxEntries = maxEntries;
    }

    public void put(K key, V value) {
        if (_map.size() >= _maxEntries) {
            // double-locking, yes, but safe here; trying to avoid "clear storms"
            synchronized (this) {
                if (_map.size() >= _maxEntries) {
                    clear();
                }
            }
        }
        _map.put(key, value);
    }

    // NOTE: key is of type Object only to retain binary backwards-compatibility
    public V get(Object key) {  return _map.get(key); }

    public void clear() { _map.clear(); }
    public int size() { return _map.size(); }

    /*
    /**********************************************************
    /* Serializable overrides
    /**********************************************************
     */

    /**
     * Ugly hack, to work through the requirement that _value is indeed final,
     * and that JDK serialization won't call ctor(s) if Serializable is implemented.
     * 
     * @since 2.1
     */
    protected transient int _jdkSerializeMaxEntries;

    private void readObject(ObjectInputStream in) throws IOException {
        _jdkSerializeMaxEntries = in.readInt();
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(_jdkSerializeMaxEntries);
    }

    protected Object readResolve() {
        return new LRUMap<Object,Object>(_jdkSerializeMaxEntries, _jdkSerializeMaxEntries);
    }
}
