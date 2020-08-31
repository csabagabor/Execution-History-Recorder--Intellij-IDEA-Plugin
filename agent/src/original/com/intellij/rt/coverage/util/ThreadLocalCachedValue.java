package original.com.intellij.rt.coverage.util;

import java.lang.ref.SoftReference;

abstract class ThreadLocalCachedValue<T> {
   private final ThreadLocal<SoftReference<T>> myThreadLocal = new ThreadLocal();

   public T getValue() {
      T value = dereference(this.myThreadLocal.get());
      if (value == null) {
         value = this.create();
         this.myThreadLocal.set(new SoftReference<>(value));
      }

      return value;
   }

   protected abstract T create();

   private static <T> T dereference(SoftReference<T> ref) {
      return ref == null ? null : ref.get();
   }
}
