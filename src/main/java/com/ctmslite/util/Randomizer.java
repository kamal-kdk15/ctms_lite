package com.ctmslite.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Balanced three-arm allocation for a demo CTMS. The caller must lock the trial
 * row FOR UPDATE before counting and assigning, so concurrent enrollments serialize.
 * This is not a validated clinical-trial randomization service.
 */
public final class Randomizer {
    private static final SecureRandom RANDOM = new SecureRandom();
    public static final String[] ARMS = {
        "Arm A (Active)", "Arm B (Standard of Care)", "Placebo"
    };

    private Randomizer() {}

    public static String chooseArm(Map<String, Object> counts) {
        long min = Long.MAX_VALUE;
        List<String> eligible = new ArrayList<String>();
        for (String arm : ARMS) {
            Object value = counts.get(arm);
            long count = value == null ? 0L : ((Number) value).longValue();
            if (count < min) {
                min = count;
                eligible.clear();
                eligible.add(arm);
            } else if (count == min) {
                eligible.add(arm);
            }
        }
        return eligible.get(RANDOM.nextInt(eligible.size()));
    }

    public static String number(int trialId, long sequence) {
        return String.format("CTMS-%03d-%05d", trialId, sequence);
    }
}
