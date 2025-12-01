package rip.operations;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DistanceVectorParser {
    public static short[] parse(String data) {
        List<String> strVector = List.of(data.split(":"));
        short[] distanceVector = new short[strVector.size()];
        try {
            for (int i = 0; i < distanceVector.length; i++) {
                String str = strVector.get(i);
                distanceVector[i] = Short.parseShort(str);
            }
        } catch(NumberFormatException | NullPointerException e) {
            return null;
        }

        return distanceVector;
    }
}
