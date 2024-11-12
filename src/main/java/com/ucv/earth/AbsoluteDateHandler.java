package com.ucv.earth;

import com.ucv.util.LoggerCustom;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.time.AbsoluteDate;

import java.util.Map;

public class AbsoluteDateHandler {

    private final OneAxisEllipsoid earth;
    private final Map<String, Ephemeris> ephemerisMap;
    private AbsoluteDate startDate;
    private AbsoluteDate endDate;

    public AbsoluteDateHandler(OneAxisEllipsoid earth, Map<String, Ephemeris> ephemerisMap, AbsoluteDate startDate, AbsoluteDate endDate) {
        this.earth = earth;
        this.ephemerisMap = ephemerisMap;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public AbsoluteDate setAbsoluteDateOnThread() {

        if (this.earth == null) {
            return null;
        }

        AbsoluteDate minDate = null;
        AbsoluteDate maxDate = null;

        for (Ephemeris ephemeris : ephemerisMap.values()) {
            if (minDate == null || ephemeris.getMinDate().compareTo(minDate) < 0) {
                minDate = ephemeris.getMinDate();
            }
            if (maxDate == null || ephemeris.getMaxDate().compareTo(maxDate) > 0) {
                maxDate = ephemeris.getMaxDate();
            }
        }

        if (minDate == null || maxDate == null) {
            LoggerCustom.getInstance().logMessage("No available data for propagation");
            return null;
        }

        compareStartAndEndDate(minDate, maxDate);
        return startDate;
    }

    private void compareStartAndEndDate(AbsoluteDate minDate, AbsoluteDate maxDate) {
        if (startDate.compareTo(minDate) < 0) {
            startDate = minDate;
        }
        if (endDate.compareTo(maxDate) > 0) {
            endDate = maxDate;
        }
    }
}
