from sklearn.neighbors import KNeighborsClassifier
from data_pipeline import DataPipeline
from sklearn import neighbors
from sklearn.model_selection import cross_val_score
import numpy as np
from knn import knn

if __name__ == "__main__":
    rupak_df = DataPipeline(
        "rupak", use_barometer=True, use_accelerometer=True
    ).dataframe
    siddhant_df = DataPipeline(
        "siddhant", use_barometer=True, use_accelerometer=True
    ).dataframe
    rijul_df = DataPipeline(
        "rijul", use_barometer=True, use_accelerometer=True
    ).dataframe

    knn([rupak_df, siddhant_df, rijul_df])
