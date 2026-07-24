/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2026 Karl Duderstadt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package de.mpg.biochem.mars.fx.molecule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveIndex;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;

/**
 * Retains molecule records loaded from a virtual archive around the current
 * selection, so {@link AbstractMoleculesTab} does not re-parse and re-write an
 * unchanged record on every selection change. Reads and writes run on a single
 * background thread so the FX thread never blocks on virtual store I/O.
 * <p>
 * Bypassed entirely for in-memory archives: {@code archive.get}/{@code put}
 * are already O(1) there and already retain a single instance per UID, so the
 * cache would only add a second identity for the same record.
 * </p>
 * <p>
 * Object identity is the load-bearing invariant here: the GUI mutates the
 * molecule it holds in place (tag hotkeys, notes editor, parameters table,
 * region/position handlers), so {@link #get(String)} must always return the
 * same retained instance for a UID as long as anything might still reference
 * it. A prefetch must never replace an already-cached instance.
 * </p>
 * All public methods are called from the FX thread. Only the I/O executor
 * thread touches {@code archive.get}/{@code archive.put}.
 */
public class MoleculeRecordCache {

	private final MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive;

	private final boolean virtual;

	// Synchronized because get()/prefetch tasks touch it from both the FX
	// thread and the I/O thread. Iteration (updateWindow, flushAndWait) must
	// hold the monitor manually per the Collections.synchronizedMap contract.
	private final Map<String, Molecule> cache = Collections.synchronizedMap(
		new LinkedHashMap<>());

	private final Set<String> inFlight = ConcurrentHashMap.newKeySet();

	private final ExecutorService ioExecutor = Executors
		.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r, "Mars-record-io");
			t.setDaemon(true);
			return t;
		});

	private volatile int lookAhead = 10;
	private volatile int lookBehind = 3;
	private volatile String selectedUID;

	public MoleculeRecordCache(
		MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties<Molecule, MarsMetadata>, MoleculeArchiveIndex<Molecule, MarsMetadata>> archive)
	{
		this.archive = archive;
		this.virtual = archive.isVirtual();
	}

	public void setLookAhead(int lookAhead) {
		this.lookAhead = Math.max(0, lookAhead);
	}

	public void setLookBehind(int lookBehind) {
		this.lookBehind = Math.max(0, lookBehind);
	}

	/**
	 * FX thread. Fast path on a cache hit; blocking deserialize on a miss (same
	 * cost as {@code archive.get} today).
	 */
	public Molecule get(String uid) {
		if (!virtual) return archive.get(uid);
		if (uid == null) return null;

		Molecule molecule = cache.get(uid);
		if (molecule != null) return molecule;

		molecule = archive.get(uid);
		if (molecule != null) cache.put(uid, molecule);
		return molecule;
	}

	/**
	 * FX thread, returns immediately. Only call this for a record known to be
	 * dirty ({@code isModified()}); the write happens on the I/O thread.
	 */
	public void saveAsync(Molecule molecule) {
		if (molecule == null) return;
		if (!virtual) {
			// put() still maintains indexes/properties for in-memory archives,
			// and is already effectively free, so just do it directly.
			archive.put(molecule);
			return;
		}

		String uid = molecule.getUID();
		inFlight.add(uid);
		ioExecutor.submit(() -> {
			try {
				archive.put(molecule);
			}
			finally {
				inFlight.remove(uid);
			}
		});
	}

	/**
	 * FX thread, returns immediately. Schedules prefetching of the window
	 * around {@code selectedIndex} and evicts anything now outside it.
	 * {@code visibleUIDs} must be in the same order the user scrolls, i.e. the
	 * filtered/visible list, not necessarily archive order.
	 */
	public void updateWindow(List<String> visibleUIDs, int selectedIndex) {
		if (!virtual) return;
		if (visibleUIDs == null || visibleUIDs.isEmpty()) return;

		selectedUID = (selectedIndex >= 0 &&
			selectedIndex < visibleUIDs.size()) ? visibleUIDs.get(
				selectedIndex) : null;

		int start = Math.max(0, selectedIndex - lookBehind);
		int end = Math.min(visibleUIDs.size(), selectedIndex + lookAhead + 1);

		Set<String> targetWindow = new LinkedHashSet<>();
		for (int i = start; i < end; i++)
			targetWindow.add(visibleUIDs.get(i));

		evictOutside(targetWindow);
		prefetch(targetWindow);
	}

	private void evictOutside(Set<String> targetWindow) {
		List<String> toEvict = new ArrayList<>();
		synchronized (cache) {
			for (String uid : cache.keySet()) {
				if (uid.equals(selectedUID)) continue;
				if (!targetWindow.contains(uid)) toEvict.add(uid);
			}
		}

		for (String uid : toEvict) {
			Molecule molecule = cache.remove(uid);
			// A save already queued for this UID holds its own reference to the
			// molecule and will complete correctly even after the map entry is
			// gone, so skip queuing a redundant duplicate write.
			if (molecule != null && molecule.isModified() && !inFlight.contains(
				uid)) saveAsync(molecule);
		}
	}

	private void prefetch(Set<String> targetWindow) {
		for (String uid : targetWindow) {
			// The selected UID is always loaded synchronously via get() before
			// updateWindow is called.
			if (uid.equals(selectedUID)) continue;
			if (cache.containsKey(uid) || inFlight.contains(uid)) continue;

			inFlight.add(uid);
			ioExecutor.submit(() -> {
				try {
					// Re-check inside the task: prefetching an already-cached UID
					// must never replace the retained instance with a fresh parse,
					// since that would discard any in-place edits made meanwhile.
					if (!cache.containsKey(uid)) {
						Molecule molecule = archive.get(uid);
						if (molecule != null) cache.put(uid, molecule);
					}
				}
				finally {
					inFlight.remove(uid);
				}
			});
		}
	}

	/**
	 * Drop a single cached UID, e.g. immediately before an explicit re-read
	 * from disk that must not be masked by a stale retained instance.
	 */
	public void invalidate(String uid) {
		cache.remove(uid);
	}

	/**
	 * Discard every cached entry without saving. Only safe to call right after
	 * {@link #flushAndWait()} has already run (e.g. following a bulk operation
	 * that owns the data and ran under the archive lock).
	 */
	public void invalidateAll() {
		cache.clear();
		selectedUID = null;
	}

	/**
	 * Blocks the calling thread until every queued read/write completes, then
	 * synchronously saves anything still dirty in the cache. Called from
	 * lock/close paths where a brief block is acceptable; never call this from
	 * the selection listener.
	 */
	public void flushAndWait() {
		if (!virtual) return;

		CountDownLatch drained = new CountDownLatch(1);
		ioExecutor.submit(drained::countDown);
		try {
			drained.await();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		List<Molecule> dirty = new ArrayList<>();
		synchronized (cache) {
			for (Molecule molecule : cache.values())
				if (molecule.isModified()) dirty.add(molecule);
		}
		for (Molecule molecule : dirty)
			archive.put(molecule);
	}

	/** Flushes dirty records, then stops the I/O thread. */
	public void shutdown() {
		flushAndWait();
		ioExecutor.shutdown();
	}
}
