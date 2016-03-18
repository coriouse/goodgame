package app.goodgame.coffemaker.core;

public enum CoffeeType {
	ESPRESSO(250L), LATTE_MACCHIATO(500L), CAPPUCCINO(750L);
	private Long time;

	CoffeeType(Long time) {
		this.time = time;
	}

	public Long getTime() {
		return this.time;
	}
}
