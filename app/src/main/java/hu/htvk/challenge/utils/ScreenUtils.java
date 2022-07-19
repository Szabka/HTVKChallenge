package hu.htvk.challenge.utils;

import android.content.Context;
import android.location.Location;
import android.widget.Toast;

import hu.htvk.challenge.json.Checkpoint;

public class ScreenUtils {
	private static final double RADIUS=6371000;

	public static void showMessage(Context ctx, String text) {
		Toast toast = Toast.makeText(ctx, text,
				Toast.LENGTH_SHORT);
		toast.show();
	}

	public static double calcDistance(String loc1str,Location loc2) {
		String[] loc1arr=loc1str.split(",");
		double lat1 =Location.convert(loc1arr[0].trim());
		double lon1 = Location.convert(loc1arr[1].trim());
		double lat2=loc2.getLatitude();
		double lon2=loc2.getLongitude();
		double dLat = Math.toRadians(lat2-lat1);
		double dLon = Math.toRadians(lon2-lon1);
		double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
				Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
						Math.sin(dLon/2) * Math.sin(dLon/2);
		double c = 2 * Math.asin(Math.sqrt(a));
		return RADIUS * c;
	}

	public static Location getCpLocation(Checkpoint cp) {
        String[] loc1arr=cp.getLocation().split(",");
        double lat1 =Location.convert(loc1arr[0].trim());
        double lon1 = Location.convert(loc1arr[1].trim());
	    Location l = new Location("htvpcheckpoint");
	    l.setLatitude(lat1);
	    l.setLongitude(lon1);
	    return l;
    }

}

