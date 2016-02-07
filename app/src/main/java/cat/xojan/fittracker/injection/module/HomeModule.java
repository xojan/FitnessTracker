package cat.xojan.fittracker.injection.module;

import android.app.Activity;

import cat.xojan.fittracker.data.UserData;
import cat.xojan.fittracker.data.repository.GoogleFitStorage;
import cat.xojan.fittracker.domain.FitnessDataInteractor;
import cat.xojan.fittracker.injection.PerActivity;
import cat.xojan.fittracker.presentation.home.HomePresenter;
import dagger.Module;
import dagger.Provides;

@Module
public class HomeModule {

    public HomeModule() {

    }

    @Provides
    @PerActivity
    FitnessDataInteractor provideFitnessDataInteractor() {
        return new FitnessDataInteractor(new GoogleFitStorage());
    }

    @Provides
    @PerActivity
    HomePresenter provideStartUpPresenter(UserData userData) {
        return new HomePresenter(userData);
    }
}
