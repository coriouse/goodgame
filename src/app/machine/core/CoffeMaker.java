package app.machine.core;

import java.lang.management.ManagementFactory;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import app.machine.core.CoffeMaker.CoffeeType;
import app.machine.core.CoffeMaker.PaymentsType;

public class CoffeMaker {

	private static final Integer COUNT_PROGRAMMERS_CHOOSING_COFFEE = 1;
	private static final Integer COUNT_PROGRAMMERS_PAY_COFFEE = 5;

	public enum CoffeeType {
		ESPRESSO(250L), LATTE_MACCHIATO(5000L), CAPPUCCINO(1000L);
		private Long time;

		CoffeeType(Long time) {
			this.time = time;
		}

		public Long getTime() {
			return this.time;
		}
	}

	public enum PaymentsType {
		CREDIT_CARD(1000L), CACHE(500L);
		private Long time;

		PaymentsType(Long time) {
			this.time = time;
		}

		public Long getTime() {
			return this.time;
		}

	}

	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	private final Lock readLock = readWriteLock.readLock();

	private Semaphore limitChooseCoffe = new Semaphore(COUNT_PROGRAMMERS_CHOOSING_COFFEE);
	private Semaphore limitPayCoffe = new Semaphore(COUNT_PROGRAMMERS_PAY_COFFEE);

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

		 readLock.unlock();

		return delay;
	}

	public Long paidCoffee(PaymentsType paymentsType) {
		 readLock.lock();
		Long delay = paymentsType.getTime();

		 readLock.unlock();
		return delay;
	}

	public void makeCoffee() {
		System.out.println("find cup");
		System.out.println("put it under the outlet");
		System.out.println("pick the type of coffee the programmer paid");
		System.out.println("wait till the machine is finished filling the cup");
		System.out.println("when the mashine is done take the cup");

	}

	public static void main(String[] args) {
		CoffeMaker coffeMaker = new CoffeMaker();
		ExecutorService executor = Executors.newFixedThreadPool(5);
		executor.submit(new Programmer(coffeMaker, CoffeeType.LATTE_MACCHIATO, PaymentsType.CREDIT_CARD));
		 executor.submit(new Programmer(coffeMaker, CoffeeType.CAPPUCCINO, PaymentsType.CACHE));
		executor.submit(new Programmer(coffeMaker, CoffeeType.LATTE_MACCHIATO, PaymentsType.CREDIT_CARD));
		executor.submit(new Programmer(coffeMaker, CoffeeType.LATTE_MACCHIATO, PaymentsType.CREDIT_CARD));
		
		 executor.submit(new Programmer(coffeMaker, CoffeeType.LATTE_MACCHIATO, PaymentsType.CREDIT_CARD));
		 executor.submit(new Programmer(coffeMaker, CoffeeType.ESPRESSO, PaymentsType.CREDIT_CARD));
		 
		executor.shutdown();
	}

}

class Programmer implements Runnable {

	private final CoffeMaker coffeMaker;
	private final CoffeeType coffeeType;
	private final PaymentsType paymentsType;
	
	private static final ThreadLocal<Long> TIME_COUNTER = new ThreadLocal<Long>();  

	public Programmer(CoffeMaker coffeMaker, CoffeeType coffeeType, PaymentsType paymentsType) {
		this.coffeMaker = coffeMaker;
		this.coffeeType = coffeeType;
		this.paymentsType = paymentsType;
	}

	public void run() {		
		
		String threadName = Thread.currentThread().getName();
		Long delayPaidCoffee = null, delayChoosingCoffee = null;
		
		ExecutorService coffeeProcessing = Executors.newFixedThreadPool(2);

		// Choose coffee
		Future<Long> chooseCoffee = coffeeProcessing.submit(new Callable<Long>() {
			public Long call() throws InterruptedException {
				coffeMaker.limitChooseCoffeAcquire();
				Long start = System.currentTimeMillis();
				Long timeOut = coffeMaker.chooseTypeCoffie(coffeeType);
				Thread.sleep(timeOut);
				TIME_COUNTER.set((TIME_COUNTER.get()+(System.currentTimeMillis()-start)));
				coffeMaker.limitChooseCoffeRelease();
				
				return timeOut;
			}
		});
		try {
			System.out.println("Start Choose coffee! " + threadName);
			delayChoosingCoffee = chooseCoffee.get();
			System.out.println("Finished Choose coffee=" + delayChoosingCoffee);
		} catch (InterruptedException | ExecutionException e) {
			e.getStackTrace();
			System.out.println("Terminated!");
		} finally {
			System.out.println("Stop Choose coffee! " + threadName);
		}

		// coffee paid
		Future<Long> paidCoffee = coffeeProcessing.submit(new Callable<Long>() {
			public Long call() throws InterruptedException {
				coffeMaker.limitPayCoffeAcquire();
				Long start = System.currentTimeMillis();
				Long timeOut = coffeMaker.paidCoffee(paymentsType);
				Thread.sleep(timeOut);
				TIME_COUNTER.set((TIME_COUNTER.get()+(System.currentTimeMillis()-start)));
				coffeMaker.limitPayCoffeRelease();
				return timeOut;
			}
		});
		
		try {
			System.out.println("Start coffee paid! " + threadName);
			delayPaidCoffee = paidCoffee.get();
			System.out.println("Finished=" + delayPaidCoffee);
		} catch (InterruptedException | ExecutionException e) {
			e.getStackTrace();
			System.out.println("Terminated!");
		} finally {
			System.out.println("Stop coffee paid! " + threadName);
		}

		// coffee making
		/*
		 * Future<Boolean> makingCoffee = coffeeProcessing.submit(new
		 * Callable<Boolean>() { public Boolean call() throws
		 * InterruptedException { Long timeOut = coffeMaker.makeCoffee();
		 * Thread.sleep(5000); return true; } }); try {
		 * System.out.println("Start!"); Long isChoosen = chooseCoffee.get(5000,
		 * TimeUnit.MILLISECONDS); System.out.println("Finished="+isChoosen); }
		 * catch (InterruptedException | ExecutionException | TimeoutException
		 * e) { e.getStackTrace(); chooseCoffee.cancel(true);
		 * System.out.println("Terminated!"); }
		 */

		coffeeProcessing.shutdown();
		System.out.println(threadName+" is finished!!!!!!! "+(delayChoosingCoffee+delayPaidCoffee)+" real time is "+TIME_COUNTER.get());
	}
}
