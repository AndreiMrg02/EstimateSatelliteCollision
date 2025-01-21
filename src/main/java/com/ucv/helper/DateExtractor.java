package com.ucv.helper;

import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DateExtractor {

    private final List<SpacecraftState> spacecraftStatesOne;
    private final List<SpacecraftState> spacecraftStatesTwo;

    public DateExtractor(List<SpacecraftState> spacecraftStatesOne, List<SpacecraftState> spacecraftStatesTwo) {
        this.spacecraftStatesOne = spacecraftStatesOne;
        this.spacecraftStatesTwo = spacecraftStatesTwo;
    }

    public AbsoluteDate extractStartDate() {
        return findMatchingDate(spacecraftStatesOne, spacecraftStatesTwo);
    }

    public AbsoluteDate extractEndDate() {
        List<SpacecraftState> reversedOne = new ArrayList<>(spacecraftStatesOne);
        List<SpacecraftState> reversedTwo = new ArrayList<>(spacecraftStatesTwo);
        Collections.reverse(reversedOne);
        Collections.reverse(reversedTwo);

        return findMatchingDate(reversedOne, reversedTwo);
    }

    private AbsoluteDate findMatchingDate(List<SpacecraftState> listOne, List<SpacecraftState> listTwo) {
        if (listOne.isEmpty() || listTwo.isEmpty()) {
            return null;
        }

        Set<AbsoluteDate> datesInListTwo = listTwo.stream()
                .map(SpacecraftState::getDate)
                .collect(Collectors.toSet());

        for (SpacecraftState state : listOne) {
            if (datesInListTwo.contains(state.getDate())) {
                return state.getDate();
            }
        }

        return null;
    }

}
