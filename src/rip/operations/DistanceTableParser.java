package rip.operations;

import java.util.List;

public class DistanceTableParser {
    public static short[][] parse(String data) {
        List<String> strList = List.of(data.split(" "));
        short[][] distanceTable = new short[strList.size()][];
        try {
            for (int i = 0; i < strList.size(); i++) {
                String str = strList.get(i);
                short[] vec = DistanceVectorParser.parse(str);
                if (vec == null) {
                    return null;
                }
                distanceTable[i] = vec;
            }
        } catch(NumberFormatException | NullPointerException e) {
            return null;
        }

        return distanceTable;
    }
}
