package com.ntros.mprocswift.service.transfer;

import org.junit.jupiter.api.Test;

import java.util.*;

public class ProdConsTest {

  private final List<Integer> buffer = new ArrayList<>();
  private static final int CAPACITY = 5;
  private final Object lock = new Object();
  private boolean done = false;
  private volatile int cycles = 1;

  @Test
  public void prodConsTest() {
    Thread prod = new Thread(this::produce, "prod-thread");
    Thread cons = new Thread(this::consume, "cons-thread");

    prod.start();
    cons.start();

    try {
      prod.join();
      cons.join();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
    System.out.println("IN MAIN THREAD: Program finished");
  }

  // remove duplicates and keep insertion order
  // can be solved by:
  // 1. LinkedHashSet
  // 2. HashSet + List
  @Test
  public void x() {
    System.out.println("res: " + removeDuplicates(Arrays.asList(1, 2, 1, 2, 3, 4, 5)));
  }

  @Test
  public void y() {

    String abba = "abba";

    String car = "car";
    System.out.println(isPalindrome(abba));
  }

  private boolean isPalindrome(String word) {
    if (word == null) {
      return false;
    }
    for (int i = 0, j = word.length() - 1; i < j; i++, j--) {
      if (word.charAt(i) != word.charAt(j)) {
        return false;
      }
    }
    return true;
  }

  private String reverse(String word) {
    if (word == null) return null;

    char[] arr = word.toCharArray();

    for (int i = 0, j = arr.length - 1; i < j; i++, j--) {
      char t = arr[i];
      arr[i] = arr[j];
      arr[j] = t;
    }
    return new String(arr);
  }

  private List<Integer> removeDuplicates(List<Integer> arr) {
    Set<Integer> s = new HashSet<>(); // detects duplicates
    List<Integer> res = new ArrayList<>(); // preserves insertion order

    for (int item : arr) {
      if (!s.contains(item)) {
        res.add(item);
        s.add(item);
      }
    }

    return res;
  }

  private void produce() {
    int c = 1;
    while (!done) {
      synchronized (lock) {
        while (buffer.size() == CAPACITY) {
          await();
          c = 1;
        }
        buffer.add(c++);
        System.out.println("prod: add " + c);
        if (c == CAPACITY) {
          c = 1;
        }
        if (cycles == 12) {
          done = true;
        }
        cycles++;
        lock.notifyAll();
        sleep(100);
      }
    }
  }

  private void consume() {
    while (!done) {
      synchronized (lock) {
        while (buffer.isEmpty()) {
          await();
        }

        while (done && !buffer.isEmpty()) {
          System.out.println("cons: remove " + buffer.remove(0));
          System.out.println("Buffer size: " + buffer.size());
        }
        if (!buffer.isEmpty()) {
          System.out.println("cons: remove " + buffer.remove(0));
          System.out.println("Buffer size: " + buffer.size());
        }
        lock.notifyAll();
        sleep(100);
      }
    }
  }

  private void await() {
    try {
      lock.wait();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void sleep(int ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }
}
