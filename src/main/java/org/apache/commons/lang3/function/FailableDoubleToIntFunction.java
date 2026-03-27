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
/*    */ public interface FailableDoubleToIntFunction<E extends Throwable>
/*    */ {
/*    */   public static final FailableDoubleToIntFunction NOP = t -> 0;
/*    */   
/*    */   int applyAsInt(double paramDouble) throws E;
/*    */   
/*    */   static <E extends Throwable> FailableDoubleToIntFunction<E> nop() {
/* 42 */     return NOP;
/*    */   }
/*    */ }


/* Location:              C:\Users\geova\Downloads\Fapcraft 1.12.2 v1.1.jar!\org\apache\commons\lang3\function\FailableDoubleToIntFunction.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */