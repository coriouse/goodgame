package app.goodgame.coffemaker.core;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public class Programmer implements Runnable {

	private final CoffeMaker coffeMaker;
	private final CoffeeType coffeeType;
	private final PaymentsType paymentsType;
	private final AtomicLong timeSpendGetCoffee = new AtomicLong();
	private static final Integer CHOOSE_COFFEE_TIME = 500;

	public Programmer(CoffeMaker coffeMaker, CoffeeType coffeeType, PaymentsType paymentsType) {
		this.coffeMaker = coffeMaker;
		this.coffeeType = coffeeType;
		this.paymentsType = paymentsType;
	}

	public void run() {

		String threadName = Thread.currentThread().getName();
		Long delayChoosingCoffee = null;

		ExecutorService coffeeProcessing = Executors.newFixedThreadPool(2);

		// Choose coffee
		final Future<Long> chooseCoffee = coffeeProcessing.submit(new Callable<Long>() {
			public Long call() throws InterruptedException {
				coffeMaker.limitChooseCoffeAcquire();
				Long startTime = System.currentTimeMillis();
				System.out.println("Start Choose coffee! " + threadName);
				Long timeOut = coffeMaker.chooseTypeCoffie(coffeeType);
				Thread.sleep(CHOOSE_COFFEE_TIME);
				coffeMaker.limitChooseCoffeRelease();
				timeSpendGetCoffee.addAndGet((System.currentTimeMillis() - startTime));
				System.out.println("Finished Choose coffee!" + threadName);
				return timeOut;
			}
		});
		try {
			delayChoosingCoffee = chooseCoffee.get();

		} catch (InterruptedException | ExecutionException e) {
			e.getStackTrace();
		}
		// coffee paid
		Future<Long> paidCoffee = coffeeProcessing.submit(new Callable<Long>() {
			public Long call() throws InterruptedException {
				coffeMaker.limitPayCoffeAcquire();
				Long startTime = System.currentTimeMillis();
				System.out.println("Start coffee paid! " + threadName);
				Long timeOut = coffeMaker.paidCoffee(paymentsType);
				Thread.sleep(timeOut);
				coffeMaker.limitPayCoffeRelease();
				System.out.println("Finished coffee paid!" + threadName);
				timeSpendGetCoffee.addAndGet((System.currentTimeMillis() - startTime));
				return timeOut;
			}
		});

		try {
			paidCoffee.get();
		} catch (InterruptedException | ExecutionException e) {
			e.getStackTrace();
		}

		// coffee making

		Future<Boolean> makingCoffee = coffeeProcessing.submit(new Callable<Boolean>() {
			public Boolean call() throws InterruptedException, ExecutionException {
				coffeMaker.limitMakeCoffeAcquire();

				System.out.println("Start coffee making!" + threadName);

				Long startTime = System.currentTimeMillis();
				// find cup
				Thread.sleep(250);
				// put it under the outlet
				Thread.sleep(250);
				// pick the type of coffee the programmer paid
				Thread.sleep(250);
				// wait till the machine is finished filling the cup
				// coffeMaker.makeCoffee(chooseCoffee.get());
				Thread.sleep(chooseCoffee.get());
				// when the mashine is done take the cup
				Thread.sleep(250);
				System.out.println("Finished coffee making!" + threadName);
				coffeMaker.limitMakeCoffeRelease();
				timeSpendGetCoffee.addAndGet((System.currentTimeMillis() - startTime));
				return true;
			}
		});
		try {

			makingCoffee.get();

		} catch (InterruptedException | ExecutionException e) {
			e.getStackTrace();
		}

		coffeeProcessing.shutdown();
		long time = timeSpendGetCoffee.get();
		coffeMaker.addTimeValue(time);
		coffeMaker.addTimeList(time);
		System.out.println(threadName + " is finished!!!!!!! Spend time=" + time);
		coffeMaker.await();
	}
}
