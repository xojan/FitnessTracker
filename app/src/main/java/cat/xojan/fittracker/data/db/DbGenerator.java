package cat.xojan.fittracker.data.db;

import cat.xojan.fittracker.BuildConfig;
import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Property;
import de.greenrobot.daogenerator.Schema;
import de.greenrobot.daogenerator.ToMany;

/**
 * Generates entities and DAOs.
 *
 * Run it as a Java application (not Android).
 */
public class DbGenerator {

    public static void main(String[] args) throws Exception {
        Schema schema = new Schema(1, BuildConfig.APPLICATION_ID);

        addWorkout(schema);

        new DaoGenerator().generateAll(schema, "");
    }

    private static void addWorkout(Schema schema) {
        Entity workout = schema.addEntity("Workout");
        workout.addIdProperty();
        workout.addStringProperty("text");
        workout.addLongProperty("workoutTime");
        workout.addLongProperty("startTime");
        workout.addLongProperty("endTime");
        workout.addLongProperty("distance");

        Entity track = schema.addEntity("Track");
        track.setTableName("LOCATIONS");
        track.addIdProperty();
        track.addDoubleProperty("longitude");
        track.addDoubleProperty("lattitude");
        Property locationDate = track.addDateProperty("date").getProperty();
        Property workoutId = track.addLongProperty("workoutId").notNull().getProperty();

        ToMany workoutToLocations = workout.addToMany(track, workoutId);
        workoutToLocations.setName("locations");
        workoutToLocations.orderAsc(locationDate);
    }
}