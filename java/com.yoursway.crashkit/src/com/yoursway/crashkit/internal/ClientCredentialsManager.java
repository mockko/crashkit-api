package com.yoursway.crashkit.internal;

import java.io.IOException;

import com.yoursway.crashkit.internal.model.ClientCredentials;
import com.yoursway.crashkit.internal.model.Repository;

public class ClientCredentialsManager {
	
	private final Repository storage;
	
	private ClientCredentials cachedInfo;

	private final ServerConnection communicator;
	
	public ClientCredentialsManager(Repository storage, ServerConnection communicator) {
		if (storage == null)
			throw new NullPointerException("storage is null");
		if (communicator == null)
			throw new NullPointerException("communicator is null");
		this.storage = storage;
		this.communicator = communicator;
	}
	
	public synchronized ClientCredentials get() throws IOException {
		if (cachedInfo == null) {
			cachedInfo = storage.readClientInfo();
			if (cachedInfo == null) {
				cachedInfo = communicator.obtainNewClientIdAndCookie();
				storage.writeClientInfo(cachedInfo);
			}
		}
		return cachedInfo;
	}

	public void reset() {
		storage.deleteClientInfo();
		cachedInfo = null;
	}

}
