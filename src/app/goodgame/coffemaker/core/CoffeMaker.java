package app.goodgame.coffemaker.core;

import java.util.Collections;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CoffeMaker {

	// counters
	private static final AtomicInteger totalCounterPaidCoffee = new AtomicInteger();
	private static final AtomicInteger capuchinoCounterPaidCoffee = new AtomicInteger();
	private static final AtomicInteger lateCounterPaidCoffee = new AtomicInteger();
	private static final AtomicInteger espressoCounterPaidCoffee = new AtomicInteger();
	private static final AtomicLong avrSpendGetCoffee = new AtomicLong();
	private static final CopyOnWriteArrayList<Long> minMaxTime = new CopyOnWriteArrayList<Long>();

	// lock
	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	private final Lock readLock = readWriteLock.readLock();

	// limit
	private static final Integer COUNT_PROGRAMMERS_CHOOSING_COFFEE = 10;
	private static final Integer COUNT_PROGRAMMERS_PAY_COFFEE = 5;
	private static final Integer COUNT_PROGRAMMERS_TAKE_COFFEE = 2;

	private Semaphore limitChooseCoffe = new Semaphore(COUNT_PROGRAMMERS_CHOOSING_COFFEE);
	private Semaphore limitPayCoffe = new Semaphore(COUNT_PROGRAMMERS_PAY_COFFEE);
	private Semaphore limitMakeCoffe = new Semaphore(COUNT_PROGRAMMERS_TAKE_COFFEE);

	private CyclicBarrier counterProgrammers;

	public void await() {
		try {
			counterProgrammers.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			e.printStackTrace();
		}
	}

	public CoffeMaker(Integer countProgrammrs) {

		counterProgrammers = new CyclicBarrier(countProgrammrs, new Runnable() {
			public void run() {
				System.out.println("Capuchino Counter Paid Coffee=" + capuchinoCounterPaidCoffee.get());
				System.out.println("Late Counter Paid Coffee=" + lateCounterPaidCoffee.get());
				System.out.println("Espresso Counter Paid Coffee=" + espressoCounterPaidCoffee.get());
				System.out.println("Total paid coffee=" + totalCounterPaidCoffee.get());
				System.out.println("Avr Spend Get Coffee=" + (avrSpendGetCoffee.get() / countProgrammrs));
				System.out.println("Max time=" + Collections.max(minMaxTime));
				System.out.println("Min time=" + Collections.min(minMaxTime));
				System.out.println("End operations");
			}
		});
	}

	public void addTimeValue(Long time) {
		avrSpendGetCoffee.addAndGet(time);

	}

	public void addTimeList(Long time) {
		minMaxTime.add(time);
	}

	public void limitMakeCoffeAcquire() {
		try {
			limitMakeCoffe.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void limitMakeCoffeRelease() {
		limitMakeCoffe.release();
	}

	public void limitChooseCoffeAcquire() {
		try {
			limitChooseCoffe.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void limitChooseCoffeRelease() {
		limitChooseCoffe.release();
	}

	public void limitPayCoffeAcquire() {
		try {
			limitPayCoffe.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void limitPayCoffeRelease() {
		limitPayCoffe.release();
	}

	public Long chooseTypeCoffie(CoffeeType coffeeType) {

		readLock.lock();
		Long delay = coffeeType.getTime();
		switch (coffeeType) {
		case CAPPUCCINO:
			capuchinoCounterPaidCoffee.incrementAndGet();
			break;
		case LATTE_MACCHIATO:
			lateCounterPaidCoffee.incrementAndGet();
			break;
		case ESPRESSO:
			espressoCounterPaidCoffee.incrementAndGet();
			break;
		}

		readLock.unlock();

		return delay;
	}

	public Long paidCoffee(PaymentsType paymentsType) {
		// readLock.lock();
		// Long delay = paymentsType.getTime();
		// readLock.unlock();
		totalCounterPaidCoffee.incrementAndGet();
		return paymentsType.getTime();
	}

	public void makeCoffee(Long timeDelayMaking) {
		// readLock.lock();
		try {
			Thread.sleep(timeDelayMaking);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// readLock.unlock();

	}

	public static void main(String[] args) {
		Integer programmers = 100;

		CoffeMaker coffeMaker = new CoffeMaker(programmers);
		ExecutorService executor = Executors.newFixedThreadPool(programmers);

		for (int i = 0; i < 10; i++) {
			executor.submit(new Programmer(coffeMaker, CoffeeType.LATTE_MACCHIATO, PaymentsType.CREDIT_CARD));
		}

		for (int i = 0; i < 10; i++) {
			executor.submit(new Programmer(coffeMaker, CoffeeType.LATTE_MACCHIATO, PaymentsType.CACHE));
		}

		for (int i = 0; i < 10; i++) {
			executor.submit(new Programmer(coffeMaker, CoffeeType.ESPRESSO, PaymentsType.CREDIT_CARD));
		}

		for (int i = 0; i < 30; i++) {
			executor.submit(new Programmer(coffeMaker, CoffeeType.ESPRESSO, PaymentsType.CACHE));
		}

		for (int i = 0; i < 20; i++) {
			executor.submit(new Programmer(coffeMaker, CoffeeType.CAPPUCCINO, PaymentsType.CREDIT_CARD));

		}

		for (int i = 0; i < 20; i++) {

			executor.submit(new Programmer(coffeMaker, CoffeeType.CAPPUCCINO, PaymentsType.CACHE));
		}

		executor.shutdown();
	}

}
