package org.apache.commons.lang3.function;

@FunctionalInterface
public interface FailableIntBinaryOperator<E extends Throwable> {
  int applyAsInt(int paramInt1, int paramInt2) throws E;
}


/* Location:              C:\Users\geova\Downloads\Fapcraft 1.12.2 v1.1.jar!\org\apache\commons\lang3\function\FailableIntBinaryOperator.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */