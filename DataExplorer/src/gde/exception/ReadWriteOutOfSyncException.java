package osde.exception;


public class ReadWriteOutOfSyncException extends Exception {
	static final long serialVersionUID = 26031957;

	public ReadWriteOutOfSyncException(String message) {
		super(message);
	}

}
