from sklearn.neighbors import KNeighborsClassifier
from data_pipeline import DataPipeline
from sklearn import neighbors
from sklearn.model_selection import cross_val_score
import numpy as np

if __name__ == "__main__":
    rupak_df = DataPipeline("rupak").dataframe
    siddhant_df = DataPipeline("siddhant").dataframe
    rijul_df = DataPipeline("rijul").dataframe

    def knn(rupak_df, siddhant_df):
        model = KNeighborsClassifier(5)
        rupak_df.dropna(inplace=True)
        X_train = rupak_df.drop(columns=["dnd_value"])
        y_train = rupak_df["dnd_value"]

        model.fit(X_train, y_train)

        siddhant_df.dropna(inplace=True)
        X_test = siddhant_df.drop(columns=["dnd_value"])
        y_test = siddhant_df["dnd_value"]
        model.predict(X_test)

        print( model.score(X_test, y_test))

        cv_scores = cross_val_score(model, X_train, y_train, cv=5)
        print(cv_scores)
        print("cv_scores mean:{}".format(np.mean(cv_scores)))
    
    knn(rupak_df, siddhant_df)

    
