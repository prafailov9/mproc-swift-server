import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Main {

    public static boolean hasOverlap(List<TimePeriodRecord> records) {
        records.sort(Comparator.comparingLong(TimePeriodRecord::getStart));
        for (int i = 1; i < records.size(); i++) {
            if (records.get(i - 1).getEnd() > records.get(i).getStart()) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        List<TimePeriodRecord> records = new ArrayList<>();


    }
}