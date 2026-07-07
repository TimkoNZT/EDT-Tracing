package com.e1c.edt.ai.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class JShellObjectBridge {
   private static final Map<Integer, Object> objectStore = new HashMap();
   private static final AtomicInteger counter = new AtomicInteger(0);
   private static final Map<Integer, Set<Integer>> sessionObjectIds = new ConcurrentHashMap();

   public static int store(int sessionId, Object obj) {
      int id = counter.incrementAndGet();
      objectStore.put(id, obj);
      ((Set)sessionObjectIds.computeIfAbsent(sessionId, (k) -> ConcurrentHashMap.newKeySet())).add(id);
      return id;
   }

   public static <T> T retrieve(int sessionId, int id) {
      Set<Integer> sessionIds = (Set)sessionObjectIds.get(sessionId);
      return (T)(sessionIds != null && sessionIds.contains(id) ? objectStore.get(id) : null);
   }

   public static void releaseSession(int sessionId) {
      Set<Integer> ids = (Set)sessionObjectIds.remove(sessionId);
      if (ids != null) {
         Map var10001 = objectStore;
         ids.forEach(var10001::remove);
      }

   }
}
