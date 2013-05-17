package fr.ens.transcriptome.aozan.collectors.interopfile;

import java.util.List;

public class LaneRead implements Comparable<LaneRead> {
	private int lane;
	private int read;

	public LaneRead(final int lane, final int read) {
		this.lane = lane;
		this.read = read;

	}

	public int getLane() {
		return lane;
	}

	public int getRead() {
		return read;
	}

	public int getCode() {
		return lane * 10 + read;
	}

	@Override
	public String toString() {
		return lane + "-" + read;
	}

	@Override
	public boolean equals(Object laneRead) {
		LaneRead lr = (LaneRead) laneRead;
		return this.lane == lr.getLane() && this.read == lr.getRead();
	}

	@Override
	public int hashCode() {
		return lane * 10 + read;
	}

	@Override
	public int compareTo(LaneRead laneRead) {
		return this.getCode() - laneRead.getCode();
	}

}