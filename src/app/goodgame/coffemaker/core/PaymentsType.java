package app.goodgame.coffemaker.core;

public enum PaymentsType {
	CREDIT_CARD(250L), CACHE(500L);
	private Long time;

	PaymentsType(Long time) {
		this.time = time;
	}

	public Long getTime() {
		return this.time;
	}

}
