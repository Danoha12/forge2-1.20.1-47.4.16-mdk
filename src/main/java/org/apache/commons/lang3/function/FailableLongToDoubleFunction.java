/*    */ package org.apache.commons.lang3.function;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ @FunctionalInterface
/*    */ public interface FailableLongToDoubleFunction<E extends Throwable>
/*    */ {
/*    */   public static final FailableLongToDoubleFunction NOP = t -> 0.0D;
/*    */   
/*    */   double applyAsDouble(long paramLong) throws E;
/*    */   
/*    */   static <E extends Throwable> FailableLongToDoubleFunction<E> nop() {
/* 42 */     return NOP;
/*    */   }
/*    */ }


/* Location:              C:\Users\geova\Downloads\Fapcraft 1.12.2 v1.1.jar!\org\apache\commons\lang3\function\FailableLongToDoubleFunction.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */