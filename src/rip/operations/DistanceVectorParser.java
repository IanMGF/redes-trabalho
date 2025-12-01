package rip.operations;

import java.util.List;

public class DistanceVectorParser {
    public static int[] parse(String data) {
        List<String> strVector = List.of(data.split(":"));
        int[] distanceVector = new int[strVector.size()];
        try {
            for (int i = 0; i < distanceVector.length; i++) {
                String str = strVector.get(i);
                distanceVector[i] = Integer.parseInt(str);
            }
        } catch(NumberFormatException | NullPointerException e) {
            return null;
        }

        return distanceVector;
    }
}
