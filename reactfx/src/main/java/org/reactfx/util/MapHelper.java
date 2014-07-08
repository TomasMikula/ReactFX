package org.reactfx.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public abstract class MapHelper<K, V> {

    public static <K, V> MapHelper<K, V> put(MapHelper<K, V> mapHelper, K key, V value) {
        if(mapHelper == null) {
            return new SingleEntryHelper<>(key, value);
        } else {
            return mapHelper.put(key, value);
        }
    }

    public static <K, V> V get(MapHelper<K, V> mapHelper, K key) {
        if(mapHelper == null) {
            return null;
        } else {
            return mapHelper.get(key);
        }
    }

    public static <K, V> MapHelper<K, V> remove(MapHelper<K, V> mapHelper, K key) {
        if(mapHelper == null) {
            return mapHelper;
        } else {
            return mapHelper.remove(key);
        }
    }

    public static <K, V> K chooseKey(MapHelper<K, V> mapHelper) {
        if(mapHelper == null) {
            throw new NoSuchElementException("empty map");
        } else {
            return mapHelper.chooseKey();
        }
    }

    public static <K, V> void replaceAll(
            MapHelper<K, V> mapHelper,
            BiFunction<? super K, ? super V, ? extends V> f) {
        if(mapHelper != null) {
            mapHelper.replaceAll(f);
        }
    }

    public static <K, V> void forEach(MapHelper<K, V> mapHelper, BiConsumer<K, V> f) {
        if(mapHelper != null) {
            mapHelper.forEach(f);
        }
    }

    public static <K, V> boolean isEmpty(MapHelper<K, V> mapHelper) {
        return mapHelper == null;
    }

    public static <K, V> int size(MapHelper<K, V> mapHelper) {
        if(mapHelper == null) {
            return 0;
        } else {
            return mapHelper.size();
        }
    }

    public static <K> boolean containsKey(MapHelper<K, ?> mapHelper, K key) {
        if(mapHelper == null) {
            return false;
        } else {
            return mapHelper.containsKey(key);
        }
    }

    private MapHelper() {
        // private constructor to prevent subclassing
    };

    protected abstract MapHelper<K, V> put(K key, V value);

    protected abstract V get(K key);

    protected abstract MapHelper<K, V> remove(K key);

    protected abstract K chooseKey();

    protected abstract void replaceAll(BiFunction<? super K, ? super V, ? extends V> f);

    protected abstract boolean containsKey(K key);

    protected abstract void forEach(BiConsumer<K, V> f);

    protected abstract int size();


    private static class SingleEntryHelper<K, V> extends MapHelper<K, V> {
        private final K key;
        private V value;

        public SingleEntryHelper(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        protected MapHelper<K, V> put(K key, V value) {
            if(Objects.equals(key, this.key)) {
                return new SingleEntryHelper<>(key, value);
            } else {
                return new MultiEntryHelper<>(this.key, this.value, key, value);
            }
        }

        @Override
        protected V get(K key) {
            return Objects.equals(key, this.key) ? value : null;
        }

        @Override
        protected MapHelper<K, V> remove(K key) {
            if(Objects.equals(key, this.key)) {
                return null;
            } else {
                return this;
            }
        }

        @Override
        protected K chooseKey() {
            return key;
        }

        @Override
        protected void replaceAll(BiFunction<? super K, ? super V, ? extends V> f) {
            value = f.apply(key, value);
        }

        @Override
        protected void forEach(BiConsumer<K, V> f) {
            f.accept(key, value);
        }

        @Override
        protected int size() {
            return 1;
        }

        @Override
        protected boolean containsKey(K key) {
            return Objects.equals(key, this.key);
        }
    }

    private static class MultiEntryHelper<K, V> extends MapHelper<K, V> {
        private final Map<K, V> entries = new HashMap<>();

        public MultiEntryHelper(K k1, V v1, K k2, V v2) {
            entries.put(k1, v1);
            entries.put(k2, v2);
        }

        @Override
        protected MapHelper<K, V> put(K key, V value) {
            entries.put(key, value);
            return this;
        }

        @Override
        protected V get(K key) {
            return entries.get(key);
        }

        @Override
        protected MapHelper<K, V> remove(K key) {
            entries.remove(key);
            switch(entries.size()) {
                case 0:
                    return null;
                case 1:
                    @SuppressWarnings("unchecked")
                    Entry<K, V> entry = (Entry<K, V>) entries.entrySet().toArray(new Entry<?, ?>[1])[0];
                    return new SingleEntryHelper<>(entry.getKey(), entry.getValue());
                default:
                    return this;
            }
        }

        @Override
        protected K chooseKey() {
            return entries.keySet().iterator().next();
        }

        @Override
        protected void replaceAll(BiFunction<? super K, ? super V, ? extends V> f) {
            entries.replaceAll(f);
        }

        @Override
        protected void forEach(BiConsumer<K, V> f) {
            for(Object entry: entries.entrySet().toArray()) {
                @SuppressWarnings("unchecked")
                Entry<K, V> e = (Entry<K, V>) entry;
                f.accept(e.getKey(), e.getValue());
            }
        }

        @Override
        protected int size() {
            return entries.size();
        }

        @Override
        protected boolean containsKey(K key) {
            return entries.containsKey(key);
        }
    }
}
