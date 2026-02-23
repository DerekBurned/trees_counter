package com.example.trees_houses_counter.di

import android.content.Context
import androidx.room.Room
import com.example.trees_houses_counter.data.local.dao.ReportDao
import com.example.trees_houses_counter.data.local.database.AppDatabase
import com.example.trees_houses_counter.data.repository.AuthRepositoryImpl
import com.example.trees_houses_counter.data.repository.ReportRepositoryImpl
import com.example.trees_houses_counter.domain.repository.AuthRepository
import com.example.trees_houses_counter.domain.repository.ReportRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "report_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideReportDao(database: AppDatabase): ReportDao {
        return database.reportDao()
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth
    ): AuthRepository {
        return AuthRepositoryImpl(firebaseAuth)
    }

    @Provides
    @Singleton
    fun provideReportRepository(
        firestore: FirebaseFirestore,
        reportDao: ReportDao,
        gson: Gson
    ): ReportRepository {
        return ReportRepositoryImpl(firestore, reportDao, gson)
    }
}
