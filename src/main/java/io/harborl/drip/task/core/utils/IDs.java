package io.harborl.drip.task.core.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A time stamp based, domain oriented random universal id generator.
 * It can be used to generate unique id for a specified domain.
 * <p/>
 * You can add an specified domain id generator through defining
 * a domain enumeration with a domain code.
 * 
 * @author Harbor Luo
 * @version v0.0.1
 *
 */
public enum IDs {

  A(1),
  B(2),
  MAIL(11)
  ;

  private final int domainCode;
  private IDs(int domainCode) {
    if (domainCode > 0x7FFF) throw new AssertionError("domainCode overflow");
    this.domainCode = domainCode;
  }

  private static final int DBITS = 16;
  private static final AtomicInteger idInc = new AtomicInteger(1);
  public long gen() {
    int internalCode = idInc.incrementAndGet();
    long randomCode = System.nanoTime();
    randomCode += new Random().nextLong() + internalCode;
    randomCode = (randomCode) & 0x0000FFFFFFFFFFFFL;
    long domainHigh = (domainCode & 0x0FFFFL) << (64 - DBITS);

    return domainHigh | randomCode;
  }

  private static Map<Integer, IDs> domainById;
  static {
    Map<Integer, IDs> swap = new HashMap<Integer, IDs>();
    for (IDs id : IDs.values()) {
      swap.put(id.domainCode, id);
    }
    domainById = Collections.unmodifiableMap(swap);
  }

  public static IDs valueOf(long id) {
    int domainCode = (int) ((id & 0xFFFF000000000000L) >>> (64 - DBITS));
    return domainById.get(domainCode);
  }
  
  static class BarrierTimer implements Runnable {
    private boolean started;
    private long start, end;
    @Override public synchronized void run() {
      if (started == true) {
        end = System.nanoTime();
      } else {
        started = true;
        start = System.nanoTime();
      }
    }

    public synchronized void clear() { started = false; }
    public synchronized long getTime() { return end - start; }
  }
  
  private static void await(CyclicBarrier barrier) {
    try {
      barrier.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (BrokenBarrierException e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) throws InterruptedException {
    final Set<Long> idSet = new HashSet<Long>();
    final int LEVEL = Runtime.getRuntime().availableProcessors() + 1;
    ExecutorService detector = 
        Executors.newFixedThreadPool(LEVEL); 

    final int M = 1000000;
    final AtomicInteger failedCount = new AtomicInteger();
    BarrierTimer timer = new BarrierTimer();
    timer.clear();
    // [JCIP code 12-5, 12-11]
    final CyclicBarrier barrier = new CyclicBarrier(LEVEL + 1, timer);
    for (int i = 0; i < LEVEL; ++i) {

      detector.submit(new Runnable() {
  
        @Override
        public void run() {
          await(barrier);
          
          for (int i = 0; i < M; ++i) {
            long id = IDs.A.gen();
            synchronized(idSet) {
              if (!idSet.add(id)) {
                System.out.println("Failed - confict - " + id);
                failedCount.incrementAndGet();
              } else if (IDs.valueOf(id) != IDs.A) {
                System.out.println("Failed - convert - " + id);
              } else if (idSet.size() % 1000000 == 0) {
                System.out.println(id);
              }
            }
          }
          
          await(barrier);
        }
      });
    }

    await(barrier);
    await(barrier);
    
    detector.shutdown();
    detector.awaitTermination(3, TimeUnit.SECONDS);
    
    System.out.println("conflict ratio : " + ((double)failedCount.get()/(LEVEL*M)));
    System.out.println("time: " + TimeUnit.NANOSECONDS.toSeconds(timer.getTime()) + " s");
    System.out.println("done - " + detector.isTerminated());
  }
}
