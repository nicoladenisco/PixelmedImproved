/* Copyright (c) 2001-2005, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import java.util.Timer;
import java.util.TimerTask;

/**
 * <p>This abstract class provides a source of DICOM network parameters.</p>
 *
 * @author	dclunie
 */
abstract public class NetworkConfigurationSource {

	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/network/NetworkConfigurationSource.java,v 1.1.1.1 2011-05-21 10:08:47 nicola Exp $";
	
	protected static final int DefaultDumpInfoInterval = 10*1000;	// milliseconds
	
	protected static final int DefaultSourceRefreshInterval = 60*60*1000;	// milliseconds
	
	protected int debugLevel;

	private final NetworkApplicationInformation networkApplicationInformation = new NetworkApplicationInformation();
	
	/**
	 * <p>Return the network application information.</p>
	 *
	 * <p>Synchronized since the information may be dynamically updated whilst accessible by other threads.</p>
	 *
	 * @return	the network application information
	 */
	public synchronized NetworkApplicationInformation getNetworkApplicationInformation() { return networkApplicationInformation; }

	protected final Timer timer = new Timer();
	
	protected class DumpNetworkApplicationInformation extends TimerTask {
	
		private int dumpInfoInterval;
		
		DumpNetworkApplicationInformation(int dumpInfoInterval) {
			this.dumpInfoInterval = dumpInfoInterval;
		}
		
		public void run() {
			System.err.println(getNetworkApplicationInformation().toString());
		}
		
		void start() {
			timer.schedule(this,dumpInfoInterval,dumpInfoInterval);
		}
	}
	
	protected DumpNetworkApplicationInformation dumper;
	
	/**
	 * <p>Start dumping current configuration information at regular intervals.</p>
	 */
	public final void activateDumper() {
		activateDumper(DefaultDumpInfoInterval);
	}
	
	/**
	 * <p>Start dumping current configuration information at specified intervals.</p>
	 *
	 * @param	dumpInfoInterval	interval in milliseconds
	 */
	public void activateDumper(int dumpInfoInterval) {
		if (dumper == null) {
			dumper = new DumpNetworkApplicationInformation(dumpInfoInterval);
		}
		dumper.start();
	}
	
	/**
	 * <p>Stop dumping current configuration information.</p>
	 */
	public void deActivateDumper() {
		if (dumper != null) {
			dumper.cancel();	// needed, since otherwise application will not exit when main thread finished
		}
	}

	/**
	 * <p>Construct an instance capable of returning configuration information but do not start anything yet.</p>
	 *
	 * @param	debugLevel
	 */
	protected NetworkConfigurationSource(int debugLevel) {
		this.debugLevel=debugLevel;
	}
	
	/**
	 * <p>Close down any running threads related to an instance of this class.</p>
	 */
	public final void close() {
		deActivateDiscovery();
		deActivateDumper();
	}

	/**
	 * <p>Start discovery of network configuration, if possible.</p>
	 */
	public final void activateDiscovery() {
		activateDiscovery(DefaultSourceRefreshInterval);
	}

	/**
	 * <p>Start discovery of network configuration, if possible.</p>
	 *
	 * @param	refreshInterval	interval to refresh configuration in milliseconds, 0 if no refresh (runs once only); may be ignored if source is asynchronous
	 */
	abstract public void activateDiscovery(int refreshInterval);
	
	/**
	 * <p>Stop discovery.</p>
	 */
	abstract public void deActivateDiscovery();
}
