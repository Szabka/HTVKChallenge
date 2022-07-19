package hu.htvk.challenge.utils;

import android.location.Location;

import java.util.Comparator;

import hu.htvk.challenge.json.Checkpoint;

public class CheckpointComparator implements Comparator<Checkpoint> {
    private final Location l;

    public CheckpointComparator(Location l) {
        this.l=l;
    }

    @Override
    public int compare(Checkpoint lhs, Checkpoint rhs) {
        double d1=ScreenUtils.calcDistance(lhs.getLocation(),l);
        double d2=ScreenUtils.calcDistance(rhs.getLocation(),l);
        return Double.compare(d1,d2);
    }
}
