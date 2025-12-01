package rip.operations;

import java.util.List;

public class DistanceTableParser {
    public static int[][] parse(String data) {
        List<String> strList = List.of(data.split(" "));
        int[][] distanceTable = new int[strList.size()][];
        try {
            for (int i = 0; i < strList.size(); i++) {
                String str = strList.get(i);
                int[] vec = DistanceVectorParser.parse(str);
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
