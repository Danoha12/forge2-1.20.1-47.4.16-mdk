package org.apache.commons.lang3.concurrent;

public interface ConcurrentInitializer<T> {
  T get() throws ConcurrentException;
}


/* Location:              C:\Users\geova\Downloads\Fapcraft 1.12.2 v1.1.jar!\org\apache\commons\lang3\concurrent\ConcurrentInitializer.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */