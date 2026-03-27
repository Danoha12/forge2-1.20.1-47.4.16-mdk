package org.apache.commons.lang3.concurrent;

public interface CircuitBreaker<T> {
  boolean isOpen();
  
  boolean isClosed();
  
  boolean checkState();
  
  void close();
  
  void open();
  
  boolean incrementAndCheckState(T paramT);
}


/* Location:              C:\Users\geova\Downloads\Fapcraft 1.12.2 v1.1.jar!\org\apache\commons\lang3\concurrent\CircuitBreaker.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */