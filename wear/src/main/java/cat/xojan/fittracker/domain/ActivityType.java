package cat.xojan.fittracker.domain;

import android.content.Context;

import com.google.android.gms.fitness.FitnessActivities;

import cat.xojan.fittracker.R;

public enum ActivityType {
    running(FitnessActivities.RUNNING, R.string.running),
    walking(FitnessActivities.WALKING, R.string.walking),
    biking(FitnessActivities.BIKING, R.string.biking);

    String fitnessActivity;
    int activityString;

    ActivityType(String fitnessActivity, int activityString) {
        this.fitnessActivity = fitnessActivity;
        this.activityString = activityString;
    }

    public static String[] getStringArray(Context context) {
        ActivityType[] tmp = values();
        String[] result = new String[tmp.length];
        for (int i = 0; i < tmp.length; i++) {
            result[i] = context.getResources().getString(tmp[i].activityString);
        }
        return result;
    }

    public String getActivity() {
        return fitnessActivity;
    }

    public static int getRightLanguageString(String activity) {
        if (activity.equals(running.fitnessActivity)) {
            return running.activityString;
        } else if (activity.equals(walking.fitnessActivity)) {
            return walking.activityString;
        } else if (activity.equals(biking.fitnessActivity)) {
            return biking.activityString;
        }

        return R.string.workout;
    }
}
